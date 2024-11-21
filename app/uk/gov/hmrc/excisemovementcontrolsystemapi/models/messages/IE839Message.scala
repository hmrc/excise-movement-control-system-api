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

import generated.{IE839Type, MessagesOption}
import play.api.libs.json.{JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.RefusalByCustoms
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE839Message(
  obj: IE839Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with GeneratedJsonWriters {

  def localReferenceNumber: Option[String] =
    obj.Body.RefusalByCustoms.NEadSub.map(_.LocalReferenceNumber)

  override def consigneeId: Option[String] =
    obj.Body.RefusalByCustoms.ConsigneeTrader.Traderid

  override def administrativeReferenceCode: Seq[Option[String]] =
    obj.Body.RefusalByCustoms.CEadVal.map(x => Some(x.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE839.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE839Type](obj, namespace, key, generated.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = localReferenceNumber.contains(lrn)

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, LRN: $localReferenceNumber, ARCs: $administrativeReferenceCode"

}

object IE839Message {
  def apply(message: DataRecord[MessagesOption]): IE839Message =
    IE839Message(message.as[IE839Type], message.key, message.namespace, RefusalByCustoms)

  def createFromXml(xml: NodeSeq): IE839Message = {
    val ie839: IE839Type = scalaxb.fromXML[IE839Type](xml)
    IE839Message(ie839, Some(xml.head.label), Option(xml.head.namespace), RefusalByCustoms)
  }
}
