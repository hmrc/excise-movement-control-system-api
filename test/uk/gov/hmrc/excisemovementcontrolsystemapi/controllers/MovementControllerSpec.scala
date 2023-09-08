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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.Fixture.FakeAuthentication

class MovementControllerSpec extends AnyWordSpec with FakeAuthentication with Matchers {

  private val fakeRequest = FakeRequest("GET", "/")

  private val controller = new MovementController(FakeSuccessAuthentication, Helpers.stubControllerComponents())

  "GET /" should {
    "return 200" in {
      val result = controller.hello("123")(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "submit" should {
    "return 200" in {
      val result = controller.submit(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
