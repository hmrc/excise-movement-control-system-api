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
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, MovementTestUtils}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with MovementTestUtils
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val movementService = mock[MovementService]
  private val workItemService = mock[WorkItemService]
  private val controller = new GetMovementsController(FakeSuccessAuthentication, cc, movementService, workItemService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, workItemService)

    when(movementService.getMovementByErn(any, any))
      .thenReturn(Future.successful(Seq(Movement("lrn", ern, Some("consigneeId"), Some("arc")))))

    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))

  }

  "getMovements" should {
    "return 200 when successful" in {
      val result = controller.getMovements(None, None, None, None)(FakeRequest("POST", "/foo"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(createMovementResponse(ern, "lrn", "arc", Some("consigneeId"))))
    }

    "get all movement for an ERN" in {
      await(controller.getMovements(None, None, None, None)(FakeRequest("GET", "/foo")))

      verify(movementService).getMovementByErn(eqTo(Seq(ern)), any)
    }

    "return multiple movement" in {
      val movement1 = Movement("lrn", ern, Some("consigneeId"), Some("arc"))
      val movement2 = Movement("lrn2", ern, Some("consigneeId2"), Some("arc2"))
      when(movementService.getMovementByErn(any, any))
        .thenReturn(Future.successful(Seq(movement1, movement2)))

      val result = controller.getMovements(None, None, None, None)(FakeRequest("POST", "/foo"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(
        createMovementResponse(ern, "lrn", "arc", Some("consigneeId")),
        createMovementResponse(ern, "lrn2", "arc2", Some("consigneeId2"))
      ))
    }

    "use a filter" in {
      val timestampNow = LocalDateTime.now().toInstant(ZoneOffset.UTC)
        .toString
      await(controller.getMovements(Some(ern), Some("lrn"), Some("arc"), Some(timestampNow))(FakeRequest("POST", "/foo")))

      val filter = MovementFilter.and(Seq(
        "ern" -> Some(ern), "lrn" -> Some("lrn"), "arc" -> Some("arc"), "lastUpdated" -> Some(timestampNow))
      )
      verify(movementService).getMovementByErn(any, eqTo(filter))

    }

    "create a Work Item if there is not one for the ERN already" in {

      await(controller.getMovements(None, None, None, None)(FakeRequest("POST", "/foo")))

      verify(workItemService).addWorkItemForErn(eqTo("testErn"), eqTo(false))

    }

    "catch Future failure from Work Item service and log it but still process submission" in {

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = controller.getMovements(None, None, None, None)(FakeRequest("POST", "/foo"))

      status(result) mustBe OK

      verify(movementService).getMovementByErn(any, any)
    }
  }
}
