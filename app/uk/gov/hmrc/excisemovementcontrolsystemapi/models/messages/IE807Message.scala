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

import generated.{IE807Type, MessagesOption}
import play.api.libs.json.{JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType.InterruptionOfMovement
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE807Message(
  obj: IE807Type,
  key: Option[String],
  namespace: Option[String],
  auditType: AuditType
) extends IEMessage
    with GeneratedJsonWriters {

  def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.InterruptionOfMovement.AttributesValue.AdministrativeReferenceCode))

  override def toXml: NodeSeq =
    scalaxb.toXML[IE807Type](obj, namespace, key, generated.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def messageType: String = MessageTypes.IE807.value

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def lrnEquals(lrn: String): Boolean = false

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, ARC: $administrativeReferenceCode"

}

object IE807Message {
  def apply(message: DataRecord[MessagesOption]): IE807Message =
    IE807Message(message.as[IE807Type], message.key, message.namespace, InterruptionOfMovement)

  def createFromXml(xml: NodeSeq): IE807Message = {
    val ie807: IE807Type = scalaxb.fromXML[IE807Type](xml)
    IE807Message(ie807, Some(xml.head.label), Option(xml.head.namespace), InterruptionOfMovement)
  }
}
