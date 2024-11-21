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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, RimValidationErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat

import java.time.Instant

trait GenericErrorResponse {
  val dateTime: Instant
  val message: String
  val debugMessage: String
}

case class ErrorResponse(
  override val dateTime: Instant,
  override val message: String,
  override val debugMessage: String
) extends GenericErrorResponse

object ErrorResponse {

  implicit val format: Reads[ErrorResponse] = Json.reads[ErrorResponse]

  implicit val write: Writes[ErrorResponse] = (
    (JsPath \ "dateTime").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "debugMessage").write[String]
  )(e => (e.dateTime.asStringInMilliseconds, e.message, e.debugMessage))

}

case class EISErrorResponseDetails(
  override val dateTime: Instant,
  override val message: String,
  override val debugMessage: String,
  validatorResults: Option[Seq[ValidationResponse]] = None
) extends GenericErrorResponse

case class EisErrorResponsePresentation(
  override val dateTime: Instant,
  override val message: String,
  override val debugMessage: String,
  correlationId: String,
  validatorResults: Option[Seq[ValidationResponse]] = None
) extends GenericErrorResponse

object EisErrorResponsePresentation {

  def apply(error: EISErrorResponse): EisErrorResponsePresentation =
    EisErrorResponsePresentation(
      DateTimeService.timestamp(),
      "Unexpected error",
      "Error occured while reading downstream response",
      error.emcsCorrelationId
    )

  def apply(error: RimValidationErrorResponse): EisErrorResponsePresentation = {
    val validationResponse = error.validatorResults.map(x =>
      ValidationResponse(
        x.errorCategory,
        x.errorType,
        x.errorReason,
        removeControlDocumentReferences(x.errorLocation),
        x.originalAttributeValue
      )
    )

    EisErrorResponsePresentation(
      DateTimeService.timestamp(),
      "Validation error",
      error.message.mkString("\n"),
      error.emcsCorrelationId,
      Some(validationResponse)
    )
  }

  private def removeControlDocumentReferences(errorMsg: Option[String]): Option[String] =
    errorMsg.map(x => x.replaceAll("/con:[^/]*(?=/)", ""))

  implicit val format: Reads[EisErrorResponsePresentation] = Json.reads[EisErrorResponsePresentation]

  implicit val write: Writes[EisErrorResponsePresentation] = (
    (JsPath \ "dateTime").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "debugMessage").write[String] and
      (JsPath \ "correlationId").write[String] and
      (JsPath \ "validatorResults").writeNullable[Seq[ValidationResponse]]
  )(e =>
    (
      e.dateTime.asStringInMilliseconds,
      e.message,
      e.debugMessage,
      e.correlationId,
      e.validatorResults
    )
  )

}

case class ValidationResponse(
  errorCategory: Option[String],
  errorType: Option[BigInt],
  errorReason: Option[String],
  errorLocation: Option[String],
  originalAttributeValue: Option[String]
)

object ValidationResponse {
  implicit def format: OFormat[ValidationResponse] = Json.format[ValidationResponse]
}
