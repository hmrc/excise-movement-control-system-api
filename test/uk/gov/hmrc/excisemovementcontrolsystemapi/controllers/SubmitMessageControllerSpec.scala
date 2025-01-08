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
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Forbidden}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.CorrelationIdAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{ErrorResponseSupport, FakeAuthentication, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EISErrorResponseDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class SubmitMessageControllerSpec
    extends PlaySpec
    with FakeAuthentication
    with FakeXmlParsers
    with BeforeAndAfterEach
    with TestXml
    with ErrorResponseSupport {

  implicit val ec: ExecutionContext    = ExecutionContext.Implicits.global
  private val cc                       = stubControllerComponents()
  private val request                  = createRequest(IE818)
  private val correlationIdAction      = new CorrelationIdAction
  private val submissionMessageService = mock[SubmissionMessageService]
  private val movementService          = mock[MovementService]
  private val messageValidation        = mock[MessageValidation]
  private val movementValidation       = mock[MovementIdValidation]
  private val dateTimeService          = mock[DateTimeService]
  private val auditService             = mock[AuditService]

  private val consignorId = "testErn"
  private val movement    =
    Movement(Some("boxId"), "LRNQA20230909022221", consignorId, Some("GBWK002281023"), Some("23GB00000000000377161"))
  private val timestamp   = Instant.parse("2024-06-12T14:13:15.1234567Z")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(submissionMessageService, movementService, movementValidation, auditService)

    when(submissionMessageService.submit(any, any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))

    when(movementValidation.validateMovementId(any)).thenReturn(Right(movement._id))
    when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

    when(messageValidation.validateSubmittedMessage(any, any, any)).thenReturn(Right(consignorId))
    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(auditService.auditMessage(any[IEMessage])(any)).thenReturn(EitherT.fromEither(Right(())))
    when(auditService.auditMessage(any[IEMessage], any)(any)).thenReturn(EitherT.fromEither(Right(())))

  }

  "submit" should {
    "return 202" in {
      val result = createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request)
      status(result) mustBe ACCEPTED
    }

    "send a request to EIS" in {

      await(createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request))

      verify(submissionMessageService).submit(any, any)(any)

    }

    "send an audit event" in {
      await(createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request))

      verify(auditService, times(1)).auditMessage(any[IEMessage])(any)
      verify(auditService, times(1)).messageSubmitted(any, any, any, eqTo("testCorrelationId"), any)(any)
    }

    "sends a failure audit when a message isn't submitted" in {
      val testError = EISErrorResponseDetails(BAD_REQUEST, timestamp, "", "", "", None)
      when(submissionMessageService.submit(any, any)(any)).thenReturn(Future.successful(Left(testError)))
      await(createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request))

      verify(auditService, times(1)).auditMessage(any, any)(any)
      verify(auditService, times(1)).messageSubmitted(any, any, any, any, any)(any)
    }

    "return an error when EIS errors" in {
      val codes = Seq(NOT_FOUND, NOT_ACCEPTABLE, IM_A_TEAPOT)
      codes.foreach { code =>
        val testError = EISErrorResponseDetails(code, timestamp, "", "", "", None)
        when(submissionMessageService.submit(any, any)(any))
          .thenReturn(Future.successful(Left(testError)))

        val result = createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request)

        status(result) mustBe code
      }

    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request)

        status(result) mustBe FORBIDDEN
      }
    }

    "a validation parser error" when {
      "xml cannot be parsed" in {
        val result = createWithFailingXmlParserAction.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request)

        status(result) mustBe BAD_REQUEST
      }
    }

    "handle movement validation errors" should {

      "return a 400 Bad Request" when {

        "movement id is invalid" in {

          val expectedError = expectedJsonErrorResponse(
            "2024-06-12T14:13:15.123Z",
            "Movement Id format error",
            "The movement ID should be a valid UUID"
          )

          when(movementValidation.validateMovementId(any)).thenReturn(Left(MovementIdFormatInvalid()))
          when(movementValidation.convertErrorToResponse(eqTo(MovementIdFormatInvalid()), eqTo(timestamp)))
            .thenReturn(BadRequest(expectedError))

          val result = createWithSuccessfulAuth.submit("b405")(request)

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe expectedError
        }
      }

      "return a 404 Not Found" when {

        "movement does not exist in database" in {

          val expectedError = expectedJsonErrorResponse(
            "2024-06-12T14:13:15.123Z",
            "Movement not found",
            "Movement 49491927-aaa1-4835-b405-dd6e7fa3aaf0 could not be found"
          )

          val movementId = "49491927-aaa1-4835-b405-dd6e7fa3aaf0"
          when(movementValidation.validateMovementId(any)).thenReturn(Right(movementId))
          when(movementService.getMovementById(any)).thenReturn(Future.successful(None))

          val result = createWithSuccessfulAuth.submit(movementId)(request)

          status(result) mustBe NOT_FOUND

          contentAsJson(result) mustBe expectedError
        }
      }

    }

    "handle message validation errors" should {

      "return a 400 Bad Request" when {

        "message validation error is turned into a bad request" in {

          case object TestMessageDoesNotMatchMovement extends MessageDoesNotMatchMovement("Consignor")

          val expectedError = expectedJsonErrorResponse(
            "2024-06-12T14:13:15.123Z",
            "Message does not match movement",
            "The Consignor in the message does not match the Consignor in the movement"
          )

          when(messageValidation.validateSubmittedMessage(any, any, any))
            .thenReturn(Left(TestMessageDoesNotMatchMovement))
          when(messageValidation.convertErrorToResponse(eqTo(TestMessageDoesNotMatchMovement), eqTo(timestamp)))
            .thenReturn(BadRequest(Json.toJson(expectedError)))

          val result = createWithSuccessfulAuth.submit(movement._id)(request)

          status(result) mustBe BAD_REQUEST

          contentAsJson(result) mustBe expectedError
        }

      }

      "return a 403 Forbidden" when {

        "message validation converts the error into a Forbidden error" in {

          case object TestMessageIdentifierIsUnauthorised
              extends MessageIdentifierIsUnauthorised(MessageValidation.consignor)

          val expectedError = expectedJsonErrorResponse(
            "2024-06-12T14:13:15.123Z",
            "Message cannot be sent",
            "The Consignor is not authorised to submit this message for the movement"
          )

          when(messageValidation.validateSubmittedMessage(any, any, any))
            .thenReturn(Left(TestMessageIdentifierIsUnauthorised))
          when(messageValidation.convertErrorToResponse(eqTo(TestMessageIdentifierIsUnauthorised), eqTo(timestamp)))
            .thenReturn(Forbidden(Json.toJson(expectedError)))

          val result = createWithSuccessfulAuth.submit(movement._id)(request)

          status(result) mustBe FORBIDDEN

          contentAsJson(result) mustBe expectedError
        }

      }

    }

  }

  private def createWithSuccessfulAuth =
    new SubmitMessageController(
      FakeSuccessAuthentication(Set(ern)),
      FakeSuccessXMLParser(mock[IEMessage]),
      submissionMessageService,
      movementService,
      auditService,
      messageValidation,
      movementValidation,
      dateTimeService,
      cc,
      correlationIdAction
    )

  private def createRequest(body: Elem): FakeRequest[Elem] =
    FakeRequest("POST", "/foo")
      .withHeaders(
        FakeHeaders(
          Seq(HeaderNames.CONTENT_TYPE -> "application/xml", HttpHeader.xCorrelationId -> "testCorrelationId")
        )
      )
      .withBody(body)

  private def createWithAuthActionFailure =
    new SubmitMessageController(
      FakeFailingAuthentication,
      FakeSuccessXMLParser(mock[IEMessage]),
      submissionMessageService,
      movementService,
      auditService,
      messageValidation,
      movementValidation,
      dateTimeService,
      cc,
      correlationIdAction
    )

  private def createWithFailingXmlParserAction =
    new SubmitMessageController(
      FakeSuccessAuthentication(Set(ern)),
      FakeFailureXMLParser,
      submissionMessageService,
      movementService,
      auditService,
      messageValidation,
      movementValidation,
      dateTimeService,
      cc,
      correlationIdAction
    )

}
