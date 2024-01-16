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

import dispatch.Future
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateMovementIdAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, GetMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.ExecutionContext

class GetMovementControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeValidateMovementIdAction
    with BeforeAndAfterEach {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val fakeRequest = FakeRequest("POST", "/foo")
  private val movementService = mock[MovementService]
  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")
  private val uuid = "cfdb20c7-d0b0-4b8b-a071-737d68dede5b"

  private val movement =  Movement("id123", "lrn1", "testErn", Some("consignee"), Some("arc"), Instant.now(), Seq.empty)

  private val controller = new GetMovementController(
    FakeSuccessAuthentication,
    movementService,
    dateTimeService,
    cc
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(movementService)

    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }


  "Get movement controller" should {

    "return the movement when successful" in {

      when(movementService.getMovementById(eqTo(uuid))).thenReturn(Future.successful(Some(
        movement
      )))

      val result = controller.getMovement(uuid)(fakeRequest)

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(
        GetMovementResponse(
          movement._id,
          movement.consignorId,
          movement.localReferenceNumber,
          movement.consigneeId,
          movement.administrativeReferenceCode,
          "Accepted"
        )
      )

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

  }

  private def createWithAuthActionFailure = new GetMovementController(
    FakeFailingAuthentication,
    movementService,
    dateTimeService,
    cc
  )


}
