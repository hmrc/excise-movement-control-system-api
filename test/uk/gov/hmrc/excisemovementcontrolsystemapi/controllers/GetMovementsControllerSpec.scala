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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mongodb.scala.MongoException
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilterBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateErnParameterAction, MovementTestUtils}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeValidateErnParameterAction
    with MovementTestUtils
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val movementService = mock[MovementService]
  private val workItemService = mock[WorkItemService]
  private val dateTimeService = mock[DateTimeService]
  private val controller = new GetMovementsController(
    FakeSuccessAuthentication,
    FakeValidateErnParameterSuccessAction,
    cc,
    movementService,
    workItemService,
    dateTimeService
  )
  private val timestamp = Instant.parse("2020-01-01T01:01:01.1Z")
  private val fakeRequest = FakeRequest("POST", "/foo")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, workItemService)

    when(movementService.getMovementByErn(any, any))
      .thenReturn(Future.successful(Seq(Movement("cfdb20c7-d0b0-4b8b-a071-737d68dede5e", "boxId", "lrn", ern, Some("consigneeId"), Some("arc"), Instant.now(), Seq.empty))))

    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))


    when(dateTimeService.timestamp()).thenReturn(timestamp)

  }

  "getMovements" should {
    "return 200 when successful" in {
      val result = controller.getMovements(None, None, None, None)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(createMovementResponse(ern, "lrn", "arc", Some("consigneeId"))))
    }

    "get all movement for an ERN" in {
      await(controller.getMovements(None, None, None, None)(FakeRequest("GET", "/foo")))

      verify(movementService).getMovementByErn(eqTo(Seq(ern)), any)
    }

    "return multiple movement" in {
      val movement1 = Movement(
        "cfdb20c7-d0b0-4b8b-a071-737d68dede5a",
        "lrn",
        ern,
        Some("consigneeId"),
        Some("arc"),
        Instant.now(),
        Seq.empty
      )
      val movement2 = Movement(
        "cfdb20c7-d0b0-4b8b-a071-737d68dede5b",
        "lrn2",
        ern,
        Some("consigneeId2"),
        Some("arc2"),
        Instant.now(),
        Seq.empty
      )
      when(movementService.getMovementByErn(any, any))
        .thenReturn(Future.successful(Seq(movement1, movement2)))

      val result = controller.getMovements(None, None, None, None)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1),
        createMovementResponseFromMovement(movement2)
      ))
    }

    "use a filter" in {
      val timestampNow = Instant.now()
      await(controller.getMovements(Some(ern), Some("lrn"), Some("arc"), Some(timestampNow.toString))(fakeRequest))

      val filter = MovementFilterBuilder()
        .withErn(Some(ern))
        .withLrn(Some("lrn"))
        .withArc(Some("arc"))
        .withUpdatedSince(Some(timestampNow)).build()
      verify(movementService).getMovementByErn(any, eqTo(filter))

    }

    "return a bad request" when {
      "the updatedSince time is provided in an invalid format" in {

        val result = controller.getMovements(Some(ern), Some("lrn"), Some("arc"), Some("invalid date"))(fakeRequest)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.toJson(
          ErrorResponse(timestamp, "Invalid date format provided in the updatedSince query parameter", "Date format should be like '2020-11-15T17:02:34.00Z'")
        )

      }

      "filtering by ERN and ERN filter is not in the authorised list" in {
        val result = createControllerWithErnParameterError.getMovements(Some("ERNValue"), None, None, None)(fakeRequest)

        status(result) mustBe BAD_REQUEST
      }
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.getMovements(None, None, None, None)(fakeRequest)

        status(result) mustBe FORBIDDEN
      }
    }


    "create a Work Item if there is not one for the ERN already" in {

      await(controller.getMovements(None, None, None, None)(fakeRequest))

      verify(workItemService).addWorkItemForErn(eqTo("testErn"), eqTo(false))

    }

    "catch Future failure from Work Item service and log it but still process submission" in {

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = controller.getMovements(None, None, None, None)(fakeRequest)

      status(result) mustBe OK

      verify(movementService).getMovementByErn(any, any)
    }

  }

  "Get movement controller" should {

    val uuid = "cfdb20c7-d0b0-4b8b-a071-737d68dede5b"
    val movement = Movement(uuid, "id123", "lrn1", "testErn", Some("consignee"), Some("arc"), Instant.now(), Seq.empty)

    "return the movement when successful" in {

      when(movementService.getMovementById(eqTo(uuid))).thenReturn(Future.successful(Some(movement)))

      val result = controller.getMovement(uuid)(fakeRequest)

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(createMovementResponseFromMovement(movement))

    }

    "return Not Found error" when {
      "movement not found in database" in {

        when(movementService.getMovementById(any)).thenReturn(Future.successful(None))
        val result = controller.getMovement(uuid)(fakeRequest)

        status(result) mustBe NOT_FOUND

        contentAsJson(result) mustBe Json.toJson(
          ErrorResponse(timestamp, "Movement not found", s"Movement $uuid is not found")
        )

      }

      "movement in database is for different ERNs" in {

        when(movementService.getMovementById(uuid)).thenReturn(Future.successful(Some(
          movement.copy(consignorId = "ern8921")
        )))

        val result = controller.getMovement(uuid)(fakeRequest)

        status(result) mustBe NOT_FOUND

        contentAsJson(result) mustBe Json.toJson(
          ErrorResponse(timestamp, "Movement not found", s"Movement $uuid is not found within the data for ERNs testErn")
        )
      }
    }

    "return Bad Request error" when {
      "supplied movement Id is not in correct format" in {

        val result = controller.getMovement("abcd43-r")(fakeRequest)

        status(result) mustBe BAD_REQUEST

        contentAsJson(result) mustBe Json.toJson(
          ErrorResponse(
            dateTimeService.timestamp(),
            "Movement Id format error",
            "Movement Id should be a valid UUID"
          )
        )

      }
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.getMovement(uuid)(fakeRequest)

        status(result) mustBe FORBIDDEN
      }
    }

    "create a Work Item if there is not one for the ERN already" in {

      when(movementService.getMovementById(eqTo(uuid))).thenReturn(Future.successful(Some(movement)))

      await(controller.getMovement(uuid)(fakeRequest))

      verify(workItemService).addWorkItemForErn(eqTo("testErn"), eqTo(false))

    }

    "catch Future failure from Work Item service and log it but still process submission" in {

      when(movementService.getMovementById(eqTo(uuid))).thenReturn(Future.successful(Some(movement)))

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = controller.getMovement(uuid)(fakeRequest)

      status(result) mustBe OK

    }

  }


  private def createControllerWithErnParameterError =
    new GetMovementsController(
      FakeSuccessAuthentication,
      FakeValidateErnParameterFailureAction,
      cc,
      movementService,
      workItemService,
      dateTimeService
    )

  private def createWithAuthActionFailure =
    new GetMovementsController(
      FakeFailingAuthentication,
      FakeValidateErnParameterSuccessAction,
      cc,
      movementService,
      workItemService,
      dateTimeService
    )
}
