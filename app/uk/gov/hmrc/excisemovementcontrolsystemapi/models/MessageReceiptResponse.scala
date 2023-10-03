package uk.gov.hmrc.excisemovementcontrolsystemapi.models

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class MessageReceiptResponse
(
    dateTime: LocalDateTime,
    exciseRegistrationNumber: String,
    recordsAffected: Int
)

object MessageReceiptResponse {
  implicit val format: OFormat[MessageReceiptResponse] = Json.format[MessageReceiptResponse]
}
