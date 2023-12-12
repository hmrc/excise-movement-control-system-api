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

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class Movement(
                     localReferenceNumber: String,
                     consignorId: String,
                     consigneeId: Option[String],
                     administrativeReferenceCode: Option[String] = None,
                     lastUpdated: Instant = Instant.now,
                     messages: Seq[Message] = Seq.empty
                   )

case class Message(
                    hash: Int,
                    encodedMessage: String,
                    messageType: String,
                    createdOn: Instant = Instant.now
                  )

object Movement {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[Movement] = Json.format[Movement]
}

object Message {
  def apply(
             encodedMessage: String,
             messageType: String,
             dateTimeService: DateTimeService): Message = {

    Message(encodedMessage.hashCode(), encodedMessage, messageType, createdOn)
  }

  implicit val format: OFormat[Message] = Json.format[Message]
}
