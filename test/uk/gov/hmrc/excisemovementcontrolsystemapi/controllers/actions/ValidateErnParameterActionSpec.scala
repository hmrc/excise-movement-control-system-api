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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class ValidateErnParameterActionSpec extends PlaySpec {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val emcsUtils: EmcsUtils = mock[EmcsUtils]
  private val currentDateTime = LocalDateTime.of(2023, 10, 18, 15, 33, 33)
  private val sut = new ValidateErnParameterActionImpl(emcsUtils, stubMessagesControllerComponents())

  when(emcsUtils.getCurrentDateTime).thenReturn(currentDateTime)

  private def defaultBlock(enrolmentRequest: EnrolmentRequest[_]) =
    Future.successful(Ok)

  "ValidateErnParameterActionSpec" should {
    "filter passes successfully" when {

      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val request = EnrolmentRequest(FakeRequest(), erns, "123")

      val checkResponseMatchesRequestBlock = (actual: EnrolmentRequest[_]) => {
        actual mustBe request
        Future.successful(Ok)
      }

      "the erns match" in {

        val result = await(sut.apply(Some("GBWK002181023")).invokeBlock(request, checkResponseMatchesRequestBlock))

        result mustBe Ok
      }

      "no ern supplied in query parameters" in {

        val result = await(sut.apply(None).invokeBlock(request, checkResponseMatchesRequestBlock))

        result mustBe Ok
      }

    }

    "an error" when {
      "ERN in parameter is not supplied in the auth" in {

        val request = EnrolmentRequest(FakeRequest(), Set("12356", "234567"), "123")

        val result = await(sut.apply(Some("GBWK002181023")).invokeBlock(request, defaultBlock))

        result mustBe BadRequest(Json.toJson(ErrorResponse(
          currentDateTime,
          "ERN parameter value error",
          "The ERN GBWK002181023 supplied in the parameter is not among the authorised ERNs 12356/234567"))
        )
      }

    }
  }
}
