/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisErrorResponsePresentation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISErrorMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{ExciseTraderValidationETDSResponse, PreValidateTraderETDS400ErrorMessageResponse, PreValidateTraderETDS500ErrorMessageResponse, PreValidateTraderETDSResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

class PreValidateTraderETDSHttpReader(
  val correlationId: String,
  val ern: String,
  val createdDateTime: String,
  val dateTimeService: DateTimeService
) extends HttpReads[Either[Result, PreValidateTraderETDSResponse]]
    with Logging
    with ResponseHandler {

  override def read(
    method: String,
    url: String,
    response: HttpResponse
  ): Either[Result, PreValidateTraderETDSResponse] = {

    val result = extractIfSuccessful(response)
    result match {
      case Right(eisResponse) =>
        Right(eisResponse) // Success case

      case Left(httpResponse: HttpResponse) =>
        Left(handleErrorResponse(httpResponse))
    }
  }

  private def handleErrorResponse(
                                   response: HttpResponse
                                 ): Result = {

    logger.warn(
      EISErrorMessage(
        createdDateTime,
        s"status: ${response.status}, body: ${response.body}",
        correlationId,
        "PreValidateTrader"
      )
    )

    //Not expecting EIS response bodies to have any payload here
    val ourErrorResponse = EisErrorResponsePresentation(
      dateTimeService.timestamp(),
      "PreValidateTrader error",
      "Error occurred during PreValidateTrader request",
      correlationId
    )

    Status(response.status)(Json.toJson(ourErrorResponse))
  }

  def extractIfSuccessful(response: HttpResponse): Either[HttpResponse, ExciseTraderValidationETDSResponse] =
    if (is2xx(response.status)) Right(extractResponse(response))
    else Left(response)

  private def extractResponse(httpResponse: HttpResponse): ExciseTraderValidationETDSResponse =
    jsonAs[ExciseTraderValidationETDSResponse](httpResponse.body)

  private def defaultErrorResponse(status: Int): Result = {
    val ourErrorResponse = EisErrorResponsePresentation(
      dateTimeService.timestamp(),
      "ETDS PreValidateTrader error",
      s"Error occurred during ETDS PreValidateTrader request with status: $status",
      correlationId
    )
    Status(status)(Json.toJson(ourErrorResponse))
  }
}

object PreValidateTraderETDSHttpReader {
  def apply(
    correlationId: String,
    ern: String,
    createDateTime: String,
    dateTimeService: DateTimeService
  ): PreValidateTraderETDSHttpReader =
    new PreValidateTraderETDSHttpReader(
      correlationId,
      ern,
      createDateTime,
      dateTimeService
    )
}
