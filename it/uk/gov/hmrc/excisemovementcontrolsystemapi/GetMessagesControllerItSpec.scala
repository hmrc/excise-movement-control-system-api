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
import com.github.tomakehurst.wiremock.client.WireMock.ok
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterAll
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDateTime}
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class GetMessagesControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with WireMockServerSpec
  with RepositoryTestStub
  with BeforeAndAfterAll {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val consignorId = "GBWK002281023"
  private val lrn = "token"
  private val url = s"http://localhost:$port/movements/$lrn/messages"
  private lazy val dateTimeService: DateTimeService = mock[DateTimeService]
  private val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")
  private val responseFromEis = EISConsumptionResponse(
    LocalDateTime.of(2023, 1, 2, 3, 4, 5),
    consignorId,
    Base64.getEncoder.encodeToString(NewMessagesXml.newMessageWithIE801.toString().getBytes(StandardCharsets.UTF_8)),
  )

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServer)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementRepository].to(movementRepository),
        bind[DateTimeService].to(dateTimeService)
      )
      .build()
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
      stubShowNewMessageRequest(consignorId)
      when(movementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(Movement(lrn, consignorId, None, None, Instant.now, Seq.empty))))
      when(dateTimeService.instant).thenReturn(timestamp)

      val result = getRequest

      result.status mustBe OK

      withClue("return a list of messages as response") {
        assertResponseContent(result.json.as[Seq[Message]])
      }

    }

    "return 400 when no movement message is found" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = getRequest

      result.status mustBe BAD_REQUEST
    }

    "return 500 when mongo db fails to fetch details" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = getRequest

      result.status mustBe INTERNAL_SERVER_ERROR
    }

    //todo: This may be deleted as it may not be a valid case. We should only have one movement,
    // for a combination of lrn consignorId/consigneeId
    "return 500 when multiple movements messages are found" in {
      withAuthorizedTrader(consignorId)
      val movementMessage = Movement("", "", None, None, timestamp, Seq(Message("", "", dateTimeService)))
      when(movementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movementMessage, movementMessage)))

      val result = getRequest

      result.status mustBe INTERNAL_SERVER_ERROR
    }

    "return forbidden (403) when there are no authorized ERN" in {
      withUnAuthorizedERN()

      getRequest.status mustBe FORBIDDEN
    }

    "return a Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      getRequest.status mustBe UNAUTHORIZED
    }

  }

  private def getRequest = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN"
      ).get()
    )
  }

  private def stubShowNewMessageRequest(exciseNumber: String) = {
    wireMock.stubFor(
      WireMock.get(s"/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=$exciseNumber")
        .willReturn(
          ok().withBody(Json.toJson(responseFromEis).toString()
        ))
    )
  }

  private def assertResponseContent(messageObj: Seq[Message]) = {
    messageObj.head.messageType mustBe MessageTypes.IE801.value

    val actualMessage = Base64.getDecoder.decode(messageObj.head.encodedMessage).map(_.toChar).mkString
    actualMessage.matches(".*</ie801:IE801>$") mustBe true
  }

}
