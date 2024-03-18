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

sealed trait MessageReceiptResponse

case class MessageReceiptSuccessResponse
(
  dateTime: Instant,
  exciseRegistrationNumber: String,
  recordsAffected: Int
) extends MessageReceiptResponse

object MessageReceiptSuccessResponse {
  implicit val format: OFormat[MessageReceiptSuccessResponse] = Json.format[MessageReceiptSuccessResponse]
}

case class MessageReceiptFailResponse
(
  status: Int,
  dateTime: Instant,
  debugMessage: String,
  correlationId: Option[String]
) extends MessageReceiptResponse with GenericErrorResponse {
  override val message = "Message Receipt error"
}

object MessageReceiptFailResponse {
  implicit val format: OFormat[MessageReceiptFailResponse] = Json.format[MessageReceiptFailResponse]
}