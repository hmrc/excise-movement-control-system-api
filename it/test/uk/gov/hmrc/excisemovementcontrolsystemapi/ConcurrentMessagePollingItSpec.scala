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
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.JsArray
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{NewMessagesXml, TestXml}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.ErrorResponseSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{ApplicationBuilderSupport, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class ConcurrentMessagePollingItSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with ApplicationBuilderSupport
    with TestXml
    with NewMessagesXml
    with WireMockServerSpec
    with ErrorResponseSupport
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val wsClient: WSClient         = app.injector.instanceOf[WSClient]
  protected lazy val messageConnector: MessageConnector = {
    org.scalatestplus.mockito.MockitoSugar.mock[MessageConnector]
  }

  private val consignorId = "GBWK002281023"
  private val validUUID   = "cfdb20c7-d0b0-4b8b-a071-737d68dede5e"
  private val url         = s"http://localhost:$port/movements/$validUUID/messages"

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    applicationBuilder(configureEisService)
      .overrides(
        play.api.inject.bind[MessageConnector].to(messageConnector)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeAll()
    reset(movementRepository, dateTimeService, authConnector, messageConnector, ernRetrievalRepository)
    when(messageConnector.getNewMessages(any, any, any)(any))
      .thenReturn(Future.successful(uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.GetMessagesResponse(Seq.empty, 0)))
    when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
    when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }
  
  """Message polling""" should {

    "return all messages on first poll without updatedSince" in {
      withAuthorizedTrader(consignorId)

      val messageCreatedTime = Instant.parse("2024-10-05T14:25:35.246Z")
      val message1           = createMessage("msg1", messageCreatedTime)
      val message2           = createMessage("msg2", messageCreatedTime.plus(100, ChronoUnit.MILLIS))

      // Movement's lastUpdated is slightly later than message creation (simulating DB processing delay)
      val movementLastUpdated = messageCreatedTime.plus(5, ChronoUnit.MILLIS)
      val movement = createMovement(movementLastUpdated, Seq(message1, message2))

      setupMovementMock(movement, Instant.parse("2024-10-05T14:25:37.485Z"))

      // First poll WITHOUT updatedSince query parameter
      val result = getRequest(url, None)

      // Should return ALL messages
      assertPollResponse(result, Seq("msg1", "msg2"))
    }

    "return new messages for a subsequent poll with lastUpdated from the previous poll" in {
      withAuthorizedTrader(consignorId)

      // First poll - establish baseline
      val messageCreatedTime1 = Instant.parse("2024-10-05T14:25:35.246Z")
      val message1            = createMessage("msg1", messageCreatedTime1)

      val movementLastUpdated1 = messageCreatedTime1.plus(5, ChronoUnit.MILLIS)
      val movement1 = createMovement(movementLastUpdated1, Seq(message1))

      setupMovementMock(movement1, Instant.parse("2024-10-05T14:25:37.485Z"))

      val result1 = getRequest(url, None)
      assertPollResponse(result1, Seq("msg1"))

      // Simulate time passing and new message arriving
      val messageCreatedTime2 = Instant.parse("2024-10-05T14:26:50.869Z")
      val message2            = createMessage("msg2", messageCreatedTime2)

      // New movement lastUpdated (bumped when new message was saved)
      val movementLastUpdated2 = messageCreatedTime2
      val movement2 = createMovement(movementLastUpdated2, Seq(message1, message2))

      setupMovementMock(movement2, Instant.parse("2024-10-05T14:26:50.869Z"))

      // Second poll with updatedSince = lastUpdated from first movement response
      val result2 = getRequest(url, Some(movementLastUpdated1.toString))

      // Should return only the new message (msg2)
      // msg1 has createdOn < movementLastUpdated1, so it should be filtered out
      // msg2 has createdOn >= movementLastUpdated1, so it should be included
      assertPollResponse(result2, Seq("msg2"))
    }

    "prevent message loss on chained polling using lastUpdated" in {
      withAuthorizedTrader(consignorId)

      // Simulate timeline of messages
      val t1 = Instant.parse("2024-10-05T14:25:35.246Z")  // Message 1 created
      val t2 = t1.plus(5, ChronoUnit.MILLIS)              // Movement lastUpdated bumped (5ms after msg1)
      val t3 = Instant.parse("2024-10-05T14:26:50.869Z")  // Message 2 created
      val t4 = t3.plus(1, ChronoUnit.MILLIS)              // Movement lastUpdated bumped again

      val message1 = createMessage("msg1", t1)
      val message2 = createMessage("msg2", t3)

      // Poll 1: Initial poll without updatedSince
      val movement1 = createMovement(t2, Seq(message1))
      setupMovementMock(movement1, Instant.parse("2024-10-05T14:25:37.485Z"))

      val result1 = getRequest(url, None)
      assertPollResponse(result1, Seq("msg1"))

      // Poll 2: Use lastUpdated from movement1 response (t2) as updatedSince
      val movement2 = createMovement(t4, Seq(message1, message2))
      setupMovementMock(movement2, Instant.parse("2024-10-05T14:26:50.869Z"))

      val result2 = getRequest(url, Some(t2.toString))
      // Should only get msg2 since msg1.createdOn < t2
      assertPollResponse(result2, Seq("msg2"))

      // Poll 3: Use lastUpdated from movement2 response (t4) as next updatedSince
      val message3 = createMessage("msg3", t4.plus(10, ChronoUnit.MILLIS))
      val movement3 = createMovement(t4.plus(10, ChronoUnit.MILLIS), Seq(message1, message2, message3))
      setupMovementMock(movement3, Instant.parse("2024-10-05T14:28:42.204Z"))

      val result3 = getRequest(url, Some(t4.toString))
      // Should only get msg3
      assertPollResponse(result3, Seq("msg3"))
    }

    "not lose messages on concurrent message insertion scenario" in {
      withAuthorizedTrader(consignorId)

      // Timeline:
      // t1: Message A created and saved
      // t2: Movement lastUpdated bumped (after saving Message A)
      // t3: Same instant - Message B created AND saved while trader polls with t2
      // t4: Movement lastUpdated bumped for both A and B
      
      val t1 = Instant.parse("2024-10-05T14:25:35.246Z")
      val t2 = t1.plus(5, ChronoUnit.MILLIS)
      val t3 = Instant.parse("2024-10-05T14:25:37.100Z")  // Trader polls at this time
      val t4 = t3.plus(1, ChronoUnit.MILLIS)

      val messageA = createMessage("msgA", t1)

      // Poll 1: Initial poll without updatedSince
      val movement1 = createMovement(t2, Seq(messageA))
      setupMovementMock(movement1, t3)

      val result1 = getRequest(url, None)
      assertPollResponse(result1, Seq("msgA"))

      // Meanwhile, Message B was created and saved with lastUpdated updated to t4
      val messageB = createMessage("msgB", t3)
      val movement2 = createMovement(t4, Seq(messageA, messageB))

      // Poll 2: Use t2 (movement1.lastUpdated) as updatedSince
      // This ensures we catch messageB even though it was created at t3
      setupMovementMock(movement2, t4)

      val result2 = getRequest(url, Some(t2.toString))

      // CRITICAL: messageB should be included because:
      // - messageB.createdOn (t3) >= t2 (updatedSince)
      // This proves the solution prevents message loss
      assertPollResponse(result2, Seq("msgB"))
    }

    "lose messages if using lastUpdate on first message poll" in {
      withAuthorizedTrader(consignorId)

      // This test demonstrates the bug from the problem statement
      // If a trader incorrectly uses movement.lastUpdated as updatedSince on FIRST poll, they lose messages
      val messageCreatedTime = Instant.parse("2024-10-05T14:25:35.246Z")
      val movementLastUpdated = messageCreatedTime.plus(5, ChronoUnit.MILLIS)  // 5ms later

      val message = createMessage("msg1", messageCreatedTime)

      val movement = createMovement(movementLastUpdated, Seq(message))
      setupMovementMock(movement, Instant.parse("2024-10-05T14:25:37.485Z"))

      // WRONG: Using movement.lastUpdated as updatedSince on the FIRST poll
      // The bug: message.createdOn (14:25:35.246Z) is BEFORE movementLastUpdated (14:25:35.251Z)
      val result = getRequest(url, Some(movementLastUpdated.toString))

      result.status mustBe OK
      val jsonMessages = result.json.as[JsArray].value

      // Because the filter logic is: createdOn >= updatedSince
      // 14:25:35.246Z >= 14:25:35.251Z is FALSE
      // So the message is FILTERED OUT and LOST
      jsonMessages.length mustBe 0
    }

  }

  private def getRequest(
    url: String,
    updatedSince: Option[String]
  ): WSResponse = {
    val urlWithParams = updatedSince.fold(url)(since => s"$url?updatedSince=$since")
    await(
      wsClient
        .url(urlWithParams)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN"
        )
        .get()
    )
  }

  private def createMessage(messageId: String, createdOn: Instant): Message = {
    val encodedMessage = Base64.getEncoder.encodeToString(IE801.toString().getBytes(StandardCharsets.UTF_8))
    Message(
      hash = messageId.hashCode,
      encodedMessage = encodedMessage,
      messageType = "IE801",
      messageId = messageId,
      recipient = consignorId,
      boxesToNotify = Set.empty,
      createdOn = createdOn
    )
  }

  private def createMovement(
    lastUpdated: Instant,
    messages: Seq[Message]
  ): Movement =
    Movement(
      validUUID,
      Some("boxId"),
      "lrn",
      consignorId,
      None,
      None,
      lastUpdated,
      messages
    )

  private def setupMovementMock(movement: Movement, timestamp: Instant): Unit = {
    when(movementRepository.getMovementById(any))
      .thenReturn(Future.successful(Some(movement)))
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  private def assertPollResponse(
    result: WSResponse,
    expectedMessageIds: Seq[String]
  ): Unit = {
    result.status mustBe OK
    val jsonMessages = result.json.as[JsArray].value
    jsonMessages.length mustBe expectedMessageIds.length
    expectedMessageIds.zipWithIndex.foreach { case (expectedId, index) =>
      jsonMessages(index)("messageId").as[String] mustBe expectedId
    }
  }

}
