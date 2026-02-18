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
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.EventReport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE840Message(
  obj: IE840Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with GeneratedJsonWriters {

  override def consigneeId: Option[String] = None
  override def consignorId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(obj.Body.EventReportEnvelope.ExciseMovement.map(_.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE840.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE840Type](obj, namespace, key, generated.defaultScope)

  override def toJson: JsValue      = Json.toJson(obj)
  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, ARC: $administrativeReferenceCode"

  override def correlationId: Option[String] = obj.Header.CorrelationIdentifier

  override def optionalLocalReferenceNumber: Option[String] = None
}

object IE840Message {
  def apply(message: DataRecord[MessagesOption]): IE840Message =
    IE840Message(message.as[IE840Type], message.key, message.namespace, EventReport)

  def createFromXml(xml: NodeSeq): IE840Message = {
    val ie840: IE840Type = scalaxb.fromXML[IE840Type](xml)
    IE840Message(ie840, Some(xml.head.label), Option(xml.head.namespace), EventReport)
  }
}
