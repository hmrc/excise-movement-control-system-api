/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1

import generated.v1
import generated.v1.IE815Type
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.DraftMovement
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.MessageTypeFormats.GeneratedJsonWritersV1

import scala.xml.NodeSeq

case class IE815MessageV1(obj: IE815Type, messageAuditType: MessageAuditType)
    extends IEMessage
    with GeneratedJsonWritersV1 {
  def localReferenceNumber: String =
    obj.Body.SubmittedDraftOfEADESAD.EadEsadDraft.LocalReferenceNumber

  def optionalLocalReferenceNumber: Option[String] = Some(
    obj.Body.SubmittedDraftOfEADESAD.EadEsadDraft.LocalReferenceNumber
  )

  override def consignorId: Option[String] =
    Some(obj.Body.SubmittedDraftOfEADESAD.ConsignorTrader.TraderExciseNumber)

  override def consigneeId: Option[String] =
    obj.Body.SubmittedDraftOfEADESAD.ConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Seq[Option[String]] = Seq(None)

  override def messageType: String = MessageTypes.IE815.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE815Type](obj, MessageTypes.IE815.value, v1.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean = localReferenceNumber.equals(lrn)

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, LRN: $localReferenceNumber"
  def correlationId             = obj.Header.CorrelationIdentifier
}

object IE815MessageV1 {
  def createFromXml(xml: NodeSeq): IE815MessageV1 =
    IE815MessageV1(scalaxb.fromXML[IE815Type](xml), DraftMovement)

}
