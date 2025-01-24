package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Json, OWrites}

case class MessageProcessingMessageAuditInfo(
  messageId: String,
  correlationId: String,
  messageTypeCode: String,
  messageType: String,
  localReferenceNumber: String,
  administrativeReferenceCode: String,
  consignorId: String,
  consigneeId: String
)
object MessageProcessingMessageAuditInfo {
  implicit val writes = Json.writes[MessageProcessingMessageAuditInfo]

}

case class MessageProcessingSuccessAuditInfo(
  exciseRegistrationNumber: String,
  messagesAvailable: Int,
  messagesInBatch: Int,
  messages: Seq[MessageProcessingMessageAuditInfo],
  processingStatus: String = "Success",
  batchId: String,
  jobId: Option[String]
)

object MessageProcessingSuccessAuditInfo {

  def apply(
    exciseRegistrationNumber: String,
    messagesAvailable: Int,
    messagesInBatch: Int,
    messages: Seq[MessageProcessingMessageAuditInfo],
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingSuccessAuditInfo =
    MessageProcessingSuccessAuditInfo(
      exciseRegistrationNumber,
      messagesAvailable,
      messagesInBatch,
      messages,
      "Success",
      batchId,
      jobId
    )

  implicit val write: OWrites[MessageProcessingSuccessAuditInfo] =
    (
      (JsPath \ "exciseRegistrationNumber").write[String] and
        (JsPath \ "messagesAvailable").write[Int] and
        (JsPath \ "messagesInBatch").write[Int] and
        (JsPath \ "messages").write[Seq[MessageProcessingMessageAuditInfo]] and
        (JsPath \ "processingStatus").write[String] and
        (JsPath \ "batchId").write[String] and
        (JsPath \ "jobId").write[Option[String]]
    )(unlift(MessageProcessingSuccessAuditInfo.unapply))
}

case class MessageProcessingFailureAuditInfo(
  exciseRegistrationNumber: String,
  messagesAvailable: Int, // TODO: remove if information is not available
  messagesInBatch: Int, // TODO: remove if information is not available
  processingStatus: String = "Failure",
  failureReason: String,
  batchId: String,
  jobId: Option[String]
)

object MessageProcessingFailureAuditInfo {

  def apply(
    exciseRegistrationNumber: String,
    messagesAvailable: Int,
    messagesInBatch: Int,
    failureReason: String,
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingFailureAuditInfo                           =
    MessageProcessingFailureAuditInfo(
      exciseRegistrationNumber,
      messagesAvailable,
      messagesInBatch,
      "Failure",
      failureReason,
      batchId,
      jobId
    )
  implicit val write: OWrites[MessageProcessingFailureAuditInfo] =
    (
      (JsPath \ "exciseRegistrationNumber").write[String] and
        (JsPath \ "messagesAvailable").write[Int] and
        (JsPath \ "messagesInBatch").write[Int] and
        (JsPath \ "processingStatus").write[String] and
        (JsPath \ "failureReason").write[String] and
        (JsPath \ "batchId").write[String] and
        (JsPath \ "jobId").write[Option[String]]
    )(unlift(MessageProcessingFailureAuditInfo.unapply))
}
