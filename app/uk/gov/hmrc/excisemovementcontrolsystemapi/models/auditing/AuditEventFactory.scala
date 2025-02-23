/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptySeq
import play.api.mvc.AnyContent
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}
import scala.xml.NodeSeq

@Singleton
class AuditEventFactory @Inject() (emcsUtils: EmcsUtils, ieMessageFactory: IEMessageFactory) extends Auditing {

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

  def createMessageSubmittedNoMovement(
    message: IE815Message,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  ): MessageSubmittedDetails = {
    val consignorId: String =
      message.consignorId.getOrElse(
        throw new Exception(s"No Consignor on IE815: ${message.messageIdentifier}")
      )
    MessageSubmittedDetails(
      message.messageType,
      message.messageAuditType.name,
      message.localReferenceNumber,
      None,
      None,
      consignorId,
      message.consigneeId,
      submittedToCore,
      message.messageIdentifier,
      correlationId,
      request.userDetails,
      NonEmptySeq(request.erns.head, request.erns.tail.toList),
      message.toJsObject
    )
  }

  def createMessageSubmitted(
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  ): MessageSubmittedDetails =
    MessageSubmittedDetails(
      request.ieMessage.messageType,
      request.ieMessage.messageAuditType.name,
      movement.localReferenceNumber,
      movement.administrativeReferenceCode,
      Some(movement._id),
      movement.consignorId,
      movement.consigneeId,
      submittedToCore,
      request.ieMessage.messageIdentifier,
      correlationId,
      request.userDetails,
      NonEmptySeq(request.erns.head, request.erns.tail.toList),
      request.ieMessage.toJsObject
    )

  def createGetMovementsDetails(
    movementFilter: MovementFilter,
    movements: Seq[Movement],
    request: EnrolmentRequest[AnyContent]
  ): GetMovementsAuditInfo = {
    val parameters = GetMovementsParametersAuditInfo(
      movementFilter.ern,
      movementFilter.arc,
      movementFilter.lrn,
      movementFilter.updatedSince.map(x => x.toString),
      movementFilter.traderType.map(x => x.traderType)
    )
    val response   = GetMovementsResponseAuditInfo(
      movements.length
    )
    GetMovementsAuditInfo(
      request = parameters,
      response = response,
      userDetails = request.userDetails,
      authExciseNumber = convertErns(request.erns)
    )

  }

  def createGetSpecificMovementDetails(
    movementId: String,
    request: EnrolmentRequest[AnyContent]
  ): GetSpecificMovementAuditInfo =
    GetSpecificMovementAuditInfo(
      request = GetSpecificMovementRequestAuditInfo(movementId),
      userDetails = request.userDetails,
      authExciseNumber = convertErns(request.erns)
    )

  def createGetMessagesAuditInfo(
    messages: Seq[Message],
    movement: Movement,
    updatedSince: Option[String],
    traderType: Option[String],
    request: EnrolmentRequest[AnyContent]
  ): GetMessagesAuditInfo = {
    val requestAuditInfo  = GetMessagesRequestAuditInfo(movement._id, updatedSince, traderType)
    val messagesAuditInfo = messages.map { msg =>
      val decodedXml         = emcsUtils.decode(msg.encodedMessage)
      val decodedXmlNodeList = xml.XML.loadString(decodedXml)

      val ieMessage = ieMessageFactory.createFromXml(msg.messageType, decodedXmlNodeList)
      MessageAuditInfo(
        msg.messageId,
        ieMessage.correlationId,
        ieMessage.messageType,
        ieMessage.messageAuditType.name,
        msg.recipient,
        msg.createdOn
      )
    }
    val response          = GetMessagesResponseAuditInfo(
      messages.length,
      messagesAuditInfo,
      movement.localReferenceNumber,
      movement.administrativeReferenceCode,
      movement.consignorId,
      movement.consigneeId
    )
    GetMessagesAuditInfo(
      request = requestAuditInfo,
      response = response,
      userDetails = request.userDetails,
      authExciseNumber = convertErns(request.erns)
    )
  }

  def createGetSpecificMessageAuditInfo(
    movement: Movement,
    message: Message,
    request: EnrolmentRequest[AnyContent]
  ): GetSpecificMessageAuditInfo = {
    val requestAuditInfo   = GetSpecificMessageRequestAuditInfo(movement._id, message.messageId)
    val decodedXml         = emcsUtils.decode(message.encodedMessage)
    val decodedXmlNodeList = xml.XML.loadString(decodedXml)

    val ieMessage = ieMessageFactory.createFromXml(message.messageType, decodedXmlNodeList)
    val response  = GetSpecificMessageResponseAuditInfo(
      ieMessage.correlationId,
      ieMessage.messageType,
      ieMessage.messageAuditType.name,
      movement.localReferenceNumber,
      movement.administrativeReferenceCode,
      movement.consignorId,
      movement.consigneeId
    )
    GetSpecificMessageAuditInfo(
      request = requestAuditInfo,
      response = response,
      userDetails = request.userDetails,
      authExciseNumber = convertErns(request.erns)
    )
  }

