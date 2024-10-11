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
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{ExciseTraderValidationETDSResponse, PreValidateTraderETDS400ErrorMessageResponse, PreValidateTraderETDS500ErrorMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderErrorETDSEISResponse400, getPreValidateTraderErrorETDSEISResponse500, getPreValidateTraderSuccessETDSEISResponse}
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant

class PreValidateTraderETDSHttpReaderSpec extends PlaySpec with EitherValues {

  private val validResponse = getPreValidateTraderSuccessETDSEISResponse
  private val businessError = getPreValidateTraderErrorETDSEISResponse400
  private val serverError   = getPreValidateTraderErrorETDSEISResponse500

  private val now             = Instant.now
  private val dateTimeService = mock[DateTimeService]

  when(dateTimeService.timestamp()).thenReturn(now)

  private val preValidateTraderETDSHttpReader =
    PreValidateTraderETDSHttpReader("123", "GB123", "date time", dateTimeService)

  "read" should {
    "return ValidETDSResponse when success" in {

      val result = preValidateTraderETDSHttpReader.read(
        "ANY",
        "/foo",
        HttpResponse(200, Json.toJson(validResponse).toString())
      )

      result match {
        case Right(successResponse: ExciseTraderValidationETDSResponse) =>
          successResponse.processingDateTime mustBe validResponse.processingDateTime
          successResponse.validationResult mustBe validResponse.validationResult
          successResponse.exciseId mustBe validResponse.exciseId
          successResponse.failDetails mustBe validResponse.failDetails
        case _                                                          =>
          fail("Expected a ValidETDSResponse")
      }
    }

    "return ETDSErrorResponse400 when business error occurs" in {
      val result = preValidateTraderETDSHttpReader.read(
        "ANY",
        "/foo",
        HttpResponse(400, Json.toJson(businessError).toString())
      )

      result match {
        case Right(errorResponse: PreValidateTraderETDS400ErrorMessageResponse) =>
          errorResponse.processingDateTime mustBe businessError.processingDateTime
          errorResponse.message mustBe businessError.message
        case _                                                                  =>
          fail("Expected a ETDSErrorResponse400")
      }
    }

    "return ETDSErrorResponse500 when business error occurs" in {
      val result = preValidateTraderETDSHttpReader.read(
        "ANY",
        "/foo",
        HttpResponse(500, Json.toJson(serverError).toString())
      )

      result match {
        case Right(errorResponse: PreValidateTraderETDS500ErrorMessageResponse) =>
          errorResponse.processingDateTime mustBe serverError.processingDateTime
          errorResponse.messages.head mustBe serverError.messages.head
        case _                                                                  =>
          fail("Expected a ETDSErrorResponse500")
      }
    }
  }
}
