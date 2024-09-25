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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages

import generated.{IE801Type, MessagesOption}
import play.api.libs.json.{JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType.MovementGenerated
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE801Message(
  private val obj: IE801Type,
  key: Option[String],
  namespace: Option[String],
  auditType: AuditType
) extends IEMessage
    with GeneratedJsonWriters {
  def localReferenceNumber: String =
    obj.Body.EADESADContainer.EadEsad.LocalReferenceNumber

  def consignorId: String =
    obj.Body.EADESADContainer.ConsignorTrader.TraderExciseNumber

  override def consigneeId: Option[String] =
    obj.Body.EADESADContainer.ConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.EADESADContainer.ExciseMovement.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE801.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE801Type](obj, namespace, key, generated.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = localReferenceNumber == lrn

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, LRN: $localReferenceNumber, ARC: $administrativeReferenceCode"

}

object IE801Message {
  def apply(message: DataRecord[MessagesOption]): IE801Message =
    IE801Message(message.as[IE801Type], message.key, message.namespace, MovementGenerated)

  def createFromXml(xml: NodeSeq): IE801Message = {
    val ie801: IE801Type = scalaxb.fromXML[IE801Type](xml)
    IE801Message(ie801, Some(xml.head.label), Option(xml.head.namespace), MovementGenerated)
  }
}
