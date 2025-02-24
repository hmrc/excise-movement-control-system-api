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

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@Inject
class MovementIdGenerator {
  def generateId: String = UUID.randomUUID().toString
}

case class Movement(
  _id: String,
  boxId: Option[String],
  localReferenceNumber: String,
  consignorId: String,
  consigneeId: Option[String],
  administrativeReferenceCode: Option[String],
  lastUpdated: Instant,
  messages: Seq[Message]
)

final case class Message(
  hash: Int,
  encodedMessage: String,
  messageType: String,
  messageId: String,
  recipient: String,
  boxesToNotify: Set[String],
  createdOn: Instant
) {

  override def toString: String =
    s"AMessage($hash, $messageType, $messageId, $recipient, $boxesToNotify, $createdOn)"
}

object Movement {

  private val oldFormat: OFormat[Movement] = Json.format[Movement]

  private val newFormat: OFormat[Movement] = {
    // do not remove, this is used by the macro expansion of `Json.format`
    implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format[Movement]
  }

  private val reads: Reads[Movement]    = newFormat orElse oldFormat
  private val writes: OWrites[Movement] = newFormat

  implicit val format: OFormat[Movement] = OFormat(reads, writes)

  def apply(
    boxId: Option[String],
    localReferenceNumber: String,
    consignorId: String,
    consigneeId: Option[String],
    administrativeReferenceCode: Option[String] = None,
    lastUpdated: Instant = Instant.now,
    messages: Seq[Message] = Seq.empty
  ): Movement =
    Movement(
      UUID.randomUUID().toString,
      boxId,
      localReferenceNumber,
      consignorId,
      consigneeId,
      administrativeReferenceCode,
      lastUpdated,
      messages
    )
}

object Message {
  def apply(
    encodedMessage: String,
    messageType: String,
    messageId: String,
    recipient: String,
    boxesToNotify: Set[String],
    createdOn: Instant
  ): Message =
    Message(encodedMessage.hashCode(), encodedMessage, messageType, messageId, recipient, boxesToNotify, createdOn)

  implicit val format: OFormat[Message] = {
    implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format[Message]
  }
}
