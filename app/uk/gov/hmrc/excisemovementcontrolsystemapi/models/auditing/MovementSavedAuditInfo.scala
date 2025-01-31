package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage

case class KeyMessageDetailsAuditInfo(
  messageId: String,
  correlationId: Option[String],
  messageTypeCode: String,
  messageType: String
) {}

case class MovementSavedSuccessAuditInfo(
  saveStatus: String = "Success",
  messagesAdded: Int,
  totalMessages: Int,
  movementId: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: String,
  batchId: String,
  jobId: Option[String],
  keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
  fullMessageDetails: Seq[IEMessage]
) {}

object MovementSavedSuccessAuditInfo {
  def apply(
    messagesAdded: Int,
    totalMessages: Int,
    movementId: String,
    localReferenceNumber: Option[String],
    administrativeReferenceCode: Option[String],
    consignorId: String,
    consigneeId: String,
    batchId: String,
    jobId: Option[String],
    keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
    fullMessageDetails: Seq[IEMessage]
  ): MovementSavedSuccessAuditInfo =
    MovementSavedSuccessAuditInfo(
      "Success",
      messagesAdded,
      totalMessages,
      movementId,
      localReferenceNumber,
      administrativeReferenceCode,
      consignorId,
      consigneeId,
      batchId,
      jobId,
      keyMessageDetails,
      fullMessageDetails
    )
}

case class MovementSavedFailureAuditInfo(
  saveStatus: String = "Success",
  failureReason: String,
  messagesAdded: Int,
  totalMessages: Int,
  movementId: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: String,
  batchId: String,
  jobId: Option[String],
  keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
  fullMessageDetails: Seq[IEMessage]
) {}

object MovementSavedFailureAuditInfo {
  def apply(
    failureReason: String,
    messagesAdded: Int,
    totalMessages: Int,
    movementId: String,
    localReferenceNumber: Option[String],
    administrativeReferenceCode: Option[String],
    consignorId: String,
    consigneeId: String,
    batchId: String,
    jobId: Option[String],
    keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
    fullMessageDetails: Seq[IEMessage]
  ): MovementSavedFailureAuditInfo =
    MovementSavedFailureAuditInfo(
      "Failure",
      failureReason,
      messagesAdded,
      totalMessages,
      movementId,
      localReferenceNumber,
      administrativeReferenceCode,
      consignorId,
      consigneeId,
      batchId,
      jobId,
      keyMessageDetails,
      fullMessageDetails
    )
}
