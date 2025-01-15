/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

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

  def createMessageSubmittedNoMovement(
    message: IE815Message,
    submittedToCore: Boolean,
    correlationId: Option[String],
    userDetails: UserDetails,
    erns: Set[String]
  ): MessageSubmittedDetails =
    MessageSubmittedDetails(
      message.messageType,
      message.messageAuditType.name,
      message.localReferenceNumber,
      None,
      None,
      message.consignorId,
      message.consigneeId,
      submittedToCore,
      message.messageIdentifier,
      correlationId,
      userDetails,
      NonEmptySeq(erns.head, erns.tail.toList),
      message.toJsObject
    )

  def createMessageSubmitted(
    message: IEMessage,
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: Option[String],
    userDetails: UserDetails,
    erns: Set[String]
  ): MessageSubmittedDetails =
    MessageSubmittedDetails(
      message.messageType,
      message.messageAuditType.name,
      movement.localReferenceNumber,
      movement.administrativeReferenceCode,
      Some(movement._id),
      movement.consignorId,
      movement.consigneeId,
      submittedToCore,
      message.messageIdentifier,
      correlationId,
      userDetails,
      NonEmptySeq(erns.head, erns.tail.toList),
      message.toJsObject
    )

  def createGetMovementsDetails(
    request: GetMovementsParametersAuditInfo,
    response: GetMovementsResponseAuditInfo,
    userDetails: UserDetails,
    authExciseNumber: NonEmptySeq[String]
  ): GetMovementsAuditInfo =
    GetMovementsAuditInfo(
      request = request,
      response = response,
      userDetails = userDetails,
      authExciseNumber = authExciseNumber
    )

}
