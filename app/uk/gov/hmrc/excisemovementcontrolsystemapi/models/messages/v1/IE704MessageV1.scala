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

import generated.v1
import generated.v1.{IE704Type, MessagesOption}
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.Refused
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.MessageTypeFormats.GeneratedJsonWritersV1

import scala.xml.NodeSeq

case class IE704MessageV1(
  obj: IE704Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with GeneratedJsonWritersV1 {
  override def consigneeId: Option[String] = None
  override def consignorId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(for {
      attribute <- obj.Body.GenericRefusalMessage.AttributesValue
      arc       <- attribute.AdministrativeReferenceCode
    } yield arc)

  override def messageType: String = MessageTypes.IE704.value

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toXml: NodeSeq =
    scalaxb.toXML[IE704Type](obj, namespace, key, v1.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean =
    localReferenceNumber.contains(lrn)

  def localReferenceNumber: Option[String] =
    for {
      attribute  <- obj.Body.GenericRefusalMessage.AttributesValue
      messageLrn <- attribute.LocalReferenceNumber
    } yield messageLrn

  def optionalLocalReferenceNumber: Option[String] = localReferenceNumber

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, LRN: $localReferenceNumber, ARC: $administrativeReferenceCode"

  def correlationId = obj.Header.CorrelationIdentifier
}

object IE704MessageV1 {
  def apply(message: DataRecord[MessagesOption]): IE704MessageV1 =
    IE704MessageV1(message.as[IE704Type], message.key, message.namespace, Refused)

  def createFromXml(xml: NodeSeq): IE704MessageV1 = {
    val ie704: IE704Type = scalaxb.fromXML[IE704Type](xml)
    IE704MessageV1(ie704, Some(xml.head.label), Option(xml.head.namespace), Refused)
  }
}