  def createMessageProcessingSuccessAuditInfo(
    ern: String,
    response: GetMessagesResponse,
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingSuccessAuditInfo = {

    val messages =
      response.messages.map(message =>
        MessageProcessingMessageAuditInfo(
          message.messageIdentifier,
          message.correlationId,
          message.messageType,
          message.messageAuditType.name,
          message.optionalLocalReferenceNumber,
          message.administrativeReferenceCode.head,
          message.consignorId,
          message.consigneeId
        )
      )

    MessageProcessingSuccessAuditInfo(
      ern,
      response.messageCount,
      response.messages.length,
      messages,
      batchId,
      jobId
    )
  }

  def createMessageProcessingFailureAuditInfo(
    ern: String,
    failureReason: String,
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingFailureAuditInfo = MessageProcessingFailureAuditInfo(ern, failureReason, batchId, jobId)

  def createMovementSavedSuccessAuditInfo(
    updatedMovement: Movement,
    batchId: Option[String],
    jobId: Option[String],
    newMessages: Seq[Message]
  ): MovementSavedSuccessAuditInfo = {
    val messagePairs = convertToIEMessage(newMessages)
    MovementSavedSuccessAuditInfo(
      newMessages.length,
      updatedMovement.messages.length,
      updatedMovement._id,
      Some(updatedMovement.localReferenceNumber),
      updatedMovement.administrativeReferenceCode,
      updatedMovement.consignorId,
      updatedMovement.consigneeId,
      batchId,
      jobId,
      generateKeyMessageDetailsAuditInfo(messagePairs),
      messagePairs.map(_._2.toJson)
    )
  }

  def createMovementSavedFailureAuditInfo(
    movement: Movement,
    failureReason: String,
    batchId: Option[String],
    jobId: Option[String],
    messagesToBeAdded: Seq[Message]
  ): MovementSavedFailureAuditInfo = {
    val messagePairs = convertToIEMessage(movement.messages)
    MovementSavedFailureAuditInfo(
      failureReason,
      messagesToBeAdded.length,
      movement.messages.length,
      movement._id,
      Some(movement.localReferenceNumber),
      movement.administrativeReferenceCode,
      movement.consignorId,
      movement.consigneeId,
      batchId,
      jobId,
      generateKeyMessageDetailsAuditInfo(messagePairs),
      messagePairs.map(_._2.toJson)
    )
  }
  private def generateKeyMessageDetailsAuditInfo(
    messagePairs: Seq[(Message, IEMessage)]
  ): Seq[KeyMessageDetailsAuditInfo] =
    messagePairs.map { pair =>
      val (message, ieMessage) = pair
      val correlationId        = ieMessage.correlationId
      KeyMessageDetailsAuditInfo(
        ieMessage.messageIdentifier,
        correlationId,
        ieMessage.messageType,
        ieMessage.messageAuditType.name,
        message.recipient
      )
    }

  private def convertToIEMessage(messages: Seq[Message]): Seq[(Message, IEMessage)] =
    messages.map { message =>
      val decodedXml         = emcsUtils.decode(message.encodedMessage)
      val decodedXmlNodeList = xml.XML.loadString(decodedXml)
      val ieMessage          = ieMessageFactory.createFromXml(message.messageType, decodedXmlNodeList)
      (message, ieMessage)
    }

  def createMessageAcknowledgedEvent(
    ern: String,
    batchId: String,
    jobId: Option[String],
    recordsAffected: Int
  ): MessageAcknowledgedSuccessAuditInfo =
    MessageAcknowledgedSuccessAuditInfo(batchId, jobId, ern, recordsAffected)

  def createMessageNotAcknowledgedEvent(
    ern: String,
    batchId: String,
    jobId: Option[String],
    failureReason: String
  ): MessageAcknowledgedFailureAuditInfo                          =
    MessageAcknowledgedFailureAuditInfo(failureReason, batchId, jobId, ern)
  private def convertErns(erns: Set[String]): NonEmptySeq[String] = NonEmptySeq(erns.head, erns.tail.toSeq)
}
