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

import generated.{IE829Type, MessagesOption}
import play.api.libs.json.{JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType.NotificationOfAcceptedExport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq


case class IE829Message
(
  private val obj: IE829Type,
  private val key: Option[String],
  private val namespace: Option[String],
  auditType: AuditType
) extends IEMessage with GeneratedJsonWriters {

  override def consigneeId: Option[String] =
    obj.Body.NotificationOfAcceptedExport.ConsigneeTrader.Traderid

  override def administrativeReferenceCode: Seq[Option[String]] = {
    obj.Body.NotificationOfAcceptedExport.ExciseMovementEad.map(x => Some(x.AdministrativeReferenceCode))
  }

  override def messageType: String = MessageTypes.IE829.value

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE829Type](obj, namespace, key, generated.defaultScope)
  }

  override def toJson: JsValue = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String = s"Message type: $messageType, message identifier: $messageIdentifier, ARCs: $administrativeReferenceCode"

}

object IE829Message {
  def apply(message: DataRecord[MessagesOption]): IE829Message = {
    IE829Message(message.as[IE829Type], message.key, message.namespace, NotificationOfAcceptedExport)
  }

  def createFromXml(xml: NodeSeq): IE829Message = {
    val ie829: IE829Type = scalaxb.fromXML[IE829Type](xml)
    IE829Message(ie829, Some(ie829.productPrefix), None, NotificationOfAcceptedExport)
  }
}