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

import cats.data.NonEmptySeq
import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, when}
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{ErrorResponseSupport, FakeAuthentication}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.{GetMessagesRequestAuditInfo, GetMessagesResponseAuditInfo, GetSpecificMessageRequestAuditInfo, GetSpecificMessageResponseAuditInfo, MessageAuditInfo, UserDetails}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{Consignee, Consignor, IE801Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.MovementIdValidation
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, MessageService, MovementService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class GetMessagesControllerSpec
    extends PlaySpec
    with FakeAuthentication
    with ErrorResponseSupport
    with BeforeAndAfterEach
    with TestXml {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys: ActorSystem     = ActorSystem("GetMessagesControllerSpec")

  private val movementService       = mock[MovementService]
  private val cc                    = stubControllerComponents()
  private val validUUID             = "cfdb20c7-d0b0-4b8b-a071-737d68dede5e"
  private val dateTimeService       = mock[DateTimeService]
  private val timeStamp             = Instant.parse("2020-01-01T01:01:01.123456Z")
  private val messageService        = mock[MessageService]
  private val messageCreatedOn      = Instant.now()
  private val auditService          = mock[AuditService]
  private val messageFactory        = mock[IEMessageFactory]
  private val emcsUtils: EmcsUtils  = new EmcsUtils
  private val MovementIdFormatError = Json.parse("""
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
    when(messageFactory.createFromXml(any, any)).thenReturn(IE801Message.createFromXml(IE801))

  }

  "getMessagesForMovement" should {
    "respond with 200 OK" when {
      "return an empty array when no messages" in {
        val movement = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq.empty)
        when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

        val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

        status(result) mustBe OK
        contentAsJson(result) mustBe JsArray()
      }
      "get all messages when there are multiple available for a movementID" in {
        val ern1 = "testErn"
        val message1  =
          Message(123, emcsUtils.encode(IE801.toString()), "IE801", "messageId1", ern1, Set.empty, messageCreatedOn)
        val message2 =
          Message(345, emcsUtils.encode(IE801.toString()), "IE801", "messageId2", ern1, Set.empty, messageCreatedOn)
        val movement = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message1, message2))
        when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

        val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

        status(result) mustBe OK

        val expectedJson = Seq(
          expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            message1.messageType,
            message1.recipient,
            message1.messageId,
            message1.createdOn
          ),
          expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            message2.messageType,
            message2.recipient,
            message2.messageId,
            message2.createdOn
          )
        )
        contentAsJson(result) mustBe JsArray(expectedJson)
      }
      "get only messages that match erns from request" in {
        val ern1 = "testErn1"
        val ern2 = "testErn2"
        val message1  =
          Message(123, emcsUtils.encode(IE801.toString()), "IE801", "messageId1", ern1, Set.empty, messageCreatedOn)
        val message2 =
          Message(345, emcsUtils.encode(IE801.toString()), "IE801", "messageId2", ern2, Set.empty, messageCreatedOn)
        val movement = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message1, message2))
        when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

        val result = createWithSuccessfulAuth(Set(ern1)).getMessagesForMovement(validUUID, None, None)(createRequest())

        status(result) mustBe OK

        val expectedJson = Seq(
          expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            message1.messageType,
            message1.recipient,
            message1.messageId,
            message1.createdOn
          )
        )
        contentAsJson(result) mustBe JsArray(expectedJson)
      }
      "filter messages when query parameters are provided" when {
        "traderType parameter is valid - consignor"  in {
          val ern1 = "testErn1"
          val ern2 = "testErn2"
          val message1  =
            Message(123, emcsUtils.encode(IE801.toString()), "IE801", "messageId1", ern1, Set.empty, messageCreatedOn)
          val message2 =
            Message(345, emcsUtils.encode(IE801.toString()), "IE801", "messageId2", ern2, Set.empty, messageCreatedOn)
          val movement = Movement(validUUID, Some("boxId"), "lrn", ern1, Some(ern2), Some("arc"), Instant.now, Seq(message1, message2))

          when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

          val result = createWithSuccessfulAuth(Set(ern1, ern2)).getMessagesForMovement(validUUID, None, Some(Consignor.name.toLowerCase))(createRequest())
          status(result) mustBe OK

          verify(messageService).updateAllMessages(eqTo(Set(ern1, ern2)))(any)

          val expectedJson = Seq(
            expectedMessageResponseAsJson(
              emcsUtils.encode(IE801.toString()),
              message1.messageType,
              message1.recipient,
              message1.messageId,
              message1.createdOn
            )
          )
          contentAsJson(result) mustBe JsArray(expectedJson)

          withClue("Submits getInformation audit event") {
            val requestAuditInfo = GetMessagesRequestAuditInfo(validUUID, None, Some(Consignor.name.toLowerCase))
            val messageAuditInfo = MessageAuditInfo(
                message1.messageId,
                Some("PORTAL6de1b822562c43fb9220d236e487c920"),
                message1.messageType,
                "MovementGenerated",
                message1.recipient,
                message1.createdOn
              )
            val responseAuditInfo = GetMessagesResponseAuditInfo(1, Seq(messageAuditInfo), movement.localReferenceNumber, movement.administrativeReferenceCode, movement.consignorId, movement.consigneeId)
            val userDetails = UserDetails("testInternalId", "testGroupId")
            val authExciseNumber = NonEmptySeq(ern1, Seq(ern2))

            verify(auditService, times(1))
              .getInformationForGetMessages(
                eqTo(requestAuditInfo),
                eqTo(responseAuditInfo),
                eqTo(userDetails),
                eqTo(authExciseNumber)
              )(
                any
              )
            }
          }
        "traderType is valid - consignee" in { //TODO am up to here for organising
          val message  =
            Message(123, emcsUtils.encode(IE801.toString()), "IE801", "messageId", "testErn", Set.empty, messageCreatedOn)
          val movement =
            Movement(validUUID, Some("boxId"), "lrn", "consignor", Some("testErn"), Some("arc"), Instant.now, Seq(message))
          when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

          val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

          status(result) mustBe OK
          contentAsJson(result) mustBe JsArray(
            Seq(
              expectedMessageResponseAsJson(
                emcsUtils.encode(IE801.toString()),
                "IE801",
                "testErn",
                "messageId",
                messageCreatedOn
              )
            )
          )
        }
        "return only messages that match consignorId" in {
          val timeInFuture = Instant.now.plusSeconds(1000)

          val message1 = Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId", ern, Set.empty, timeInFuture)
          val message2 = Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId", ern, Set.empty, timeInFuture)
          val message3 =
            Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId", "otherErn", Set.empty, timeInFuture)
          val movement = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message1, message2, message3))
          when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

          val result =
            createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, Some("consignor"))(createRequest())

          status(result) mustBe OK

          val expectedJson1 = expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            "IE801",
            ern,
            "messageId",
            timeInFuture
          )
          val expectedJson2 = expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            "IE801",
            ern,
            "messageId",
            timeInFuture
          )
          contentAsJson(result) mustBe JsArray(Seq(expectedJson1, expectedJson2))

        }
        "return only messages that match consigneeId" in {
          val timeInFuture = Instant.now.plusSeconds(1000)

          val message1 =
            Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId", "consigneeId", Set.empty, timeInFuture)
          val message2 = Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId", ern, Set.empty, timeInFuture)
          val message3 =
            Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId", "otherErn", Set.empty, timeInFuture)
          val movement = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message1, message2, message3))
          when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

          val result = createWithSuccessfulAuth(Set("other_ern", "consigneeId"))
            .getMessagesForMovement(validUUID, None, Some("consignee"))(createRequest())

          status(result) mustBe OK

          val expectedJson1 = expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            "IE801",
            "consigneeId",
            "messageId",
            timeInFuture
          )
          contentAsJson(result) mustBe JsArray(Seq(expectedJson1))

        }
        "get all the new messages when there is a time query parameter provided" in {
          val timeInFuture = Instant.now.plusSeconds(1000)
          val timeInPast   = Instant.now.minusSeconds(1000)
          val message      =
            Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId1", "testErn", Set.empty, timeInFuture)
          val message2     = Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId2", "ern", Set.empty, timeInPast)
          val movement     = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message, message2))
          when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

          val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some(messageCreatedOn.toString), None)(
            createRequest()
          )

          status(result) mustBe OK

          val jsonResponse = expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            "IE801",
            "testErn",
            "messageId1",
            timeInFuture
          )
          contentAsJson(result) mustBe JsArray(Seq(jsonResponse))
        }
        "get all the new messages including messages with a createdOn time of NOW when there is a time query parameter provided" in {
          val timeNowString = messageCreatedOn.toString
          val timeInFuture  = Instant.now.plusSeconds(1000)
          val timeInPast    = Instant.now.minusSeconds(1000)
          val message       =
            Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId1", "testErn", Set.empty, timeInFuture)
          val message2      = Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId2", "ern", Set.empty, timeInPast)
          val message3      =
            Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId3", "testErn", Set.empty, messageCreatedOn)
          val movement      = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message, message2, message3))
          when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

          val result =
            createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some(timeNowString), None)(createRequest())

          status(result) mustBe OK

          val expectedJson = Seq(
            expectedMessageResponseAsJson(
              emcsUtils.encode(IE801.toString()),
              "IE801",
              "testErn",
              "messageId1",
              timeInFuture
            ),
            expectedMessageResponseAsJson(
              emcsUtils.encode(IE801.toString()),
              "IE801",
              "testErn",
              "messageId3",
              messageCreatedOn
            )
          )
          contentAsJson(result) mustBe JsArray(expectedJson)
        }
        "succeed when a valid date format is provided" in {
          val timeInFuture = Instant.now.plusSeconds(1000)
          val message      =
            Message(emcsUtils.encode(IE801.toString()), "IE801", "messageId", "testErn", Set.empty, timeInFuture)
          val movement     = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
          when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

          val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some("2020-11-15T17:02:34.00Z"), None)(
            createRequest()
          )

          status(result) mustBe OK

          val expectedJson = expectedMessageResponseAsJson(
            emcsUtils.encode(IE801.toString()),
            "IE801",
            "testErn",
            "messageId",
            timeInFuture
          )
          contentAsJson(result) mustBe JsArray(Seq(expectedJson))
        }
      }
    }
    "respond with 400 BAD_REQUEST" when {
      "respond with BAD_REQUEST when the MovementID is an invalid UUID" in {
        val result = createWithSuccessfulAuth().getMessagesForMovement("invalidUUID", None, None)(createRequest())

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe MovementIdFormatError
      }
      "respond with BAD_REQUEST when invalid traderType has passed" in {
        val result = createWithSuccessfulAuth()
          .getMessagesForMovement(UUID.randomUUID().toString, None, Some("invalidTraderType"))(createRequest())

        status(result) mustBe BAD_REQUEST
      }
      "fail when an invalid date format is provided" in {
        val timeInFuture = Instant.now.plusSeconds(1000)
        val message      = Message("message", "IE801", "messageId", "ern", Set.empty, timeInFuture)
        val movement     = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
        when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

        val result =
          createWithSuccessfulAuth().getMessagesForMovement(validUUID, Some("invalid date"), None)(createRequest())

        status(result) mustBe BAD_REQUEST

        contentAsJson(result) mustBe expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "Invalid date format provided in the updatedSince query parameter",
          "Date format should be like '2020-11-15T17:02:34.00Z'"
        )
      }
    }
    "respond with 404 NOT_FOUND when no movement exists for given movementID" in {
      when(movementService.getMovementById(any)).thenReturn(Future.successful(None))

      val result = createWithSuccessfulAuth().getMessagesForMovement(validUUID, None, None)(createRequest())

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe expectedJsonErrorResponse(
        "2020-01-01T01:01:01.123Z",
        "Movement not found",
        s"Movement $validUUID could not be found"
      )
    }
    "respond with 403 FORBIDDEN when movement is for a different ern " in {
      val message  = Message("message", "IE801", "messageId", "ern", Set.empty, Instant.now)
      val movement = Movement(
        validUUID,
        Some("boxId"),
        "lrn",
        "consignor",
        Some("consigneeId"),
        Some("arc"),
        Instant.now,
        Seq(message)
      )
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

    val messageId           = UUID.randomUUID().toString
    val message             = Message(encodeMessage, "IE801", messageId, "ern", Set.empty, timeStamp)
    val movementWithMessage = Movement(validUUID, Some("boxId"), "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))

    "return 200" in {
      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(movementWithMessage)))

      val result = createWithSuccessfulAuth().getMessageForMovement(validUUID, messageId)(createRequest())

      status(result) mustBe OK

      withClue("Submits getInformation audit event") {

        val request          = GetSpecificMessageRequestAuditInfo(validUUID, messageId)
        val response         =
          GetSpecificMessageResponseAuditInfo(
            Some("PORTAL6de1b822562c43fb9220d236e487c920"),
            "IE801",
            "MovementGenerated",
            "lrn",
            Some("arc"),
            movementWithMessage.consignorId,
            movementWithMessage.consigneeId
          )
        val userDetails      = UserDetails("testInternalId", "testGroupId")
        val authExciseNumber = NonEmptySeq("testErn", Seq.empty[String])

        verify(auditService, times(1))
          .getInformationForGetSpecificMessage(
            eqTo(request),
            eqTo(response),
            eqTo(userDetails),
            eqTo(authExciseNumber)
          )(
            any
          )
      }
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

  private def contentAsXml(result: Future[Result]): Elem =
    xml.XML.loadString(contentAsString(result))

  private def createWithSuccessfulAuth(erns: Set[String] = Set(ern)) =
    new GetMessagesController(
      FakeSuccessAuthentication(erns),
      new ValidateAcceptHeaderAction(dateTimeService),
      movementService,
      messageService,
      new MovementIdValidation,
      cc,
      new EmcsUtils,
      dateTimeService,
      auditService,
      messageFactory
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
      dateTimeService,
      auditService,
      messageFactory
    )

  private def createRequest(
    acceptHeader: String = "application/vnd.hmrc.1.0+xml"
  ): FakeRequest[AnyContent] =
    FakeRequest()
      .withHeaders(
        FakeHeaders(
          Seq(
            HeaderNames.ACCEPT -> acceptHeader
          )
        )
      )

  private def formatToValidDateTime(dateTime: Instant): String =
    ZonedDateTime
      .ofInstant(dateTime, ZoneOffset.UTC)
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))

  private def expectedMessageResponseAsJson(
    encodedMessage: String,
    messageType: String,
    recipient: String,
    messageId: String,
    dateTime: Instant
  ): JsValue =
    Json.parse(s"""
         |{
         |   "encodedMessage":"$encodedMessage",
         |   "messageType":"$messageType",
         |   "recipient":"$recipient",
         |   "messageId":"$messageId",
         |   "createdOn": "${formatToValidDateTime(dateTime)}"
         | }
         |""".stripMargin)
}
