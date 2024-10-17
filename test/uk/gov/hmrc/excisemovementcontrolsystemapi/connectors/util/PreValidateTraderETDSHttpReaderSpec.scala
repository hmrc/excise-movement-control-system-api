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

import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT, INTERNAL_SERVER_ERROR, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisErrorResponsePresentation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.ExciseTraderValidationETDSResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getExciseTraderValidationETDSResponse, getPreValidateTraderETDSMessageResponseAllFail}
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant

class PreValidateTraderETDSHttpReaderSpec extends PlaySpec with EitherValues {

  private val validResponse = getExciseTraderValidationETDSResponse
  private val businessError = getPreValidateTraderETDSMessageResponseAllFail

  private val now             = Instant.now
  private val dateTimeService = mock[DateTimeService]

  when(dateTimeService.timestamp()).thenReturn(now)

  private val preValidateTraderHttpReader =
    PreValidateTraderETDSHttpReader("123", "GB123", "date time", dateTimeService)

  "read" should {
    "return PreValidateTraderEISResponse when success" in {

      val result = preValidateTraderHttpReader.read(
        "ANY",
        "/foo",
        HttpResponse(200, Json.toJson(validResponse).toString())
      )

      val responseObject = result.toOption.value
      responseObject.processingDateTime mustBe validResponse.processingDateTime
      responseObject.validationResult mustBe validResponse.validationResult

    }

    "return PreValidateTraderErrorResponse when error occurs" in {

      val result = preValidateTraderHttpReader.read(
        "ANY",
        "/foo",
        HttpResponse(200, Json.toJson(businessError).toString())
      )

      val responseObject = result.toOption.value

      responseObject.processingDateTime mustBe businessError.processingDateTime
      responseObject.validationResult mustBe businessError.validationResult
    }

    forAll(Seq(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE)) { statusCode =>
      s"return $statusCode" when {
        s"$statusCode has returned from HttpResponse" in {

          val expectedResponse = Status(statusCode)(
            Json.toJson(
              EisErrorResponsePresentation(
                now,
                "PreValidateTrader error",
                "Error occurred during PreValidateTrader request",
                "123"
              )
            )
          )

          val result = preValidateTraderHttpReader
            .read(
              "ANY",
              "/foo",
              HttpResponse(statusCode, "")
            )
            .left
            .value

          result mustBe expectedResponse
        }
      }
    }

  }
}
