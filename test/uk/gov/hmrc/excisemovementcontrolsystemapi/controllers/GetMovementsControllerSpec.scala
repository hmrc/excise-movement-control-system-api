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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{ACCEPTED, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.GetMovementConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISConsumptionResponse, EISErrorResponse}

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val getMovementConnector = mock[GetMovementConnector]
  private val controller = new GetMovementsController(FakeSuccessAuthentication, cc, getMovementConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(getMovementConnector)

    when(getMovementConnector.get(any, any)(any)).thenReturn(Future.successful(
      Right(EISConsumptionResponse(
        LocalDateTime.of(2023, 10, 26, 3, 2, 2),
        ern,
        "message")))
    )
  }

  "getMovements" should {
    "return 200 when successful" in {
      val result = controller.getMovements(FakeRequest("POST", "/foo"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(GetMovementResponse(
        ern,
        "lrn",
        "consigneeId",
        "arc",
        ACCEPTED
      )))
    }

    "request movement from EIS" in {
      await(controller.getMovements(FakeRequest("GET", "/foo")))

      verify(getMovementConnector).get(eqTo(ern), eqTo("arc"))(any)
    }
  }

}
