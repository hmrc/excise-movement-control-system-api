/*
 * Copyright 2024 HM Revenue & Customs
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
import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, verifyZeroInteractions, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EISErrorResponseDetails, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IE818Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.{MessageIdentifierIsUnauthorised, MessageValidation}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ErnSubmissionRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
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
  private val movementService          = mock[MovementService]
  private val cc                       = stubControllerComponents()
  private val request                  = createRequestWithClientId
  private val mockIeMessage            = mock[IE815Message]
  private val boxIdRepository          = mock[BoxIdRepository]
  private val ernSubmissionRepository  = mock[ErnSubmissionRepository]
  private val notificationService      = mock[PushNotificationService]
  private val messageValidation        = mock[MessageValidation]
  private val dateTimeService          = mock[DateTimeService]
  private val auditService             = mock[AuditService]
  private val appConfig                = mock[AppConfig]
  private val consignorId              = "456"
  private val timestamp                = Instant.parse("2024-05-06T15:30:15.12345612Z")
  private val defaultBoxId             = "boxId"
  private val clientBoxId              = "clientBoxId"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      submissionMessageService,
      movementService,
      submissionMessageService,
      notificationService,
      auditService,
      boxIdRepository,
      ernSubmissionRepository
    )

    when(submissionMessageService.submit(any, any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))
    when(notificationService.getBoxId(any, any)(any))
      .thenReturn(Future.successful(Right(defaultBoxId)))

    when(messageValidation.validateDraftMovement(any, any)).thenReturn(Right(consignorId))
    when(dateTimeService.timestamp()).thenReturn(timestamp)

    when(mockIeMessage.consigneeId).thenReturn(Some("789"))
    when(mockIeMessage.consignorId).thenReturn(consignorId)
    when(mockIeMessage.localReferenceNumber).thenReturn("123")

    when(appConfig.pushNotificationsEnabled).thenReturn(true)
    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(auditService.auditMessage(any[IEMessage])(any)).thenReturn(EitherT.fromEither(Right(())))
    when(ernSubmissionRepository.save(any)).thenReturn(Future.successful(Done))
  }

  def createTestError(status: Int) = EISErrorResponseDetails(status, timestamp, "", "", "", None)

  "submit" should {

    "return 202" when {

      "push pull notifications feature flag is enabled" in {
        when(movementService.saveNewMovement(any))
          .thenReturn(
            Future.successful(Right(Movement(Some(defaultBoxId), "123", consignorId, Some("789"), None, Instant.now)))
          )

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
          val captor      = ArgCaptor[Movement]
          verify(movementService).saveNewMovement(captor.capture)
          val newMovement = captor.value
          newMovement.localReferenceNumber mustBe "123"
          newMovement.consignorId mustBe consignorId
          newMovement.consigneeId mustBe Some("789")
          newMovement.administrativeReferenceCode mustBe None
          newMovement.messages mustBe Seq.empty
          newMovement.boxId mustBe Some(defaultBoxId)
        }
      }

      "push pull notifications feature flag is disabled" in {
        when(appConfig.pushNotificationsEnabled).thenReturn(false)

        when(movementService.saveNewMovement(any))
          .thenReturn(
            Future.successful(Right(Movement(Some(defaultBoxId), "123", consignorId, Some("789"), None, Instant.now)))
          )

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe ACCEPTED

        withClue("should not access the notification service") {
          verify(notificationService, times(0)).getBoxId(any, any)(any)
        }

        withClue("should save the new movement with no box id") {
          val captor      = ArgCaptor[Movement]
          verify(movementService).saveNewMovement(captor.capture)
          val newMovement = captor.value
          newMovement.boxId mustBe None
        }

      }
    }

    "pass the Client Box id to notification service when is present" in {
      when(movementService.saveNewMovement(any))
        .thenReturn(
          Future.successful(Right(Movement(Some(defaultBoxId), "123", consignorId, Some("789"), None, Instant.now)))
        )

      val result = createWithSuccessfulAuth.submit(createRequestWithClientBoxId)

      status(result) mustBe ACCEPTED
      verify(notificationService).getBoxId(eqTo("clientId"), eqTo(Some(clientBoxId)))(any)
    }

    "sends expected audit events" in {
      when(movementService.saveNewMovement(any))
        .thenReturn(
          Future.successful(Right(Movement(Some(defaultBoxId), "123", consignorId, Some("789"), None, Instant.now)))
        )

      when(auditService.auditMessage(any[IEMessage])(any)).thenReturn(EitherT.fromEither(Right(())))

      await(createWithSuccessfulAuth.submit(request))

      verify(auditService).auditMessage(any[IEMessage])(any)
      verify(auditService).messageSubmitted(any, any, any, any, any)(any)
    }

    "sends a failure audit when a message isn't submitted" in {
      when(submissionMessageService.submit(any, any)(any))
        .thenReturn(Future.successful(Left(createTestError(BAD_REQUEST))))

      await(createWithSuccessfulAuth.submit(request))

      verify(auditService).auditMessage(any, any)(any)
    }

    "sends failure audits when a message submits but doesn't save" in {
      when(movementService.saveNewMovement(any)).thenReturn(Future.successful(Left(BadRequest(""))))

      await(createWithSuccessfulAuth.submit(request))

      verify(auditService).auditMessage(any, any)(any)
      verify(auditService).messageSubmitted(any, any, any, any, any)(any)

    }

    "adds the boxId to the BoxIdRepository for consignor" in {
      when(movementService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement(Some(defaultBoxId), "lrn", consignorId, None))))

      await(createWithSuccessfulAuth.submit(request))

      verify(boxIdRepository).save(consignorId, defaultBoxId)
    }

    "return an error" when {

      "message is wrong type" in {
        val result = createWithWrongMessageType.submit(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe expectedJsonResponse(
          "Invalid message type",
          "Message type IE818 cannot be sent to the draft excise movement endpoint"
        )

      }

      "not authorised to send message" in {
        case object TestMessageIdentifierIsUnauthorised
            extends MessageIdentifierIsUnauthorised(MessageValidation.consignor)

        val expectedError = expectedJsonResponse(
          "Message cannot be sent",
          "The Consignor is not authorised to submit this message for the movement"
        )

        when(messageValidation.validateDraftMovement(any, any)).thenReturn(Left(TestMessageIdentifierIsUnauthorised))
        when(messageValidation.convertErrorToResponse(eqTo(TestMessageIdentifierIsUnauthorised), eqTo(timestamp)))
          .thenReturn(Forbidden(Json.toJson(expectedError)))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe expectedError

      }

      "get box id returns an error" in {
        when(notificationService.getBoxId(any, any)(any))
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
          .thenReturn(Future.successful(Left(createTestError(NOT_FOUND))))

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

      "XML is not a IE815 message" in {
        when(movementService.saveNewMovement(any))
          .thenReturn(
            Future.successful(Right(Movement(Some(defaultBoxId), "123", "456", Some("789"), None, Instant.now)))
          )

        val result = createWithWrongMessageType.submit(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe expectedJsonResponse(
          "Invalid message type",
          "Message type IE818 cannot be sent to the draft excise movement endpoint"
        )
      }
    }
  }

  private def expectedJsonResponse(message: String, debugMessage: String) =
    Json.parse(s"""
        |{
        |   "dateTime":"2024-05-06T15:30:15.123Z",
        |   "message":"$message",
        |   "debugMessage": "$debugMessage"
        |}
        |""".stripMargin)

  private def createWithWrongMessageType = {

    val mockIe818Message = mock[IE818Message]

    when(mockIe818Message.messageType).thenReturn(MessageTypes.IE818.value)

    new DraftExciseMovementController(
      FakeSuccessAuthentication(Set(ern)),
      FakeSuccessXMLParser(mockIe818Message),
      movementService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      boxIdRepository,
      ernSubmissionRepository,
      appConfig,
      cc
    )
  }

  private def createWithAuthActionFailure =
    new DraftExciseMovementController(
      FakeFailingAuthentication,
      FakeSuccessXMLParser(mockIeMessage),
      movementService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      boxIdRepository,
      ernSubmissionRepository,
      appConfig,
      cc
    )

  private def createWithFailingXmlParserAction =
    new DraftExciseMovementController(
      FakeSuccessAuthentication(Set(ern)),
      FakeFailureXMLParser,
      movementService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      boxIdRepository,
      ernSubmissionRepository,
      appConfig,
      cc
    )

  private def createWithSuccessfulAuth =
    new DraftExciseMovementController(
      FakeSuccessAuthentication(Set(ern)),
      FakeSuccessXMLParser(mockIeMessage),
      movementService,
      submissionMessageService,
      notificationService,
      messageValidation,
      dateTimeService,
      auditService,
      boxIdRepository,
      ernSubmissionRepository,
      appConfig,
      cc
    )

  private def createRequestWithClientId: FakeRequest[Elem] =
    createRequest(
      Seq(
        HeaderNames.CONTENT_TYPE -> "application/xml",
        "X-Client-Id"            -> "clientId"
      )
    )

  private def createRequestWithClientBoxId: FakeRequest[Elem] =
    createRequest(
      Seq(
        HeaderNames.CONTENT_TYPE -> "application/xml",
        "X-Client-Id"            -> "clientId",
        "X-Callback-Box-Id"      -> clientBoxId
      )
    )

  private def createRequestWithoutClientId: FakeRequest[Elem]                                      =
    createRequest(Seq(HeaderNames.CONTENT_TYPE -> "application/xml"))

  private def createRequest(headers: Seq[(String, String)], body: Elem = IE815): FakeRequest[Elem] =
    FakeRequest()
      .withHeaders(FakeHeaders(headers))
      .withBody(body)
}
