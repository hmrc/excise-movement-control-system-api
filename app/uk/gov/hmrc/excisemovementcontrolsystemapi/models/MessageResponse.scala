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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat

import java.time.Instant

case class MessageResponse(
  encodedMessage: String,
  messageType: String,
  recipient: String,
  messageId: String,
  createdOn: Instant
)

object MessageResponse {
  implicit val format: Reads[MessageResponse] = Json.reads[MessageResponse]

  implicit val write: Writes[MessageResponse] = (
    (JsPath \ "encodedMessage").write[String] and
      (JsPath \ "messageType").write[String] and
      (JsPath \ "recipient").write[String] and
      (JsPath \ "messageId").write[String] and
      (JsPath \ "createdOn").write[String]
  )(e =>
    (
      e.encodedMessage,
      e.messageType,
      e.recipient,
      e.messageId,
      e.createdOn.asStringInMilliseconds
    )
  )
}
