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
import play.api.libs.json.{Format, JsError, JsSuccess, Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisErrorResponsePresentation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.ETDSFailDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

class PreValidateTraderETDSHttpReader(
  val correlationId: String,
  val ern: String,
  val createdDateTime: String,
  val dateTimeService: DateTimeService
) extends HttpReads[Either[Result, PreValidateTraderResponse]]
    with Logging
    with ResponseHandler {

  override def read(
    method: String,
    url: String,
    response: HttpResponse
  ): Either[Result, PreValidateTraderResponse] = {

    val result = extractIfSuccessful(response)
    result match {
      case Right(eisResponse) =>
        Right(eisResponse) // Success case

      case Left(httpResponse: HttpResponse) =>
        handleErrorResponse(httpResponse)
    }
  }

  private def handleErrorResponse(
    response: HttpResponse
  ): Either[Result, PreValidateTraderResponse] =
    response.status match {
      case 400 if response.body.trim.nonEmpty =>
        Json.parse(response.body).validate[PreValidateTraderETDS400ErrorMessageResponse] match {
          case JsSuccess(errorResponse, _) =>
            Right(errorResponse)
          case JsError(errors)             =>
            logger.error(s"Failed to parse 400 response: $errors")
            Left(defaultErrorResponse(400))
        }

      case 500 if response.body.trim.nonEmpty =>
        Json.parse(response.body).validate[PreValidateTraderETDS500ErrorMessageResponse] match {
          case JsSuccess(errorResponse, _) =>
            Right(errorResponse)
          case JsError(errors)             =>
            logger.error(s"Failed to parse 500 response: $errors")
            Left(defaultErrorResponse(500))
        }

      case _ =>
        Left(defaultErrorResponse(response.status))
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

sealed trait PreValidateTraderResponse

object PreValidateTraderResponse {
  implicit val preValidateTraderResponseWrites: Writes[PreValidateTraderResponse] = Writes[PreValidateTraderResponse] {
    case validResponse: ExciseTraderValidationETDSResponse      =>
      Json.toJson(validResponse)
    case error400: PreValidateTraderETDS400ErrorMessageResponse =>
      Json.toJson(error400)
    case error500: PreValidateTraderETDS500ErrorMessageResponse =>
      Json.toJson(error500)
  }
}

case class ExciseTraderValidationETDSResponse(
  processingDateTime: String,
  exciseId: String,
  validationResult: String,
  failDetails: Option[ETDSFailDetails] = None
) extends PreValidateTraderResponse

object ExciseTraderValidationETDSResponse {
  implicit val format: Format[ExciseTraderValidationETDSResponse] = Json.format[ExciseTraderValidationETDSResponse]
}

case class PreValidateTraderETDS400ErrorMessageResponse(
  processingDateTime: String,
  message: String
) extends PreValidateTraderResponse

object PreValidateTraderETDS400ErrorMessageResponse {
  implicit val format: Format[PreValidateTraderETDS400ErrorMessageResponse] =
    Json.format[PreValidateTraderETDS400ErrorMessageResponse]
}

case class PreValidateTraderETDS500ErrorMessageResponse(
  processingDateTime: String,
  messages: Seq[String]
) extends PreValidateTraderResponse

object PreValidateTraderETDS500ErrorMessageResponse {
  implicit val format: Format[PreValidateTraderETDS500ErrorMessageResponse] =
    Json.format[PreValidateTraderETDS500ErrorMessageResponse]
}
