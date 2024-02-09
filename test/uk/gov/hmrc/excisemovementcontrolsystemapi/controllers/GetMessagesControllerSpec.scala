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

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mongodb.scala.MongoException
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{await, contentAsJson, contentAsString, defaultAwaitTimeout, status, stubControllerComponents}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.{MovementIdFormatInvalid, MovementIdValidation}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class GetMessagesControllerSpec extends PlaySpec
  with FakeAuthentication
  with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys: ActorSystem = ActorSystem("GetMessagesControllerSpec")

  private val movementService = mock[MovementService]
  private val cc = stubControllerComponents()
  private val validUUID = "cfdb20c7-d0b0-4b8b-a071-737d68dede5e"
  private val dateTimeService = mock[DateTimeService]
  private val timeStamp = Instant.parse("2020-01-01T01:01:01.1Z")
  private val workItemService = mock[WorkItemService]
  private val messageCreateOn = Instant.now()

  private val MovementIdFormatError = Json.parse(
    """
      |{
      | "dateTime":"2020-01-01T01:01:01.100Z",
      | "message":"Movement Id format error",
      | "debugMessage":"The movement ID should be a valid UUID"
      | }""".stripMargin)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, dateTimeService, workItemService)

    when(movementService.getMatchingERN(any, any))
      .thenReturn(Future.successful(Some(ern)))

    when(dateTimeService.timestamp()).thenReturn(timeStamp)

    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))
  }

  "getMessagesForMovement" should {
    "respond with BAD_REQUEST when the MovementID is an invalid UUID" in {

      val result = createWithSuccessfulAuth.getMessagesForMovement("invalidUUID", None)(createRequest())

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe MovementIdFormatError
    }

    "return 200" in {
      val message = Message("message", "IE801", "messageId", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "get all the new messages" in {
      val message = Message("message", "IE801", "messageId1", messageCreateOn)
      val message2 = Message("message2", "IE801", "messageId2", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message, message2))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message, message2))
    }

    "get all the new messages when there is a time query parameter provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", "messageId1", timeInFuture)
      val message2 = Message("message2", "IE801", "messageId2", timeInPast)
      val movement = createMovementWithMessages(Seq(message, message2))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some(messageCreateOn.toString))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "get all the new messages including messages with a createdOn time of NOW when there is a time query parameter provided" in {
      val timeNowString = messageCreateOn.toString
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", "messageId1", timeInFuture)
      val message2 = Message("message2", "IE801", "messageId2", timeInPast)
      val message3 = Message("message3", "IE801", "messageId3", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message, message2, message3))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some(timeNowString))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message, message3))
    }

    "succeed when a valid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", "messageId", timeInFuture)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some("2020-11-15T17:02:34.00Z"))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "fail when an invalid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", "messageId", timeInFuture)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some("invalid date"))(createRequest())

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe Json.toJson(
        ErrorResponse(timeStamp, "Invalid date format provided in the updatedSince query parameter", "Date format should be like '2020-11-15T17:02:34.00Z'")
      )
    }

    "create a Work Item if there is not one for the ERN already" in {
      val message = Message("message", "IE801", "messageId", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      await(createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest()))

      verify(workItemService).addWorkItemForErn(eqTo("testErn"), eqTo(false))

    }

    "return an empty array when no messages" in {
      val movement = createMovementWithMessages(Seq.empty)
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe JsArray()
    }

    "return NOT_FOUND when no movement exists for given movementID" in {
      when(movementService.getMovementById(any)).thenReturn(Future.successful(None))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson(
        ErrorResponse(timeStamp, "Movement not found",
          s"Movement $validUUID could not be found")
      )
    }

    "return NOT_FOUND when movement is for a different ern " in {
      val message = Message("message", "IE801", "messageId", Instant.now)
      val movement = Movement(validUUID, "lrn", "boxId", "consignor", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson(
        ErrorResponse(timeStamp, "Invalid MovementID supplied for ERN",
          s"Movement $validUUID is not found within the data for ERNs testErn")
      )
    }

    "catch Future failure from Work Item service and log it but still process submission" in {
      val message = Message("message", "IE801", "messageId", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))
      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
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
    val message = Message(encodeMessage, "IE801", messageId, timeStamp)
    val movementWithMessage = createMovementWithMessages(Seq(message))

    "return 200" in {
      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(movementWithMessage)))

      val result = createWithSuccessfulAuth.getMessageForMovement(validUUID, messageId)(createRequestForXML)

      status(result) mustBe OK
    }

    "return the message as xml for that movement and messageId" in {
      val message1 = Message("encodeMessage", "IE803", UUID.randomUUID().toString, timeStamp)
      val movement = movementWithMessage.copy(messages = Seq(message, message1))
      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessageForMovement(validUUID, messageId)(createRequest())

      status(result) mustBe OK
      contentAsXml(result) mustBe xml.XML.loadString(messageXml)
    }

    "return an error" when {
      "failed authentication" in {
        val result = createWithFailedAuth.getMessageForMovement(validUUID, messageId)(createRequest())

        status(result) mustBe FORBIDDEN
      }

      "movementId is invalid" in {
        val result = createWithSuccessfulAuth.getMessageForMovement(
          "invalidMovementId",
          messageId
        )(createRequest())

        status(result) mustBe BAD_REQUEST
      }

      "movementId is empty" in {
        val result = createWithSuccessfulAuth.getMessageForMovement(
          "",
          messageId
        )(createRequest())

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe MovementIdFormatError
      }

      "return a 404 if movement is not found" in {
        when(movementService.getMovementById(any)).thenReturn(Future.successful(None))

        val result = createWithSuccessfulAuth.getMessageForMovement(
          validUUID,
          messageId
        )(createRequest())

        status(result) mustBe  NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(
            timeStamp,
            "Movement not found",
            s"Movement $validUUID could not be found")
        )
      }

      "return a 404 if message is not found" in {
        val movementWithoutMessages: Movement = movementWithMessage.copy(messages = Seq.empty)
        when(movementService.getMovementById(any))
          .thenReturn(Future.successful(Some(movementWithoutMessages)))

        val result = createWithSuccessfulAuth.getMessageForMovement(
          validUUID,
          messageId
        )(createRequest())

        status(result) mustBe  NOT_FOUND
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(
          timeStamp,
          "No message found for the MovementID provided",
          s"MessageId $messageId was not found in the database")
        )
      }
    }

  }

   private def contentAsXml(result: Future[Result]): Elem = {
    xml.XML.loadString(contentAsString(result))
  }

  private def createMovementWithMessages(messages: Seq[Message]): Movement = {
    Movement(validUUID, "boxId", "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, messages)
  }

  private def createWithSuccessfulAuth =
    new GetMessagesController(
      FakeSuccessAuthentication,
      movementService,
      workItemService,
      new MovementIdValidation,
      cc,
      new EmcsUtils,
      dateTimeService
    )

  private def createWithFailedAuth =
    new GetMessagesController(
      FakeFailingAuthentication,
      movementService,
      workItemService,
      new MovementIdValidation,
      cc,
      new EmcsUtils,
      dateTimeService
    )

  private def createRequest(): FakeRequest[AnyContent] = {
    FakeRequest("GET", "/foo")
  }

  private def createRequestForXML = {
    createRequest()
      .withHeaders(FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml")))
  }
}
