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

import play.api.mvc.AnyContent
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.IE815MessageV1
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2.IE815MessageV2
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.xml.NodeSeq

trait AuditEventFactory {

  def createMessageAuditEvent(input: IEMessage, failureOpt: Option[String])(implicit
    hc: HeaderCarrier
  ): ExtendedDataEvent

  def createMessageSubmittedNoMovement(
    message: IE815MessageV1,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  ): MessageSubmittedDetails

  def createMessageSubmittedNoMovement(
    message: IE815MessageV2,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  ): MessageSubmittedDetails

  def createMessageSubmitted(
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  ): MessageSubmittedDetails

  def createGetMovementsDetails(
    movementFilter: MovementFilter,
    movements: Seq[Movement],
    request: EnrolmentRequest[AnyContent]
  ): GetMovementsAuditInfo

  def createGetSpecificMovementDetails(
    movementId: String,
    request: EnrolmentRequest[AnyContent]
  ): GetSpecificMovementAuditInfo

  def createGetMessagesAuditInfo(
    messages: Seq[Message],
    movement: Movement,
    updatedSince: Option[String],
    traderType: Option[String],
    request: EnrolmentRequest[AnyContent]
  ): GetMessagesAuditInfo

  def createGetSpecificMessageAuditInfo(
    movement: Movement,
    message: Message,
    request: EnrolmentRequest[AnyContent]
  ): GetSpecificMessageAuditInfo

  def createMessageProcessingSuccessAuditInfo(
    ern: String,
    response: GetMessagesResponse,
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingSuccessAuditInfo

  def createMessageProcessingFailureAuditInfo(
    ern: String,
    failureReason: String,
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingFailureAuditInfo

  def createMovementSavedSuccessAuditInfo(
    updatedMovement: Movement,
    batchId: Option[String],
    jobId: Option[String],
    newMessages: Seq[Message]
  ): MovementSavedSuccessAuditInfo

  def createMovementSavedFailureAuditInfo(
    movement: Movement,
    failureReason: String,
    batchId: Option[String],
    jobId: Option[String],
    messagesToBeAdded: Seq[Message]
  ): MovementSavedFailureAuditInfo

  def createMessageAcknowledgedEvent(
    ern: String,
    batchId: String,
    jobId: Option[String],
    recordsAffected: Int
  ): MessageAcknowledgedSuccessAuditInfo

  def createMessageNotAcknowledgedEvent(
    ern: String,
    batchId: String,
    jobId: Option[String],
    failureReason: String
  ): MessageAcknowledgedFailureAuditInfo

}
