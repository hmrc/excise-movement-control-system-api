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

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

//todo - EMCS-389: this could be refactor to create specific error response
// for each endpoint, (submission, notification, clientid error, etc

trait GenericErrorResponse {
  val dateTime: Instant
  val message: String
  val debugMessage: String
  val correlationId: Option[String]
}

case class ErrorResponse
(
  override val dateTime: Instant,
  override val message: String,
  override val debugMessage: String,
  override val correlationId: Option[String] = None,
  validatorResults: Option[Seq[ValidationResponse]] = None) extends GenericErrorResponse

object ErrorResponse {

  def apply(dateTime: Instant, message: String, debugMessage: String, correlationId: String): ErrorResponse =
    ErrorResponse(dateTime, message, debugMessage, Some(correlationId))

  implicit def format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

case class ValidationResponse(
  errorCategory: String,
  errorType: BigInt,
  errorReason: String,
  errorLocation: String,
  originalAttributeValue: String
)

object ValidationResponse {
  implicit def format: OFormat[ValidationResponse] = Json.format[ValidationResponse]
}



