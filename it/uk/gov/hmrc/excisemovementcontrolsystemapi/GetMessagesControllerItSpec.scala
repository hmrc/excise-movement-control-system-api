/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.excisemovementcontrolsystemapi

import com.github.tomakehurst.wiremock.client.WireMock
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{NewMessagesXml, TestXml}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}

class GetMessagesControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with NewMessagesXml
  with WireMockServerSpec
  with RepositoryTestStub
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val consignorId = "GBWK002281023"
  private val validUUID = "cfdb20c7-d0b0-4b8b-a071-737d68dede5e"
  private val messageId = UUID.randomUUID().toString
  private val url = s"http://localhost:$port/movements/$validUUID/messages"
  private val messageUrl = s"http://localhost:$port/movements/$validUUID/message/$messageId"
  private lazy val dateTimeService: DateTimeService = mock[DateTimeService]
  private val timestamp = Instant.now()

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureEisService)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementRepository].to(movementRepository),
        bind[DateTimeService].to(dateTimeService)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeAll()
    reset(movementRepository, dateTimeService, authConnector)
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }
  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Get Messages" should {
    "return 200" in {
      withAuthorizedTrader(consignorId)
      val encodedMessage = Base64.getEncoder.encodeToString(IE801.toString().getBytes(StandardCharsets.UTF_8))
      val message = Message(encodedMessage, "IE801", messageId, timestamp)
      when(movementRepository.getMovementById(any))
        .thenReturn(Future.successful(Some(Movement(validUUID, "boxId", "lrn", consignorId, None, None, Instant.now, Seq(message)))))

      val result = getRequest()

      result.status mustBe OK

      withClue("return a list of messages as response") {
        assertResponseContent(result.json.as[Seq[Message]], Seq(message))
      }

    }

    "return 404 when no movement is found" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementById(any))
        .thenReturn(Future.successful(None))

      val result = getRequest()

      result.status mustBe NOT_FOUND
    }

    "return 404 when movement is not valid for ERN" in {
      withAuthorizedTrader(consignorId)
      val message = Message("encodedMessage", "IE801", "messageId", dateTimeService.timestamp())
      when(movementRepository.getMovementById(any))
        .thenReturn(Future.successful(Some(Movement(validUUID, "boxId", "lrn", "consignor", None, None, Instant.now, Seq(message)))))

      val result = getRequest()

      result.status mustBe NOT_FOUND
    }

    "return 500 when mongo db fails to fetch details" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementById(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = getRequest()

      result.status mustBe INTERNAL_SERVER_ERROR
    }

    //todo: This may be deleted as it may not be a valid case. We should only have one movement,
    // for a combination of lrn consignorId/consigneeId
    "return 500 when multiple movements messages are found" in {
      withAuthorizedTrader(consignorId)
      val movementMessage = Movement("boxId", "", "", None, None, timestamp, Seq(Message("", "", "messageId", dateTimeService.timestamp())))
      when(movementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movementMessage, movementMessage)))

      val result = getRequest()

      result.status mustBe INTERNAL_SERVER_ERROR
    }

    "return forbidden (403) when there are no authorized ERN" in {
      withAnEmptyERN()

      getRequest().status mustBe FORBIDDEN
    }

    "return a Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      getRequest().status mustBe UNAUTHORIZED
    }

  }

  "GET a message for a movement and a messageId" in {
    withAuthorizedTrader(consignorId)

    val encodedMessage = Base64.getEncoder.encodeToString(IE801.toString().getBytes(StandardCharsets.UTF_8))
    val message = Message(encodedMessage, "IE801", messageId, timestamp)
    when(movementRepository.getMovementById(any))
      .thenReturn(Future.successful(Some(Movement(validUUID, "boxId", "lrn", consignorId, None, None, Instant.now, Seq(message)))))

    val result = getRequest(messageUrl)

    result.status mustBe 200

    withClue("Should return a xml message"){
      result.xml mustBe IE801
    }
  }

  "Get one message return 404" when {
    "movement not found" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementById(any)).thenReturn(Future.successful(None))

      val result = getRequest(messageUrl)

      result.status mustBe NOT_FOUND
      result.json mustBe Json.toJson(movementNotFoundError)
    }

    "messageId does not match" in {
      withAuthorizedTrader(consignorId)

      val encodedMessage = Base64.getEncoder.encodeToString(IE801.toString().getBytes(StandardCharsets.UTF_8))
      val message = Message(encodedMessage, "IE801", "messageId", timestamp)
      when(movementRepository.getMovementById(any))
        .thenReturn(Future.successful(Some(createMovementWithMessages(messages = Seq(message)))))

      val result = getRequest(messageUrl)

      result.status mustBe NOT_FOUND
      result.json mustBe Json.toJson(messageNotFoundError)
    }

    "message list is empty" in {
      withAuthorizedTrader(consignorId)

      val movementWithOutMessage = createMovementWithMessages()
      when(movementRepository.getMovementById(any))
        .thenReturn(Future.successful(Some(movementWithOutMessage)))

      val result = getRequest(messageUrl)

      result.status mustBe NOT_FOUND
      result.json mustBe Json.toJson(messageNotFoundError)
    }

    "messageId is empty" in {
      withAuthorizedTrader(consignorId)

      val invalidUrl = s"http://localhost:$port/movements/$validUUID/message/"
      val result = getRequest(invalidUrl)

      result.status mustBe NOT_FOUND
    }
  }

  "Get one message return 400" when {
    "movementId is invalid UUID" in {
      withAuthorizedTrader(consignorId)

      val invalidUrl = s"http://localhost:$port/movements/12345/message/$messageId"
      val result = getRequest(invalidUrl)

      result.status mustBe BAD_REQUEST
      result.json mustBe Json.toJson(ErrorResponse(
        timestamp,
        "Movement Id format error",
        "Movement Id should be a valid UUID"
      ))
    }
  }

  "Get on message return 500 when mongo db fails to fetch details" in {
    withAuthorizedTrader(consignorId)
    when(movementRepository.getMovementById(any))
      .thenReturn(Future.failed(new RuntimeException("error")))

    val result = getRequest(messageUrl)

    result.status mustBe INTERNAL_SERVER_ERROR
  }

  "Get one message return forbidden (403) when there are no authorized ERN" in {
    withAnEmptyERN()

    getRequest(messageUrl).status mustBe FORBIDDEN
  }

  "Get one message return a Unauthorized (401) when no authorized trader" in {
    withUnauthorizedTrader(InternalError("A general auth failure"))

    getRequest(messageUrl).status mustBe UNAUTHORIZED
  }

  "Get one message return a 500 if message is not XML" in {
    withAuthorizedTrader(consignorId)
    val message = Message("encodedMessage", "IE801", messageId, timestamp)
    when(movementRepository.getMovementById(any))
      .thenReturn(Future.successful(Some(createMovementWithMessages(messages = Seq(message)))))

    val result = getRequest(messageUrl)

    result.status mustBe INTERNAL_SERVER_ERROR
  }

  private def getRequest(url: String = url) = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN"
      ).get()
    )
  }

  private def assertResponseContent(actual: Seq[Message], expected: Seq[Message]) = {
    actual.head.messageType mustBe expected.head.messageType
    actual.head.encodedMessage mustBe expected.head.encodedMessage
  }

  private def createMovementWithMessages(movementId: String = validUUID, messages: Seq[Message] = Seq.empty):Movement = {
    Movement(movementId, "boxId", "lrn", consignorId, Some("consigneeId"), Some("arc"), Instant.now, messages)
  }

  private def movementNotFoundError: ErrorResponse = {
    ErrorResponse(
      timestamp,
      "No movement found for the MovementID provided",
      s"MovementID $validUUID was not found in the database"
    )
  }

  private def messageNotFoundError: ErrorResponse = {
    ErrorResponse(
      timestamp,
      "No message found for the MovementID provided",
      s"MessageId $messageId was not found in the database"
    )
  }
}
