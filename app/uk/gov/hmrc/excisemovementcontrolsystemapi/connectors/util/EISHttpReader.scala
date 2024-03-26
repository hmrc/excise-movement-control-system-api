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
import play.api.libs.json.{Json, Reads}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Status}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorMessage, EISErrorResponse, EISSubmissionResponse, RimValidationErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EisErrorResponsePresentation, ValidationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.reflect.runtime.universe.TypeTag
import scala.util.{Success, Try}

class EISHttpReader(
  val correlationId: String,
  val ern: String,
  val createdDateTime: String,
  val dateTimeService: DateTimeService,
  val messageType: String
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

    logger.warn(EISErrorMessage(createdDateTime, ern, response.body, correlationId, messageType))

    (tryAsJson[RimValidationErrorResponse](response), tryAsJson[EISErrorResponse](response)) match {
      case (Some(x), None) => handleRimValidationResponse(response, x)
      case (None, Some(y)) => handleEISErrorResponse(response, y)
      case _ =>
        InternalServerError(Json.toJson(EisErrorResponsePresentation(
          dateTimeService.timestamp(),
          "Unexpected error",
          "Error occurred while reading downstream response",
          correlationId))
        )
    }

  }

  private def handleRimValidationResponse(response: HttpResponse, rimError: RimValidationErrorResponse): Result = {

    val validationResponse = rimError.validatorResults.map(
      x => ValidationResponse(
        x.errorCategory,
        x.errorType,
        x.errorReason,
        removeControlDocumentReferences(x.errorLocation),
        x.originalAttributeValue
      )
    )

    Status(response.status)(Json.toJson(EisErrorResponsePresentation(
      dateTimeService.timestamp(),
      "Validation error",
      rimError.message.mkString("\n"),
      rimError.emcsCorrelationId,
      Some(validationResponse)
    )))

  }

  private def handleEISErrorResponse(response: HttpResponse, eisError: EISErrorResponse) = {
    Status(response.status)(Json.toJson(eisError.asPresentation))
  }

  private def tryAsJson[A](response: HttpResponse)(implicit reads: Reads[A], tt: TypeTag[A]): Option[A] = {

    Try(jsonAs[A](response.body)) match {
      case Success(value) => Some(value)
      case _ => None
    }
  }

}

object EISHttpReader {
  def apply(correlationId: String, ern: String, createDateTime: String, dateTimeService: DateTimeService, messageType: String): EISHttpReader = {
    new EISHttpReader(
      correlationId: String,
      ern: String,
      createDateTime: String,
      dateTimeService: DateTimeService,
      messageType: String
    )
  }
}
