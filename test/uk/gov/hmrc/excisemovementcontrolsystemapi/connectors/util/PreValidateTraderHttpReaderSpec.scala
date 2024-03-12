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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util


import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderErrorEISResponse, getPreValidateTraderSuccessEISResponse}
import uk.gov.hmrc.http.HttpResponse

class PreValidateTraderHttpReaderSpec extends PlaySpec with EitherValues {

  private val validResponse = getPreValidateTraderSuccessEISResponse
  private val businessError = getPreValidateTraderErrorEISResponse

  private val preValidateTraderHttpReader = PreValidateTraderHttpReader("123", "GB123", "date time")
  "read" should {
    "return PreValidateTraderResponse when success" in {

      val result = preValidateTraderHttpReader.read(
        "ANY",
        "/foo",
        HttpResponse(200, Json.toJson(validResponse).toString())
      )

      val responseObject = result.toOption.value.exciseTraderValidationResponse.value
      responseObject.validationTimeStamp mustBe validResponse.exciseTraderValidationResponse.value.validationTimeStamp
      responseObject.exciseTraderResponse(0) mustBe validResponse.exciseTraderValidationResponse.value.exciseTraderResponse(0)

    }

    "return PreValidateTraderErrorResponse when error occurs" in {

      val result = preValidateTraderHttpReader.read(
        "ANY",
        "/foo",
        HttpResponse(200, Json.toJson(businessError).toString())
      )

      val responseObject = result.toOption.value

      responseObject.validationTimeStamp mustBe businessError.validationTimeStamp
      responseObject.exciseTraderResponse.value(0) mustBe businessError.exciseTraderResponse.value(0)
    }

    forAll(Seq(
      (BAD_REQUEST, BadRequest("")),
      (NOT_FOUND, NotFound("")),
      (INTERNAL_SERVER_ERROR, InternalServerError("")),
      (SERVICE_UNAVAILABLE, ServiceUnavailable("")))) { case (statusCode, expectedResult) =>
      s"return $statusCode" when {
        s"$statusCode has returned from HttpResponse" in {
          val result = preValidateTraderHttpReader.read(
            "ANY",
            "/foo",
            HttpResponse(statusCode, "")
          )

          result.left.value mustBe expectedResult
        }
      }
    }

  }
}
