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

import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication

import scala.concurrent.ExecutionContext

class GetMovementsControllerSpec extends PlaySpec with FakeAuthentication {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()

  "getMovements" should {
    "return 200 when successful" in {
      val controller = new GetMovementsController( FakeSuccessAuthentication, cc)

      val result = controller.getMovements(FakeRequest("POST", "/foo"))

      status(result) mustBe OK

      val expectedJson = Json.parse(
        """[
          |  {
          |    "consignorId": "MRlY1BIyYA1nd",
          |    "localReferenceNumber": "1v$%wqd",
          |    "consigneeId": "dUC\"v",
          |    "administrativeReferenceCode": "73CUHCY7XA4BIDE35CBR6",
          |    "status": "Accepted"
          |  }
          |]""".stripMargin
      )

      contentAsJson(result) mustBe expectedJson
    }
  }

}
