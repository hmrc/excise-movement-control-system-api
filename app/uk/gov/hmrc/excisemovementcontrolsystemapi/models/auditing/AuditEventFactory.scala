package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptyList
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.xml.NodeSeq

object AuditEventFactory extends Auditing {

  def createMessageAuditEvent(input: IEMessage, failureOpt: Option[String])(implicit
    hc: HeaderCarrier
  ): ExtendedDataEvent = {
    val detail = AuditDetail(input.messageType, input.toJson, failureOpt)
    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = input.messageAuditType.name,
      tags = hc.toAuditTags(),
      detail = detail.toJsObj
    )
  }

  //Two needed, different resources
  def createMessageSubmitted(
    message: IEMessage,
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  ): Unit =
    MessageSubmittedDetails(
      message.messageType,
      message.messageAuditType.name,
      movement.localReferenceNumber,
      movement.administrativeReferenceCode,
      movement._id,
      movement.consignorId,
      movement.consigneeId,
      submittedToCore,
      message.messageIdentifier,
      correlationId,
      UserDetails(
      ),
      NonEmptyList(request.erns.head, request.erns.tail.toList),
      message.toJson
    )
}
