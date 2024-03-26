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

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat

import java.time.Instant

case class EISErrorResponse(dateTime: Instant,
                            status: String,
                            message: String,
                            debugMessage: String,
                            emcsCorrelationId: String
                           )

object EISErrorResponse {
  implicit val format: Reads[EISErrorResponse] = Json.reads[EISErrorResponse]

  implicit val write: Writes[EISErrorResponse] = (
    (JsPath \ "dateTime").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "debugMessage").write[String] and
      (JsPath \ "emcsCorrelationId").write[String]

    )(e => (e.dateTime.asStringInMilliseconds, e.status, e.message, e.debugMessage, e.emcsCorrelationId))
}
