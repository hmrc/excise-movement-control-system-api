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

import cats.data.{NonEmptyList, NonEmptySeq}
import play.api.libs.json.{JsObject, Json}
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

  def createMessageSubmitted(
    message: IEMessage,
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: String,
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val messageSubmitted = MessageSubmittedDetails(
      message.messageType,
      message.messageAuditType.name,
      movement.localReferenceNumber,
      movement.administrativeReferenceCode,
      Some(movement._id),
      movement.consignorId,
      movement.consigneeId,
      submittedToCore,
      message.messageIdentifier,
      Some(correlationId),
      UserDetails("", "", "", "", ""),
      NonEmptySeq(request.erns.head, request.erns.tail.toList),
      message.toJsObject
    )

    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = "MessageSubmitted",
      tags = hc.toAuditTags(),
      detail = Json.toJsObject(messageSubmitted)
    )
  }
}
