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


import cats.data.EitherT
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.mockito.captor.ArgCaptor
import org.mongodb.scala.MongoException
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, NotFound}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IE818Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.SuccessBoxNotificationResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.{MessageIdentifierIsUnauthorised, MessageValidation}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, MovementService, PushNotificationService, SubmissionMessageService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class DraftExciseMovementControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeXmlParsers
    with TestXml
    with BeforeAndAfterEach
    with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val submissionMessageService = mock[SubmissionMessageService]
  private val movementService = mock[MovementService]
  private val cc = stubControllerComponents()
  private val request = createRequestWithClientId
  private val mockIeMessage = mock[IE815Message]
  private val workItemService = mock[WorkItemService]
  private val notificationService = mock[PushNotificationService]
  private val messageValidation = mock[MessageValidation]
  private val dateTimeService = mock[DateTimeService]
  private val auditService = mock[AuditService]
  private val defaultBoxId = "boxId"
    private val clientBoxId = "clientBoxId"
  private val timestamp = Instant.now
  private val consignorId = "456"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(submissionMessageService, movementService, workItemService, submissionMessageService, auditService)

    when(submissionMessageService.submit(any, any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))
    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))
    when(notificationService.getBoxId(any, any)(any))
      .thenReturn(Future.successful(Right(SuccessBoxNotificationResponse(defaultBoxId))))

    when(messageValidation.validateDraftMovement(any, any)).thenReturn(Right(consignorId))
    when(dateTimeService.timestamp()).thenReturn(timestamp)

    when(mockIeMessage.consigneeId).thenReturn(Some("789"))
    when(mockIeMessage.consignorId).thenReturn(consignorId)
    when(mockIeMessage.localReferenceNumber).thenReturn("123")
    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(auditService.auditMessage(any)(any)).thenReturn(EitherT.fromEither(Right(())))
  }

  "submit" should {

    "return 202" in {
      when(movementService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement(defaultBoxId, "123", "456", Some("789"), None, Instant.now))))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe ACCEPTED

      withClue("submit the message") {
        val captor = ArgCaptor[ParsedXmlRequest[_]]
        verify(submissionMessageService).submit(captor.capture, any)(any)
        captor.value.ieMessage mustBe mockIeMessage
      }

      withClue("should get the box id") {
        verify(notificationService).getBoxId(eqTo("clientId"), eqTo(None))(any)
      }

      withClue("should save the new movement") {
        val captor = ArgCaptor[Movement]
        verify(movementService).saveNewMovement(captor.capture)
        val newMovement = captor.value
        newMovement.localReferenceNumber mustBe "123"
        newMovement.consignorId mustBe consignorId
        newMovement.consigneeId mustBe Some("789")
        newMovement.administrativeReferenceCode mustBe None
        newMovement.messages mustBe Seq.empty
        newMovement.boxId mustBe defaultBoxId
      }
    }

    "pass the Client Box id to notification service when is present" in {
      when(movementService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement(defaultBoxId, "123", "456", Some("789"), None, Instant.now))))

      val result = createWithSuccessfulAuth.submit(createRequestWithClientBoxId)

      status(result) mustBe ACCEPTED
      verify(notificationService).getBoxId(eqTo("clientId"), eqTo(Some(clientBoxId)))(any)
    }

    "sends an audit event" in {
      when(movementService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement(defaultBoxId, "123", consignorId, Some("789"), None, Instant.now))))

      when(auditService.auditMessage(any)(any)).thenReturn(EitherT.fromEither(Right(())))

      await(createWithSuccessfulAuth.submit(request))

      verify(auditService).auditMessage(any)(any)
    }

    "call the add work item routine to create or update the database" in {
      when(movementService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement(defaultBoxId, "lrn", ern, None))))

      await(createWithSuccessfulAuth.submit(request))

      verify(workItemService).addWorkItemForErn(consignorId, fastMode = true)
    }

    "return ACCEPTED if failing to add workItem " in {
      when(movementService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement(defaultBoxId, "lrn", ern, None))))
      when(workItemService.addWorkItemForErn(any, any))
        .thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe ACCEPTED
    }

    "return an error" when {

      "message is wrong type" in {
        val result = createWithWrongMessageType.submit(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(
          timestamp,
          "Invalid message type",
          "Message type IE818 cannot be sent to the draft excise movement endpoint"
        ))

      }

      "not authorised to send message" in {
        case object TestMessageIdentifierIsUnauthorised extends MessageIdentifierIsUnauthorised(MessageValidation.consignor)

        val expectedError = ErrorResponse(
          timestamp,
          "Message cannot be sent",
          "The Consignor is not authorised to submit this message for the movement"
        )

        when(messageValidation.validateDraftMovement(any, any)).thenReturn(Left(TestMessageIdentifierIsUnauthorised))
        when(messageValidation.convertErrorToResponse(eqTo(TestMessageIdentifierIsUnauthorised), eqTo(timestamp))).thenReturn(Forbidden(Json.toJson(expectedError)))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.toJson(expectedError)

      }

      "get box id returns an error" in {
        when(notificationService.getBoxId(any,any)(any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe BAD_REQUEST

        withClue("should not submit the message") {
          verifyZeroInteractions(submissionMessageService)
        }
      }

      "clientId is not available" in {
        val result = createWithSuccessfulAuth.submit(createRequestWithoutClientId)

        status(result) mustBe BAD_REQUEST
      }

      "cannot submit a message" in {
        when(submissionMessageService.submit(any, any)(any))
          .thenReturn(Future.successful(Left(NotFound("not found"))))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe NOT_FOUND
      }

      "message xml cannot be parsed" in {
        val result = createWithFailingXmlParserAction.submit(request)

        status(result) mustBe BAD_REQUEST
      }

      "authentication fails" in {
        val result = createWithAuthActionFailure.submit(request)

        status(result) mustBe FORBIDDEN
      }

      "cannot save the movement" in {
        when(movementService.saveNewMovement(any))
          .thenReturn(Future.successful(Left(InternalServerError("error"))))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "not XMl is not a IE815 message" in {
        when(movementService.saveNewMovement(any))
          .thenReturn(Future.successful(Right(Movement(defaultBoxId, "123", "456", Some("789"), None, Instant.now))))

        val result = createWithWrongMessageType.submit(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(ErrorResponse(
          timestamp,
          "Invalid message type",
          "Message type IE818 cannot be sent to the draft excise movement endpoint"
        ))
      }
    }
  }

  private def createWithWrongMessageType = {

    val mockIe818Message = mock[IE818Message]

    when(mockIe818Message.messageType).thenReturn(MessageTypes.IE818.value)

    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser(mockIe818Message),
      movementService,
      workItemService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      cc
    )
  }
  private def createWithAuthActionFailure =
    new DraftExciseMovementController(
      FakeFailingAuthentication,
      FakeSuccessXMLParser(mockIeMessage),
      movementService,
      workItemService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      cc
    )

  private def createWithFailingXmlParserAction =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeFailureXMLParser,
      movementService,
      workItemService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      cc
    )

  private def createWithSuccessfulAuth =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser(mockIeMessage),
      movementService,
      workItemService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      cc
    )

  private def createRequestWithClientId: FakeRequest[Elem] = {
    createRequest(Seq(
        HeaderNames.CONTENT_TYPE -> "application/xml",
        "X-Client-Id" -> "clientId"
      ))
  }

  private def createRequestWithClientBoxId: FakeRequest[Elem] = {
    createRequest(Seq(
        HeaderNames.CONTENT_TYPE -> "application/xml",
        "X-Client-Id" -> "clientId",
        "X-Callback-Box-Id" -> clientBoxId
      ))
  }

  private def createRequestWithoutClientId: FakeRequest[Elem] = {
    createRequest(Seq(HeaderNames.CONTENT_TYPE -> "application/xml"))
  }

  private def createRequest(headers: Seq[(String, String)], body: Elem = IE815): FakeRequest[Elem] = {
    FakeRequest()
      .withHeaders(FakeHeaders(headers))
      .withBody(body)
  }
}
