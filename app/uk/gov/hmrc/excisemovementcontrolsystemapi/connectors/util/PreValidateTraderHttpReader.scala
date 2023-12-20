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

import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISErrorMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.PreValidateTraderResponse
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

class PreValidateTraderHttpReader(
                                   val correlationId: String,
                                   val ern: String,
                                   val createdDateTime: String
                                 ) extends HttpReads[Either[Result, PreValidateTraderResponse]]
  with Logging
  with ResponseHandler {

  override def read(method: String, url: String, response: HttpResponse): Either[Result, PreValidateTraderResponse] = {

    val result = extractIfSuccessful(response)
    result match {
      case Right(eisResponse) => Right(eisResponse)
      case Left(httpResponse: HttpResponse) => Left(handleErrorResponse(httpResponse))
    }
  }

  def extractIfSuccessful(response: HttpResponse): Either[HttpResponse, PreValidateTraderResponse] =
    if (is2xx(response.status)) Right(extractResponse(response))
    else Left(response)

  private def extractResponse(httpResponse: HttpResponse): PreValidateTraderResponse = {
    jsonAs[PreValidateTraderResponse](httpResponse.body)
  }


  private def handleErrorResponse
  (
    response: HttpResponse
  ): Result = {

    logger.warn(EISErrorMessage(createdDateTime, ern, response.body, correlationId, "PreValidateTrader"))

    //Not expecting bodies to have any payload here
    response.status match {
      case BAD_REQUEST =>
        BadRequest(response.body)
      case NOT_FOUND => NotFound(response.body)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(response.body)
      case _ => InternalServerError(response.body)
    }
  }

}

object PreValidateTraderHttpReader {
  def apply(correlationId: String, ern: String, createDateTime: String): PreValidateTraderHttpReader = {
    new PreValidateTraderHttpReader(
      correlationId: String,
      ern: String,
      createDateTime: String
    )
  }
}
