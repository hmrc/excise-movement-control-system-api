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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2

import generated.v2
import generated.v2.{IE801Type, MessagesOption}
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.MovementGenerated
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2.MessageTypeFormats.GeneratedJsonWritersV2

import scala.xml.NodeSeq

case class IE801MessageV2(
  obj: IE801Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with GeneratedJsonWritersV2 {
  def localReferenceNumber: String =
    obj.Body.EADESADContainer.EadEsad.LocalReferenceNumber

  def optionalLocalReferenceNumber: Option[String] = Some(obj.Body.EADESADContainer.EadEsad.LocalReferenceNumber)

  override def consignorId: Option[String] =
    Some(obj.Body.EADESADContainer.ConsignorTrader.TraderExciseNumber)

  override def consigneeId: Option[String] =
    obj.Body.EADESADContainer.ConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.EADESADContainer.ExciseMovement.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE801.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE801Type](obj, namespace, key, v2.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean = localReferenceNumber == lrn

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, LRN: $localReferenceNumber, ARC: $administrativeReferenceCode"

  override def correlationId: Option[String] = obj.Header.CorrelationIdentifier
}

object IE801MessageV2 {
  def apply(message: DataRecord[MessagesOption]): IE801MessageV2 =
    IE801MessageV2(message.as[IE801Type], message.key, message.namespace, MovementGenerated)

  def createFromXml(xml: NodeSeq): IE801MessageV2 = {
    val ie801: IE801Type = scalaxb.fromXML[IE801Type](xml)
    IE801MessageV2(ie801, Some(xml.head.label), Option(xml.head.namespace), MovementGenerated)
  }
}
