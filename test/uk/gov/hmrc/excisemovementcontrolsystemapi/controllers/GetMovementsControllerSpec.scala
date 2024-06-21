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
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.{MovementFilter, TraderType}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{ErrorResponseSupport, FakeAuthentication, FakeValidateErnParameterAction, FakeValidateTraderTypeAction, FakeValidateUpdatedSinceAction, MovementTestUtils}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.{MovementIdFormatInvalid, MovementIdValidation}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MessageService, MovementService}
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

  private val controller = new GetMovementsController(
    FakeSuccessAuthentication(Set(ern)),
    FakeValidateErnParameterSuccessAction,
    FakeValidateUpdatedSinceSuccessAction,
    FakeValidateTraderTypeSuccessAction,
    cc,
    movementService,
    dateTimeService,
    messageService,
    movementIdValidator
  )

  private val timestamp   = Instant.parse("2020-01-01T01:01:01.123456Z")
  private val fakeRequest = FakeRequest("POST", "/foo")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, messageService)

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
              Instant.now(),
              Seq.empty
            )
          )
        )
      )

    when(dateTimeService.timestamp()).thenReturn(timestamp)

    when(messageService.updateAllMessages(any)(any)).thenReturn(Future.successful(Done))

  }

  "getMovements" should {
    "return 200 when successful" in {
      val result = controller.getMovements(None, None, None, None, None)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(createMovementResponse(ern, "lrn", "arc", Some("consigneeId"))))
    }

    "get all movement for an ERN" in {
      await(controller.getMovements(None, None, None, None, None)(FakeRequest("GET", "/foo")))

      verify(movementService).getMovementByErn(eqTo(Seq(ern)), any)
    }

    "updates messages for all authorised ERNs if ERN filter not supplied" in {
      val controller = new GetMovementsController(
        FakeSuccessAuthenticationMultiErn(Set(ern, "otherErn")),
        FakeValidateErnParameterSuccessAction,
        FakeValidateUpdatedSinceSuccessAction,
        FakeValidateTraderTypeSuccessAction,
        cc,
        movementService,
        dateTimeService,
        messageService,
        movementIdValidator
      )
      await(controller.getMovements(None, None, None, None, None)(fakeRequest))

      verify(messageService).updateAllMessages(eqTo(Set(ern, "otherErn")))(any)
    }

    "only updates messages filtered ERN if filter supplied" in {
      val controller = new GetMovementsController(
        FakeSuccessAuthenticationMultiErn(Set(ern, "otherErn")),
        FakeValidateErnParameterSuccessAction,
        FakeValidateUpdatedSinceSuccessAction,
        FakeValidateTraderTypeSuccessAction,
        cc,
        movementService,
        dateTimeService,
        messageService,
        movementIdValidator
      )
      await(controller.getMovements(Some("otherErn"), None, None, None, None)(fakeRequest))

      verify(messageService).updateAllMessages(eqTo(Set("otherErn")))(any)
    }

    "return multiple movement" in {
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

      val result = controller.getMovements(None, None, None, None, None)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(
        Seq(
          createMovementResponseFromMovement(movement1),
          createMovementResponseFromMovement(movement2)
        )
      )
    }

    "use a filter" when {
      "traderType is consignor" in {
        val timestampNow = Instant.now()
        await(
          controller.getMovements(Some(ern), Some("lrn"), Some("arc"), Some(timestampNow.toString), Some("consignor"))(
            fakeRequest
          )
        )

        val filter = MovementFilter(
          ern = Some(ern),
          lrn = Some("lrn"),
          arc = Some("arc"),
          updatedSince = Some(timestampNow),
          traderType = Some(TraderType(traderType = "consignor", erns = Seq(ern)))
        )
        verify(movementService).getMovementByErn(any, eqTo(filter))

      }

      "traderType is consignee" in {
        val timestampNow = Instant.now()
        await(
          controller.getMovements(Some(ern), Some("lrn"), Some("arc"), Some(timestampNow.toString), Some("consignee"))(
            fakeRequest
          )
        )

        val filter = MovementFilter(
          ern = Some(ern),
          lrn = Some("lrn"),
          arc = Some("arc"),
          updatedSince = Some(timestampNow),
          traderType = Some(TraderType(traderType = "consignee", erns = Seq(ern)))
        )
        verify(movementService).getMovementByErn(any, eqTo(filter))

      }
    }

    "return a bad request" when {
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
      "wrong traderType is passed in" in {
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

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.getMovements(None, None, None, None, None)(fakeRequest)

        status(result) mustBe FORBIDDEN
      }
    }
  }

  "Get movement controller" should {

    val uuid     = "cfdb20c7-d0b0-4b8b-a071-737d68dede5b"
    val movement =
      Movement(uuid, Some("id123"), "lrn1", "testErn", Some("consignee"), Some("arc"), Instant.now(), Seq.empty)

    "return the movement when successful" in {

      when(movementIdValidator.validateMovementId(eqTo(uuid))).thenReturn(Right(uuid))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      val result = controller.getMovement(uuid)(fakeRequest)

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(createMovementResponseFromMovement(movement))

    }

    "updates messages for all authorised ERNs if ERN filter not supplied" in {
      val controller = new GetMovementsController(
        FakeSuccessAuthenticationMultiErn(Set(ern, "otherErn")),
        FakeValidateErnParameterSuccessAction,
        FakeValidateUpdatedSinceSuccessAction,
        FakeValidateTraderTypeSuccessAction,
        cc,
        movementService,
        dateTimeService,
        messageService,
        movementIdValidator
      )

      when(movementIdValidator.validateMovementId(eqTo(uuid))).thenReturn(Right(uuid))
      when(movementService.getMovementById(any)).thenReturn(Future.successful(Some(movement)))

      await(controller.getMovement(uuid)(fakeRequest))

      verify(messageService).updateAllMessages(eqTo(Set(ern, "otherErn")))(any)
    }

    "return Not Found error" when {
      "movement not found in database" in {
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

      "movement in database is for different ERNs" in {

        when(movementIdValidator.validateMovementId(eqTo(uuid))).thenReturn(Right(uuid))
        when(movementService.getMovementById(any))
          .thenReturn(Future.successful(Some(movement.copy(consignorId = "ern8921"))))

        val result = controller.getMovement(uuid)(fakeRequest)

        status(result) mustBe NOT_FOUND

        contentAsJson(result) mustBe expectedJsonErrorResponse(
          "2020-01-01T01:01:01.123Z",
          "Movement not found",
          s"Movement $uuid is not found within the data for ERNs testErn"
        )
      }
    }

    "return Bad Request error" when {
      "supplied movement Id is not in correct format" in {

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

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.getMovement(uuid)(fakeRequest)

        status(result) mustBe FORBIDDEN
      }
    }

  }

  private def createControllerWithErnParameterError =
    new GetMovementsController(
      FakeSuccessAuthentication(Set(ern)),
      FakeValidateErnParameterFailureAction,
      FakeValidateUpdatedSinceSuccessAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator
    )

  private def createWithAuthActionFailure =
    new GetMovementsController(
      FakeFailingAuthentication,
      FakeValidateErnParameterSuccessAction,
      FakeValidateUpdatedSinceSuccessAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator
    )

  private val createWithUpdateSinceActionFailure =
    new GetMovementsController(
      FakeSuccessAuthentication(Set(ern)),
      FakeValidateErnParameterSuccessAction,
      FakeValidateUpdatedSinceFailureAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator
    )

  private val createWithTraderTypeActionFailure =
    new GetMovementsController(
      FakeSuccessAuthentication(Set(ern)),
      FakeValidateErnParameterSuccessAction,
      FakeValidateUpdatedSinceFailureAction,
      FakeValidateTraderTypeSuccessAction,
      cc,
      movementService,
      dateTimeService,
      messageService,
      movementIdValidator
    )

}
