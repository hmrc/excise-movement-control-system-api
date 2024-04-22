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
import scala.util.Try

sealed trait MessageReceiptResponse

case class MessageReceiptSuccessResponse
(
  dateTime: Instant,
  exciseRegistrationNumber: String,
  recordsAffected: Int
) extends MessageReceiptResponse

object MessageReceiptSuccessResponse {

  implicit lazy val format: OFormat[MessageReceiptSuccessResponse] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    val readNumber: Reads[Int] =
      Reads.IntReads orElse Reads.StringReads.flatMap { string =>
        Try(string.toInt).map(Reads.pure(_)).getOrElse(Reads.failed("invalid number"))
      }

    lazy val writes: OWrites[MessageReceiptSuccessResponse] = Json.writes[MessageReceiptSuccessResponse]

    lazy val reads: Reads[MessageReceiptSuccessResponse] = (
      (__ \ "dateTime").read[Instant] and
      (__ \ "exciseRegistrationNumber").read[String] and
      (__ \ "recordsAffected").read(readNumber)
    )(MessageReceiptSuccessResponse.apply _)

    OFormat(reads, writes)
  }
}

case class MessageReceiptFailResponse
(
  status: Int,
  dateTime: Instant,
  debugMessage: String
) extends MessageReceiptResponse with GenericErrorResponse {
  override val message = "Message Receipt error"
}

object MessageReceiptFailResponse {
  implicit val format: OFormat[MessageReceiptFailResponse] = Json.format[MessageReceiptFailResponse]
}