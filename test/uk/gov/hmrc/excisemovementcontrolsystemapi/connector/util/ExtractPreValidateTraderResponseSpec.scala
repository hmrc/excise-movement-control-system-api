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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connector.util

import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ExtractPreValidateTraderResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.PreValidateTraderErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderErrorResponse, getPreValidateTraderSuccessResponse}
import uk.gov.hmrc.http.HttpResponse

import scala.reflect.runtime.universe.typeOf

class ExtractPreValidateTraderResponseSpec extends PlaySpec with EitherValues {

  private val validResponse = getPreValidateTraderSuccessResponse
  private val businessError = getPreValidateTraderErrorResponse

  "extractResponse" should {

    "return the PreValidateTraderResponse if Success returned by EIS" in {

      val response = ExtractPreValidateTraderResponse.extractResponse(HttpResponse(200, Json.toJson(validResponse).toString()))

      response.map { result =>
        result.exciseTraderValidationResponse.validationTimeStamp mustBe validResponse.exciseTraderValidationResponse.validationTimeStamp
        result.exciseTraderValidationResponse.exciseTraderResponse(0) mustBe validResponse.exciseTraderValidationResponse.exciseTraderResponse(0)
      }

    }

    "return the PreValidateTraderErrorResponse if Business Error returned by EIS" in {

      val response = ExtractPreValidateTraderResponse.extractResponse(HttpResponse(200, Json.toJson(businessError).toString()))

      response.left.value.exciseTraderResponse(0) mustBe businessError.exciseTraderResponse(0)
      response.left.value.validationTimeStamp mustBe businessError.validationTimeStamp

    }

    "throw if cannot parse json into either format" in {
      val ex = intercept[RuntimeException] {
        ExtractPreValidateTraderResponse.extractResponse(HttpResponse(200, """{"test":"test"}""")
        )
      }

      ex.getMessage mustBe s"Response body could not be read as type ${typeOf[PreValidateTraderErrorResponse]}"
    }
  }

}
