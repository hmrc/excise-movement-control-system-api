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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.CorrelationIdAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.{MovementFilter, TraderType}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.{MovementIdFormatInvalid, MovementIdValidation}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, MessageService, MovementService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsControllerSpec
    extends PlaySpec
    with FakeAuthentication
    with FakeValidateErnParameterAction
    with FakeValidateTraderTypeAction
    with FakeValidateUpdatedSinceAction
    with MovementTestUtils
    with ErrorResponseSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc                    = stubControllerComponents()
  private val movementService       = mock[MovementService]
  private val dateTimeService       = mock[DateTimeService]
  private val messageService        = mock[MessageService]
  private val movementIdValidator   = mock[MovementIdValidation]
  private val auditService          = mock[AuditService]

  private val controller = new GetMovementsController(
    FakeSuccessAuthentication(Set(ern)),
    new CorrelationIdAction,
    FakeValidateErnParameterSuccessAction,
    FakeValidateUpdatedSinceSuccessAction,
    FakeValidateTraderTypeSuccessAction,
    cc,
    movementService,
    dateTimeService,
    messageService,
    movementIdValidator,
    auditService
  )

  private val timestamp   = Instant.parse("2020-01-01T01:01:01.123456Z")
  private val fakeRequest = FakeRequest("GET", "/foo")

  private def createControllerWithErnParameterError =
    new GetMovementsController(
      FakeSuccessAuthentication(Set(ern)),
      new CorrelationIdAction,
      FakeValidateErnParameterFailureAction,
      FakeValidateUpdatedSinceSuccessAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator,
      auditService
    )

  private def createWithAuthActionFailure =
    new GetMovementsController(
      FakeFailingAuthentication,
      new CorrelationIdAction,
      FakeValidateErnParameterSuccessAction,
      FakeValidateUpdatedSinceSuccessAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator,
      auditService
    )

  private val createWithUpdateSinceActionFailure =
    new GetMovementsController(
      FakeSuccessAuthentication(Set(ern)),
      new CorrelationIdAction,
      FakeValidateErnParameterSuccessAction,
      FakeValidateUpdatedSinceFailureAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator,
      auditService
    )

  private val createWithTraderTypeActionFailure =
    new GetMovementsController(
      FakeSuccessAuthentication(Set(ern)),
      new CorrelationIdAction,
      FakeValidateErnParameterSuccessAction,
      FakeValidateUpdatedSinceFailureAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator,
      auditService
    )

  val enrolmentRequest: EnrolmentRequest[AnyContent] =
    EnrolmentRequest(FakeRequest(), Set(ern), fakeUserDetails)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, messageService, auditService)

    when(movementService.getMovementByErn(any, any))
      .thenReturn(
        Future.successful(
          Seq(
            Movement(
              "cfdb20c7-d0b0-4b8b-a071-737d68dede5e",
              Some("boxId"),
              "lrn",
              ern,
              Some("consigneeId"),
              Some("arc"),
              timestamp,
              Seq.empty
            )
          )
        )
      )

    when(dateTimeService.timestamp()).thenReturn(timestamp)

    when(messageService.updateAllMessages(any)(any)).thenReturn(Future.successful(Done))

  }

  "getMovements" should {
    "respond with 200 OK" when {
      "called with no query parameters and valid request" in {
        val expectedResponse = createMovementResponse(ern, "lrn", "arc", Some("consigneeId"), Some(timestamp))
        val result           = controller.getMovements(None, None, None, None, None)(enrolmentRequest)
        val filter           = MovementFilter(None, None, None, None, None)
        val movements        = Seq(
          Movement(
            expectedResponse.movementId,
            Some("boxId"),
            "lrn",
            ern,
            Some("consigneeId"),
            Some("arc"),
            timestamp,
            Seq.empty[Message]
          )
        )

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(
          Seq(expectedResponse)
        )
        withClue("Submits GetInformation (GetMovements) audit event") {
          verify(auditService, times(1))
            .getInformationForGetMovements(eqTo(filter), eqTo(movements), any[EnrolmentRequest[AnyContent]])(
              any
            )
        }
      }
      "there are no query parameters" when {
        "expecting multiple movements from one ERN" in {
          val movement1 = Movement(
            "cfdb20c7-d0b0-4b8b-a071-737d68dede5a",
            Some("boxId"),
            "lrn",
            ern,
            Some("consigneeId"),
            Some("arc"),
            Instant.now(),
            Seq.empty
          )
          val movement2 = Movement(
            "cfdb20c7-d0b0-4b8b-a071-737d68dede5b",
            Some("boxId"),
            "lrn2",
            ern,
            Some("consigneeId2"),
            Some("arc2"),
            Instant.now(),
            Seq.empty
          )
          when(movementService.getMovementByErn(any, any))
            .thenReturn(Future.successful(Seq(movement1, movement2)))

          val result = controller.getMovements(None, None, None, None, None)(enrolmentRequest)

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(
            Seq(
              createMovementResponseFromMovement(movement1),
              createMovementResponseFromMovement(movement2)
            )
          )

          verify(movementService).getMovementByErn(eqTo(Seq(ern)), any)
          withClue("Submits GetInformation (GetMovements) audit event") {
            verify(auditService, times(1))
              .getInformationForGetMovements(
                eqTo(MovementFilter(None, None, None, None, None)),
                eqTo(Seq(movement1, movement2)),
                any[EnrolmentRequest[AnyContent]]
              )(any)
          }
        }
        "expecting movements from multiple ERNs" in {

          val controller = new GetMovementsController(
            FakeSuccessAuthenticationMultiErn(Set(ern, "ern2")),
            new CorrelationIdAction,
            FakeValidateErnParameterSuccessAction,
            FakeValidateUpdatedSinceSuccessAction,
            FakeValidateTraderTypeSuccessAction,
            cc,
            movementService,
            dateTimeService,
            messageService,
            movementIdValidator,
            auditService
          )

          val movement1 = Movement(
            "cfdb20c7-d0b0-4b8b-a071-737d68dede5a",
            Some("boxId"),
            "lrn",
            ern,
            Some("consigneeId"),
            Some("arc"),
            Instant.now(),
            Seq.empty
          )
          val movement2 = Movement(
            "cfdb20c7-d0b0-4b8b-a071-737d68dede5b",
            Some("boxId"),
            "lrn2",
            "ern2",
            Some("consigneeId2"),
            Some("arc2"),
            Instant.now(),
            Seq.empty
          )
          when(movementService.getMovementByErn(any, any))
            .thenReturn(Future.successful(Seq(movement1, movement2)))

          val result = controller.getMovements(None, None, None, None, None)(enrolmentRequest)

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(
            Seq(
              createMovementResponseFromMovement(movement1),
              createMovementResponseFromMovement(movement2)
            )
          )

          verify(messageService).updateAllMessages(eqTo(Set(ern, "ern2")))(any)

          withClue("Submits GetInformation (GetMovements) audit event") {
            verify(auditService, times(1))
              .getInformationForGetMovements(
                eqTo(MovementFilter(None, None, None, None, None)),
                eqTo(Seq(movement1, movement2)),
                any[EnrolmentRequest[AnyContent]]
              )(
                any
              )
          }

        }
      }
      "there are query parameters" when {
        "an ERN is specified in the query parameters" in {
          val localErn = "ern2"

          val controller = new GetMovementsController(
            FakeSuccessAuthenticationMultiErn(Set(ern, localErn)),
            new CorrelationIdAction,
            FakeValidateErnParameterSuccessAction,
            FakeValidateUpdatedSinceSuccessAction,
            FakeValidateTraderTypeSuccessAction,
            cc,
            movementService,
            dateTimeService,
            messageService,
            movementIdValidator,
            auditService
          )
          val movement2  = Movement(
            "cfdb20c7-d0b0-4b8b-a071-737d68dede5b",
            Some("boxId"),
            "lrn2",
            localErn,
            Some("consigneeId2"),
            Some("arc2"),
            Instant.now(),
            Seq.empty
          )

          when(movementService.getMovementByErn(any, any))
            .thenReturn(Future.successful(Seq(movement2)))

          val result = controller.getMovements(Some(localErn), None, None, None, None)(enrolmentRequest)

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(
            Seq(createMovementResponseFromMovement(movement2))
          )

          verify(messageService).updateAllMessages(eqTo(Set(localErn)))(any)

          withClue("Submits GetInformation (GetMovements) audit event") {
            verify(auditService, times(1))
              .getInformationForGetMovements(
                eqTo(MovementFilter(Some(localErn), None, None, None, None)),
                eqTo(Seq(movement2)),
                any[EnrolmentRequest[AnyContent]]
              )(
                any
              )
          }

        }
        "consignor is specified and multiple filter parameters are given" in {
          val movements = Seq(
            Movement(
              "cfdb20c7-d0b0-4b8b-a071-737d68dede5e",
              Some("boxId"),
              "lrn",
              ern,
              Some("consigneeId"),
              Some("arc"),
              timestamp,
              Seq.empty[Message]
            )
          )

          await(
            controller
              .getMovements(Some(ern), Some("lrn"), Some("arc"), Some(timestamp.toString), Some("consignor"))(
                enrolmentRequest
              )
          )

          val filter = MovementFilter(
            ern = Some(ern),
            lrn = Some("lrn"),
            arc = Some("arc"),
            updatedSince = Some(timestamp),
            traderType = Some(TraderType(traderType = "consignor", erns = Seq(ern)))
          )

          verify(movementService).getMovementByErn(any, eqTo(filter))

          withClue("Submits GetInformation (GetMovements) audit event") {
            verify(auditService, times(1))
              .getInformationForGetMovements(
                eqTo(
                  MovementFilter(
                    Some(ern),
                    Some("lrn"),
                    Some("arc"),
                    Some(timestamp),
                    Some(TraderType("consignor", Seq(ern)))
                  )
                ),
                eqTo(movements),
                any
              )(any)
          }
        }
        "consignee is specified and multiple filter parameters are given" in {
          val movements = Seq(
            Movement(
              "cfdb20c7-d0b0-4b8b-a071-737d68dede5e",
              Some("boxId"),
              "lrn",
              ern,
              Some("consigneeId"),
              Some("arc"),
              timestamp,
              Seq.empty[Message]
            )
          )
          await(
            controller
              .getMovements(Some(ern), Some("lrn"), Some("arc"), Some(timestamp.toString), Some("consignee"))(
                enrolmentRequest
              )
          )

          val filter = MovementFilter(
            ern = Some(ern),
            lrn = Some("lrn"),
            arc = Some("arc"),
            updatedSince = Some(timestamp),
            traderType = Some(TraderType(traderType = "consignee", erns = Seq(ern)))
          )
          verify(movementService).getMovementByErn(any, eqTo(filter))

          withClue("Submits GetInformation (GetMovements) audit event") {
            verify(auditService, times(1))
              .getInformationForGetMovements(
                eqTo(
                  MovementFilter(
                    Some(ern),
                    Some("lrn"),
                    Some("arc"),
                    Some(timestamp),
                    Some(TraderType("consignee", Seq(ern)))
                  )
                ),
                eqTo(movements),
                any
              )(
                any
              )
          }
        }
      }
    }
    "respond with 400 BAD_REQUEST" when {
      "the updatedSince time is provided in an invalid format" in {

        val result = createWithUpdateSinceActionFailure.getMovements(
          Some(ern),
          Some("lrn"),
          Some("arc"),
          Some(Instant.now().toString),
          None
        )(fakeRequest)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "Invalid date format provided in the updatedSince query parameter",
          "Date format should be like '2020-11-15T17:02:34.00Z'"
        )
      }
      "the wrong traderType is passed in" in {
        val result = createWithTraderTypeActionFailure.getMovements(
          Some(ern),
          Some("lrn"),
          Some("arc"),
          None,
          Some("wrongTraderType")
        )(fakeRequest)
        status(result) mustBe BAD_REQUEST
      }
      "filtering by ERN and ERN filter is not in the authorised list" in {
        val result =
          createControllerWithErnParameterError.getMovements(Some("ERNValue"), None, None, None, None)(fakeRequest)

        status(result) mustBe BAD_REQUEST
      }
    }
    "respond with 403 FORBIDDEN" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.getMovements(None, None, None, None, None)(fakeRequest)

        status(result) mustBe FORBIDDEN
      }
    }
    "respond with 500 INTERNAL_SERVER_ERROR" when {
      "anything in the updateMovements call fails" in {

        when(messageService.updateAllMessages(any)(any)).thenReturn(Future.failed(new RuntimeException()))

        val result = controller.getMovements(None, None, None, None, None)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.toJson(
          ErrorResponse(dateTimeService.timestamp(), "Error getting movements", "Unknown error while getting movements")
        )
      }
    }
  }

  "getMovement" should {
    val uuid     = "cfdb20c7-d0b0-4b8b-a071-737d68dede5b"
    val testErn  = "testErn"
    val movement =
      Movement(uuid, Some("id123"), "lrn1", testErn, Some("consignee"), Some("arc"), Instant.now(), Seq.empty)

    "respond with 200 OK" when {
      "request is valid and the authorised ern is in the message recipients" in {

        val movement =
          Movement(
            uuid,
            Some("id123"),
            "lrn1",
            "consignor",
            Some("consignee"),
            Some("arc"),
            Instant.now(),
            Seq(Message("message", "IE801", "messageId", ern, Set.empty, timestamp))
          )

        when(movementIdValidator.validateMovementId(eqTo(uuid))).thenReturn(Right(uuid))
        when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

        val result = controller.getMovement(uuid)(fakeRequest)

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.toJson(createMovementResponseFromMovement(movement))
        withClue("Submits GetInformation (SpecificMovement) audit event") {
          verify(auditService, times(1))
            .getInformationForGetSpecificMovement(eqTo(uuid), any[EnrolmentRequest[AnyContent]])(any)
        }
      }
      "request is valid and there are multiple authorised ERNS" in {
        val controller = new GetMovementsController(
          FakeSuccessAuthenticationMultiErn(Set(ern, "otherErn")),
          new CorrelationIdAction,
          FakeValidateErnParameterSuccessAction,
          FakeValidateUpdatedSinceSuccessAction,
          FakeValidateTraderTypeSuccessAction,
          cc,
          movementService,
          dateTimeService,
          messageService,
          movementIdValidator,
          auditService
        )

        when(movementIdValidator.validateMovementId(eqTo(uuid))).thenReturn(Right(uuid))
        when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

        val result = controller.getMovement(uuid)(fakeRequest)

        status(result) mustBe OK

        verify(messageService).updateAllMessages(eqTo(Set(ern, "otherErn")))(any)
      }
    }
    "respond with 404 NOT_FOUND" when {
      "the specified movement is not found in the database" in {
        when(movementIdValidator.validateMovementId(eqTo(uuid))).thenReturn(Right(uuid))
        when(movementService.getMovementById(any)).thenReturn(Future.successful(None))

        val result = controller.getMovement(uuid)(fakeRequest)

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "Movement not found",
          s"Movement $uuid could not be found"
        )
      }
      "movement in database is not available for the authorised ERNs" in {

        when(movementIdValidator.validateMovementId(eqTo(uuid))).thenReturn(Right(uuid))
        when(movementService.getMovementById(any))
          .thenReturn(Future.successful(Some(movement.copy(consignorId = "ern8921"))))

        val result = controller.getMovement(uuid)(fakeRequest)

        status(result) mustBe NOT_FOUND

        contentAsJson(result) mustBe expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "Movement not found",
          s"Movement $uuid is not found within the data for ERNs $testErn"
        )
      }
    }
    "respond with 400 BAD_REQUEST" when {
      "supplied movement Id is not in the correct format" in {

        val expectedError = expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "Movement Id format error",
          s"Movement Id should be a valid UUID"
        )

        when(movementIdValidator.validateMovementId(any))
          .thenReturn(Left(MovementIdFormatInvalid()))
        when(movementIdValidator.convertErrorToResponse(eqTo(MovementIdFormatInvalid()), eqTo(timestamp))).thenReturn(
          BadRequest(expectedError)
        )

        val result = controller.getMovement("abcd43-r")(fakeRequest)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe expectedError

      }
    }
    "respond with 403 FORBIDDEN" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.getMovement(uuid)(fakeRequest)

        status(result) mustBe FORBIDDEN
      }
    }
  }
}
