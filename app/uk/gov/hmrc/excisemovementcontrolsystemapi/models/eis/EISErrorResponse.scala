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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse

import java.time.LocalDateTime

class EISErrorResponse(dateTime: LocalDateTime,
                       message: String,
                       debugMessage: String,
                       emcsCorrelationId: String
                      ) extends ErrorResponse(dateTime, message, debugMessage, emcsCorrelationId)

object EISErrorResponse {
  implicit def format: OFormat[EISErrorResponse] = Json.format[EISErrorResponse]

  def apply(dateTime: LocalDateTime, message: String, debugMessage: String, emcsCorrelationId: String): EISErrorResponse = {
    new EISErrorResponse(dateTime, message, debugMessage, emcsCorrelationId)
  }

  def unapply(eisErrorResponse: EISErrorResponse): Option[(LocalDateTime, String, String, String)] = {
    Some((eisErrorResponse.dateTime, eisErrorResponse.message, eisErrorResponse.debugMessage, eisErrorResponse.emcsCorrelationId))
  }
}
