package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptySeq

case class GetSpecificMessageRequestAuditInfo(movementId: String, messageId: String)

case class GetSpecificMessageResponseAuditInfo(
  correlationId: Option[String],
  messageTypeCode: String,
  messageType: String,
  localReferenceNumber: String,
  administrativeReferenceCode: String,
  consignorId: String,
  consigneeId: String
)

case class GetSpecificMessageAuditInfo(
  requestType: String = "SpecificMessage",
  request: GetSpecificMessageRequestAuditInfo,
  response: GetSpecificMessageResponseAuditInfo,
  userDetails: UserDetails,
  authExciseNumber: NonEmptySeq[String]
)
