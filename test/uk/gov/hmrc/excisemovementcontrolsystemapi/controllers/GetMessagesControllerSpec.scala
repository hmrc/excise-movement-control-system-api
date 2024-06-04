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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{await, contentAsJson, contentAsString, defaultAwaitTimeout, status, stubControllerComponents}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.ValidateAcceptHeaderAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{ErrorResponseSupport, FakeAuthentication}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.MovementIdValidation
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MessageService, MovementService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class GetMessagesControllerSpec extends PlaySpec
  with FakeAuthentication
  with ErrorResponseSupport
  with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys: ActorSystem = ActorSystem("GetMessagesControllerSpec")

  private val movementService = mock[MovementService]
  private val cc = stubControllerComponents()
  private val validUUID = "cfdb20c7-d0b0-4b8b-a071-737d68dede5e"
  private val dateTimeService = mock[DateTimeService]
  private val timeStamp = Instant.parse("2020-01-01T01:01:01.123456Z")
  private val messageService = mock[MessageService]
  private val messageCreateOn = Instant.now()

  private val MovementIdFormatError = Json.parse(
    """
      |{
      | "dateTime":"2020-01-01T01:01:01.123Z",
      | "message":"Movement Id format error",
      | "debugMessage":"The movement ID should be a valid UUID"
      | }""".stripMargin)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, dateTimeService, messageService)

    when(dateTimeService.timestamp()).thenReturn(timeStamp)
    when(messageService.updateAllMessages(any)(any)).thenReturn(Future.successful(Done))
  }

  "getMessagesForMovement" should {
    "respond with BAD_REQUEST when the MovementID is an invalid UUID" in {
      val result = createWithSuccessfulAuth().getMessagesForMovement("invalidUUID", None, None)(createRequest())

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe MovementIdFormatError
    }

    "respond with BAD_REQUEST when invalid traderType has passed" in {
      val result = createWithSuccessfulAuth().getMessagesForMovement(UUID.randomUUID().toString, None, Some("invalidTraderType"))(createRequest())

      status(result) mustBe BAD_REQUEST
    }

    "return 200 when consignor is valid" in {
      val message = Message(123, "message", "IE801", "messageId", "ern", Set.empty, messageCreateOn)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))
      val controller = new GetMessagesController(
        FakeSuccessAuthenticationMultiErn(Set(ern, "otherErn")),
        new ValidateAcceptHeaderAction(dateTimeService),
        movementService,
        messageService,
        new MovementIdValidation,
        cc,
        new EmcsUtils,
        dateTimeService
      )
      await(controller.getMessagesForMovement(validUUID, None, None)(createRequest()))

      verify(messageService).updateAllMessages(eqTo(Set(ern, "otherErn")))(any)
    }

    "return 200 when consignee is valid" in {
      val message = Message(123, "message", "IE801", "messageId", "ern", Set.empty, messageCreateOn)
      val movement = Movement(validUUID, Some("boxId"), "lrn", "consignor", Some("testErn"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe JsArray(Seq(expectedMessageResponseAsJson(
        "message",
        "IE801",
        "messageId",
        messageCreateOn
      )))
    }

    "return 200 when message recipient is valid" in {
      val message = Message(123, "message", "IE801", "messageId", "testErn", Set.empty, messageCreateOn)
      val movement = Movement(validUUID, Some("boxId"), "lrn", "consignor", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe JsArray(Seq(expectedMessageResponseAsJson(
        "message",
        "IE801",
        "messageId",
        messageCreateOn
      )))
    }

    "get all the new messages" in {
      val message = Message(123, "message", "IE801", "messageId1", "ern", Set.empty, messageCreateOn)
      val message2 = Message(345,"message2", "IE801", "messageId2", "ern", Set.empty, messageCreateOn)
      val movement = createMovementWithMessages(Seq(message, message2))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

      status(result) mustBe OK

      val expectedJson = Seq(
        expectedMessageResponseAsJson("message", "IE801", "messageId1", messageCreateOn),
        expectedMessageResponseAsJson("message2", "IE801", "messageId2", messageCreateOn)
      )
      contentAsJson(result) mustBe JsArray(expectedJson)
    }

    "get all the new messages when there is a time query parameter provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", "messageId1", "ern", Set.empty, timeInFuture)
      val message2 = Message("message2", "IE801", "messageId2", "ern", Set.empty, timeInPast)
      val movement = createMovementWithMessages(Seq(message, message2))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some(messageCreateOn.toString), None)(createRequest())

      status(result) mustBe OK

      val jsonResponse = expectedMessageResponseAsJson(
        "message",
        "IE801",
        "messageId1",
        timeInFuture)
      contentAsJson(result) mustBe JsArray(Seq(jsonResponse))
    }

    "get all the new messages including messages with a createdOn time of NOW when there is a time query parameter provided" in {
      val timeNowString = messageCreateOn.toString
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", "messageId1", "ern", Set.empty, timeInFuture)
      val message2 = Message("message2", "IE801", "messageId2", "ern", Set.empty, timeInPast)
      val message3 = Message("message3", "IE801", "messageId3", "ern", Set.empty, messageCreateOn)
      val movement = createMovementWithMessages(Seq(message, message2, message3))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some(timeNowString), None)(createRequest())

      status(result) mustBe OK

      val expectedJson = Seq(
        expectedMessageResponseAsJson("message", "IE801", "messageId1", timeInFuture),
        expectedMessageResponseAsJson("message3", "IE801", "messageId3", messageCreateOn)
      )
      contentAsJson(result) mustBe JsArray(expectedJson)
    }

    "succeed when a valid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", "messageId", "ern", Set.empty, timeInFuture)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some("2020-11-15T17:02:34.00Z"), None)(createRequest())

      status(result) mustBe OK

      val expectedJson = expectedMessageResponseAsJson(
        "message",
        "IE801",
        "messageId",
        timeInFuture
      )
      contentAsJson(result) mustBe JsArray(Seq(expectedJson))
    }

    "return only messages that match consignorId" in {
      val timeInFuture = Instant.now.plusSeconds(1000)

      val message1 = Message("firstMessage", "IE801", "messageId", ern, Set.empty, timeInFuture)
      val message2 = Message("secondMessage", "IE801", "messageId", ern, Set.empty, timeInFuture)
      val message3 = Message("message", "IE801", "messageId", "otherErn", Set.empty, timeInFuture)
      val movement = createMovementWithMessages(Seq(message1, message2, message3))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, Some("consignor"))(createRequest())

      status(result) mustBe OK

      val expectedJson1 = expectedMessageResponseAsJson(
        "firstMessage",
        "IE801",
        "messageId",
        timeInFuture
      )
      val expectedJson2 = expectedMessageResponseAsJson(
        "secondMessage",
        "IE801",
        "messageId",
        timeInFuture
      )
      contentAsJson(result) mustBe JsArray(Seq(expectedJson1, expectedJson2))

    }

    "return only messages that match consigneeId" in {
      val timeInFuture = Instant.now.plusSeconds(1000)

      val message1 = Message("firstMessage", "IE801", "messageId", "consigneeId", Set.empty, timeInFuture)
      val message2 = Message("secondMessage", "IE801", "messageId", ern, Set.empty, timeInFuture)
      val message3 = Message("message", "IE801", "messageId", "otherErn", Set.empty, timeInFuture)
      val movement = createMovementWithMessages(Seq(message1, message2, message3))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth(Set("other_ern", "consigneeId")).getMessagesForMovement(validUUID, None, Some("consignee"))(createRequest())

      status(result) mustBe OK

      val expectedJson1 = expectedMessageResponseAsJson(
        "firstMessage",
        "IE801",
        "messageId",
        timeInFuture
      )
      contentAsJson(result) mustBe JsArray(Seq(expectedJson1))

    }
    "fail when an invalid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", "messageId", "ern", Set.empty, timeInFuture)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some("invalid date"), None)(createRequest())

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe expectedJsonErrorResponse(
        "2020-01-01T01:01:01.123Z",
        "Invalid date format provided in the updatedSince query parameter",
        "Date format should be like '2020-11-15T17:02:34.00Z'"
      )
    }

    "return an empty array when no messages" in {
      val movement = createMovementWithMessages(Seq.empty)
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe JsArray()
    }

    "return NOT_FOUND when no movement exists for given movementID" in {
      when(movementService.getMovementById(any)).thenReturn(Future.successful(None))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe expectedJsonErrorResponse(
        "2020-01-01T01:01:01.123Z",
        "Movement not found",
          s"Movement $validUUID could not be found"
      )
    }

    "return FORBIDDEN when movement is for a different ern " in {
      val message = Message("message", "IE801", "messageId", "ern", Set.empty, Instant.now)
      val movement = Movement(validUUID, Some("boxId"), "lrn", "consignor", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe expectedJsonErrorResponse(
        "2020-01-01T01:01:01.123Z",
        "Forbidden",
        "Invalid MovementID supplied for ERN"
      )
    }
  }

  "getMessageForMovement" should {
    val messageXml =
      """
        |<body>
        | <received>
        |   <where>london</where>
        | </received>
        |</body>
        |""".stripMargin

    val encodeMessage = Base64.getEncoder.encodeToString(messageXml.getBytes(StandardCharsets.UTF_8))

    val messageId = UUID.randomUUID().toString
    val message = Message(encodeMessage, "IE801", messageId, "ern", Set.empty, timeStamp)
    val movementWithMessage = createMovementWithMessages(Seq(message))

    "return 200" in {
      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(movementWithMessage)))

      val result = createWithSuccessfulAuth().getMessageForMovement(validUUID, messageId)(createRequest())

      status(result) mustBe OK
    }

    "return the message as xml for that movement and messageId" in {
      val message1 = Message("encodeMessage", "IE803", UUID.randomUUID().toString, "ern", Set.empty, timeStamp)
      val movement = movementWithMessage.copy(messages = Seq(message, message1))
      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth().getMessageForMovement(validUUID, messageId)(createRequest())

      status(result) mustBe OK
      contentAsXml(result) mustBe xml.XML.loadString(messageXml)
    }

    "return an error" when {
      "failed authentication" in {
        val result = createWithFailedAuth.getMessageForMovement(validUUID, messageId)(createRequest())

        status(result) mustBe FORBIDDEN
      }

      "movementId is invalid" in {
        val result = createWithSuccessfulAuth().getMessageForMovement(
          "invalidMovementId",
          messageId
        )(createRequest())

        status(result) mustBe BAD_REQUEST
      }

      "movementId is empty" in {
        val result = createWithSuccessfulAuth().getMessageForMovement(
          "",
          messageId
        )(createRequest())

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe MovementIdFormatError
      }

      "return a 404 if movement is not found" in {
        when(movementService.getMovementById(any)).thenReturn(Future.successful(None))

        val result = createWithSuccessfulAuth().getMessageForMovement(
          validUUID,
          messageId
        )(createRequest())

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "Movement not found",
          s"Movement $validUUID could not be found"
        )
      }

      "return a 404 if message is not found" in {
        val movementWithoutMessages: Movement = movementWithMessage.copy(messages = Seq.empty)
        when(movementService.getMovementById(any))
          .thenReturn(Future.successful(Some(movementWithoutMessages)))

        val result = createWithSuccessfulAuth().getMessageForMovement(
          validUUID,
          messageId
        )(createRequest())

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "No message found for the MovementID provided",
          s"MessageId $messageId was not found in the database"
        )
      }
    }

    "return an error iƒ Accept header is not hmrc xml" in {
      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(movementWithMessage)))

      val result = createWithSuccessfulAuth()
        .getMessageForMovement(
          validUUID,
          messageId
        )(createRequest("application/vnd.hmrc.1.0+json"))

      status(result) mustBe NOT_ACCEPTABLE
    }

  }

  private def contentAsXml(result: Future[Result]): Elem = {
    xml.XML.loadString(contentAsString(result))
  }

  private def createMovementWithMessages(messages: Seq[Message]): Movement = {
    Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, messages)
  }

  private def createWithSuccessfulAuth(erns: Set[String] = Set(ern)) =
    new GetMessagesController(
      FakeSuccessAuthentication(erns),
      new ValidateAcceptHeaderAction(dateTimeService),
      movementService,
      messageService,
      new MovementIdValidation,
      cc,
      new EmcsUtils,
      dateTimeService
    )

  private def createWithFailedAuth =
    new GetMessagesController(
      FakeFailingAuthentication,
      new ValidateAcceptHeaderAction(dateTimeService),
      movementService,
      messageService,
      new MovementIdValidation,
      cc,
      new EmcsUtils,
      dateTimeService
    )

  private def createRequest(
    acceptHeader: String = "application/vnd.hmrc.1.0+xml"
  ): FakeRequest[AnyContent] = {
    FakeRequest()
      .withHeaders(FakeHeaders(Seq(
        HeaderNames.ACCEPT -> acceptHeader
      )))
  }

  private def formatToValidDateTime(dateTime: Instant): String = {
    ZonedDateTime
      .ofInstant(dateTime, ZoneOffset.UTC)
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))


  }

  private def expectedMessageResponseAsJson(
    encodedMessage: String,
    messageType: String,
    messageId: String,
    dateTime: Instant
  ): JsValue = {
    Json.parse(
      s"""
         |{
         |   "encodedMessage":"$encodedMessage",
         |   "messageType":"$messageType",
         |   "messageId":"$messageId",
         |   "createdOn": "${formatToValidDateTime(dateTime)}"
         | }
         |""".stripMargin)
  }
}
