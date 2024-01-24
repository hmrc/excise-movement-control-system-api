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

//todo: this could be refactor to create specific error response
// for each endpoint, (submission, notification, clientid error, etc

trait GenericErrorResponse {
  val dateTime: Instant
  val message: String
  val debugMessage: String
}

case class ErrorResponse
(
  override val dateTime: Instant,
  override val message: String,
  override val debugMessage: String) extends GenericErrorResponse

object ErrorResponse {
  implicit def format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

