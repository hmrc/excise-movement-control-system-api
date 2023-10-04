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

package uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class MovementMessage(
                            localReferenceNumber: String,
                            consignorId: String,
                            consigneeId: Option[String],
                            administrativeReferenceCode: Option[String] = None,
                            createdOn: Instant = Instant.now,
                            messages: Option[Seq[Message]] = None
                          )

case class Message(encodedMessage: String, messageType: String, createdOn: Instant = Instant.now)


object MovementMessage {
  implicit val format: OFormat[MovementMessage] = Json.format[MovementMessage]
}

object Message {
  implicit val format: OFormat[Message] = Json.format[Message]
}
