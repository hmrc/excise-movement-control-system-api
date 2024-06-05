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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisErrorResponsePresentation

import java.time.Instant

case class EISErrorResponse(
  dateTime: Instant,
  status: String,
  message: String,
  debugMessage: String,
  emcsCorrelationId: String
) {}

object EISErrorResponse {
  implicit val format: OFormat[EISErrorResponse] = Json.format[EISErrorResponse]

  implicit class Presentation(val errorResponse: EISErrorResponse) extends AnyVal {

    implicit def asPresentation: EisErrorResponsePresentation =
      EisErrorResponsePresentation(
        errorResponse.dateTime,
        errorResponse.message,
        errorResponse.debugMessage,
        errorResponse.emcsCorrelationId
      )

  }
}
