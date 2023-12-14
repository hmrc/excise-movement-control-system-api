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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorMessage, EISSubmissionResponse, RimValidationErrorResponse}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.util.{Success, Try}

class EISHttpReader(
                     val correlationId: String,
                     val ern: String,
                     val createdDateTime: String
                   ) extends HttpReads[Either[Result, EISSubmissionResponse]]
  with Logging
  with ResponseHandler {

  override def read(method: String, url: String, response: HttpResponse): Either[Result, EISSubmissionResponse] = {
    val result = extractIfSuccessful[EISSubmissionResponse](response)
    result match {
      case Right(eisResponse) => Right(eisResponse)
      case Left(httpResponse: HttpResponse) => Left(handleErrorResponse(httpResponse))
    }
  }

  private def handleErrorResponse
  (
    response: HttpResponse
  ): Result = {

    logger.warn(EISErrorMessage(createdDateTime, ern, response.body, correlationId, MessageTypes.IE815.value))

    val messageAsJson = response.json

    response.status match {
      case BAD_REQUEST => extractValidJson(response)
      case NOT_FOUND => NotFound(messageAsJson)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(messageAsJson)
      case _ => InternalServerError(messageAsJson)
    }
  }

  private def extractValidJson(response: HttpResponse): Result = {
    Try(jsonAs[RimValidationErrorResponse](response.body)) match {
      case Success(value) =>
        val errors = value.validatorResults.map(o => o.copy(errorLocation = removeControlDocumentReferences(o.errorLocation)))
        BadRequest(Json.toJson(value.copy(validatorResults = errors)))
      case _ => BadRequest(response.json)
    }
  }
}

object EISHttpReader {
  def apply(correlationId: String, ern: String, createDateTime: String): EISHttpReader = {
    new EISHttpReader(
      correlationId: String,
      ern: String,
      createDateTime: String
    )
  }
}
