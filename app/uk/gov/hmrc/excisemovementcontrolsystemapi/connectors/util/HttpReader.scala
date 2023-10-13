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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorMessage, EISResponse}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object HttpReader extends EisResponseHandler with Logging {
  def read(
            correlationId: String,
            ern: String,
            dateTime: String
          ): HttpReads[Either[Result, EISResponse]] =
    (_: String, _: String, response: HttpResponse) => {
      val result = extractIfSuccessful[EISResponse](response)
      result match {
        case Right(eisResponse) => Right(eisResponse)
        case Left(httpResponse: HttpResponse) => Left(handleErrorResponse(ern, correlationId, dateTime, httpResponse))
      }
    }

  private def handleErrorResponse
  (
    consignorId: String,
    correlationId: String,
    createdDateTime: String,
    response: HttpResponse,
  ): Result = {

    val message = response.body
    logger.warn(EISErrorMessage(createdDateTime, consignorId, message, correlationId, MessageTypes.IE815.value))

    response.status match {
      case BAD_REQUEST => BadRequest(message)
      case NOT_FOUND => NotFound(message)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(message)
      case _ => InternalServerError(message)
    }
  }
}
