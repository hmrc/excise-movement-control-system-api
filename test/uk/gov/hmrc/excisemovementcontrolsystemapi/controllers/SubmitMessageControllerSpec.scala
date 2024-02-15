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
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mongodb.scala.MongoException
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Forbidden, NotFound}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
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
    with TestXml {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val request = createRequest(IE818)
  private val submissionMessageService = mock[SubmissionMessageService]
  private val movementService = mock[MovementService]
  private val workItemService = mock[WorkItemService]
  private val messageValidation = mock[MessageValidation]
  private val movementValidation = mock[MovementIdValidation]
  private val dateTimeService = mock[DateTimeService]
  private val auditService = mock[AuditService]

  private val consignorId = "testErn"
  private val movement = Movement(Some("boxId"), "LRNQA20230909022221", consignorId, Some("GBWK002281023"), Some("23GB00000000000377161"))
  private val timestamp = Instant.now

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(submissionMessageService, workItemService, movementService, movementValidation, auditService)

    when(submissionMessageService.submit(any, any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))
    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))

    when(movementValidation.validateMovementId(any)).thenReturn(Right(movement._id))
    when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

    when(messageValidation.validateSubmittedMessage(any, any, any)).thenReturn(Right(consignorId))
    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(auditService.auditMessage(any)(any)).thenReturn(EitherT.fromEither(Right(())))
  }

  "submit" should {
    "return 200" in {
      val result = createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request)
      status(result) mustBe ACCEPTED
    }

    "send a request to EIS" in {

      await(createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request))

      verify(submissionMessageService).submit(any, any)(any)

    }

    "sends an audit event" in {
      await(createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request))

      verify(auditService).auditMessage(any)(any)
    }

    "call the add work item routine to create or update the database" in {

      await(createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request))

      verify(workItemService).addWorkItemForErn(consignorId, fastMode = true)

    }

    "return an error when EIS errors" in {
      when(submissionMessageService.submit(any, any)(any))
        .thenReturn(Future.successful(Left(NotFound("not found"))))

      val result = createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request)

      status(result) mustBe NOT_FOUND
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

          val expectedError = Json.toJson(ErrorResponse(
            timestamp,
            "Movement Id format error",
            "The movement ID should be a valid UUID"
          ))

          when(movementValidation.validateMovementId(any)).thenReturn(Left(MovementIdFormatInvalid()))
          when(movementValidation.convertErrorToResponse(eqTo(MovementIdFormatInvalid()), eqTo(timestamp))).thenReturn(BadRequest(expectedError))

          val result = createWithSuccessfulAuth.submit("b405")(request)

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe expectedError
        }
      }

      "return a 404 Not Found" when {

        "movement does not exist in database" in {

          val expectedError = Json.toJson(ErrorResponse(
            timestamp,
            "Movement not found",
            "Movement 49491927-aaa1-4835-b405-dd6e7fa3aaf0 could not be found"
          ))

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

          val expectedError = Json.toJson(ErrorResponse(
            timestamp,
            "Message does not match movement",
            "The Consignor in the message does not match the Consignor in the movement"
          ))

          when(messageValidation.validateSubmittedMessage(any, any, any)).thenReturn(Left(TestMessageDoesNotMatchMovement))
          when(messageValidation.convertErrorToResponse(eqTo(TestMessageDoesNotMatchMovement), eqTo(timestamp))).thenReturn(BadRequest(Json.toJson(expectedError)))

          val result = createWithSuccessfulAuth.submit(movement._id)(request)

          status(result) mustBe BAD_REQUEST

          contentAsJson(result) mustBe expectedError
        }

      }

      "return a 403 Forbidden" when {

        "message validation converts the error into a Forbidden error" in {

          case object TestMessageIdentifierIsUnauthorised extends MessageIdentifierIsUnauthorised(MessageValidation.consignor)

          val expectedError = Json.toJson(ErrorResponse(
            timestamp,
            "Message cannot be sent",
            "The Consignor is not authorised to submit this message for the movement"
          ))

          when(messageValidation.validateSubmittedMessage(any, any, any)).thenReturn(Left(TestMessageIdentifierIsUnauthorised))
          when(messageValidation.convertErrorToResponse(eqTo(TestMessageIdentifierIsUnauthorised), eqTo(timestamp))).thenReturn(Forbidden(Json.toJson(expectedError)))

          val result = createWithSuccessfulAuth.submit(movement._id)(request)

          status(result) mustBe FORBIDDEN

          contentAsJson(result) mustBe expectedError
        }

      }

    }

    "catch Future failure from Work Item service and log it but still process submission" in {

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.submit("49491927-aaa1-4835-b405-dd6e7fa3aaf0")(request)

      status(result) mustBe ACCEPTED

      verify(submissionMessageService).submit(any, any)(any)
    }

  }

  private def createWithSuccessfulAuth =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser(mock[IEMessage]),
      submissionMessageService,
      workItemService,
      movementService,
      auditService,
      messageValidation,
      movementValidation,
      dateTimeService,
      cc
    )

  private def createRequest(body: Elem): FakeRequest[Elem] = {
    FakeRequest("POST", "/foo")
      .withHeaders(FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml")))
      .withBody(body)
  }

  private def createWithAuthActionFailure =
    new SubmitMessageController(
      FakeFailingAuthentication,
      FakeSuccessXMLParser(mock[IEMessage]),
      submissionMessageService,
      workItemService,
      movementService,
      auditService,
      messageValidation,
      movementValidation,
      dateTimeService,
      cc
    )

  private def createWithFailingXmlParserAction =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeFailureXMLParser,
      submissionMessageService,
      workItemService,
      movementService,
      auditService,
      messageValidation,
      movementValidation,
      dateTimeService,
      cc
    )

}
