package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

case class KeyMessageDetailsAuditInfo(
  messageId: String,
  correlationId: Option[String],
  messageTypeCode: String,
  messageType: String
) {}

object KeyMessageDetailsAuditInfo {
  implicit val writes = Json.writes[KeyMessageDetailsAuditInfo]
}

case class MovementSavedSuccessAuditInfo(
  saveStatus: String = "Success",
  messagesAdded: Int,
  totalMessages: Int,
  movementId: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: Option[String],
  batchId: String,
  jobId: Option[String],
  keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
  fullMessageDetails: Seq[Message]
) {}

object MovementSavedSuccessAuditInfo {
  def apply(
    messagesAdded: Int,
    totalMessages: Int,
    movementId: String,
    localReferenceNumber: Option[String],
    administrativeReferenceCode: Option[String],
    consignorId: String,
    consigneeId: Option[String],
    batchId: String,
    jobId: Option[String],
    keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
    fullMessageDetails: Seq[Message]
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

  implicit val write: OWrites[MovementSavedSuccessAuditInfo] =
    (
      (JsPath \ "saveStatus").write[String] and
        (JsPath \ "messagesAdded").write[Int] and
        (JsPath \ "totalMessages").write[Int] and
        (JsPath \ "movementId").write[String] and
        (JsPath \ "localReferenceNumber").write[Option[String]] and
        (JsPath \ "administrativeReferenceCode").write[Option[String]] and
        (JsPath \ "consignorId").write[String] and
        (JsPath \ "consigneeId").write[Option[String]] and
        (JsPath \ "batchId").write[String] and
        (JsPath \ "jobId").write[Option[String]] and
        (JsPath \ "keyMessageDetails").write[Seq[KeyMessageDetailsAuditInfo]] and
        (JsPath \ "fullMessageDetails").write[Seq[Message]]
    )(unlift(MovementSavedSuccessAuditInfo.unapply))
}

case class MovementSavedFailureAuditInfo(
  saveStatus: String = "Failure",
  failureReason: String,
  messagesToBeAdded: Int,
  totalMessages: Int, // number on the movement in total after saving.
  movementId: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: Option[String],
  batchId: String,
  jobId: Option[String],
  keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
  fullMessageDetails: Seq[Message]
) {}

object MovementSavedFailureAuditInfo {
  def apply(
    failureReason: String,
    messagesToBeAdded: Int, // messages that were *supposed* to be added?
    totalMessages: Int, // messages that were+are on the mvt (i.e. not changed due to save failure)
    movementId: String,
    localReferenceNumber: Option[String],
    administrativeReferenceCode: Option[String],
    consignorId: String,
    consigneeId: Option[String],
    batchId: String,
    jobId: Option[String],
    keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
    fullMessageDetails: Seq[Message]
  ): MovementSavedFailureAuditInfo =
    MovementSavedFailureAuditInfo(
      "Failure",
      failureReason,
      messagesToBeAdded,
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

  implicit val write: OWrites[MovementSavedFailureAuditInfo] =
    ((JsPath \ "saveStatus").write[String] and
      (JsPath \ "failureReason").write[String] and
      (JsPath \ "messagesToBeAdded").write[Int] and
      (JsPath \ "totalMessages").write[Int] and
      (JsPath \ "movementId").write[String] and
      (JsPath \ "localReferenceNumber").write[Option[String]] and
      (JsPath \ "administrativeReferenceCode").write[Option[String]] and
      (JsPath \ "consignorId").write[String] and
      (JsPath \ "consigneeId").write[Option[String]] and
      (JsPath \ "batchId").write[String] and
      (JsPath \ "jobId").write[Option[String]] and
      (JsPath \ "keyMessageDetails").write[Seq[KeyMessageDetailsAuditInfo]] and
      (JsPath \ "fullMessageDetails").write[Seq[Message]])(unlift(MovementSavedFailureAuditInfo.unapply))
}
