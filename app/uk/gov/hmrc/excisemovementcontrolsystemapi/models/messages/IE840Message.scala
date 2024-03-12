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

import generated.{IE840Type, MessagesOption}
import play.api.libs.json.{JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType.EventReport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE840Message
(
  private val obj: IE840Type,
  private val key: Option[String],
  private val namespace: Option[String],
  auditType: AuditType
) extends IEMessage with GeneratedJsonWriters {

  override def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(obj.Body.EventReportEnvelope.ExciseMovement.map(_.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE840.value

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE840Type](obj, namespace, key, generated.defaultScope)
  }

  override def toJson: JsValue = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String = s"Message type: $messageType, message identifier: $messageIdentifier, ARC: $administrativeReferenceCode"

}

object IE840Message {
  def apply(message: DataRecord[MessagesOption]): IE840Message = {
    IE840Message(message.as[IE840Type], message.key, message.namespace, EventReport)
  }

  def createFromXml(xml: NodeSeq): IE840Message = {
    val ie840: IE840Type = scalaxb.fromXML[IE840Type](xml)
    IE840Message(ie840, Some(ie840.productPrefix), None, EventReport)
  }
}