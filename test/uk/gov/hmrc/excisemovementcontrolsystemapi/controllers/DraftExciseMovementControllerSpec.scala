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

import dispatch.Future
import generated.IE815Type
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{ParseIE815XmlAction, ParseIE815XmlActionImpl}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.AuthorizedIE815Request
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.AuthorizedRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.XmlParser



class DraftExciseMovementControllerSpec extends AnyWordSpec with FakeAuthentication with FakeXmlParsers with Matchers {

  private val fakeRequest = FakeRequest("GET", "/")

  private val successController = new DraftExciseMovementController(FakeSuccessAuthentication, FakeSuccessIE815XMLParser, Helpers.stubControllerComponents())


    "submit" should {
      "return 200" in {
        val result = successController.submit(fakeRequest)
      status(result) shouldBe Status.OK
    }

      "return 400" when {
        val xmlFailureController = new DraftExciseMovementController(FakeSuccessAuthentication, FakeErrorIE815XMLParser, Helpers.stubControllerComponents())
        "xml cannot be parsed" in {
          val result = xmlFailureController.submit(fakeRequest)

          status(result) shouldBe Status.BAD_REQUEST
        }
      }

      "return error" when {
        val authActionForbiddenController = new DraftExciseMovementController(FakeForbiddenAuthentication, FakeSuccessIE815XMLParser, Helpers.stubControllerComponents())
        "authentication fails" in {
          val result = authActionForbiddenController.submit(fakeRequest)

          status(result) shouldBe Status.FORBIDDEN
        }
      }

  }
}
