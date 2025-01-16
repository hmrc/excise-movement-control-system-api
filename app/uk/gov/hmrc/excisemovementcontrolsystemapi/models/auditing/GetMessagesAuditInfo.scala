package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptySeq

import java.time.Instant

case class GetMessagesRequestAuditInfo(
  movementId: String,
  updatedSince: Option[String],
  traderType: Option[String]
)

object GetMessagesRequestAuditInfo {}

case class GetMessagesResponseAuditInfo(
  numberOfMessages: Int,
  message: Seq[MessageAuditInfo],
  localReferenceNumber: String,
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: String
)

object GetMessagesResponseAuditInfo {}

case class MessageAuditInfo(
  messageId: String,
  correlationId: String,
  messageTypeCode: String,
  messageType: String,
  recipient: String,
  createdOn: Instant
)

object MessageAuditInfo {}

case class GetMessagesAuditInfo(
  requestType: String = "MovementMessages",
  request: GetMessagesRequestAuditInfo,
  response: GetMessagesResponseAuditInfo,
  userDetails: UserDetails,
  authExciseNumber: NonEmptySeq[String]
)

object GetMessagesAuditInfo {}
