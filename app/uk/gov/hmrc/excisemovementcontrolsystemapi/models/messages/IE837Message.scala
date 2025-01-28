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

import generated.{IE837Type, MessagesOption}
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.Delay
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE837Message(
  obj: IE837Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with SubmitterTypeConverter
    with GeneratedJsonWriters {
  def submitter: ExciseTraderType = convertSubmitterType(
    obj.Body.ExplanationOnDelayForDelivery.AttributesValue.SubmitterType
  )

  def consignorId: Option[String] = submitter match {
    case Consignor => Some(obj.Body.ExplanationOnDelayForDelivery.AttributesValue.SubmitterIdentification)
    case _         => None
  }

  override def consigneeId: Option[String] = submitter match {
    case Consignee => Some(obj.Body.ExplanationOnDelayForDelivery.AttributesValue.SubmitterIdentification)
    case _         => None
  }

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.ExplanationOnDelayForDelivery.ExciseMovement.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE837.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE837Type](obj, namespace, key, generated.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, ARC: $administrativeReferenceCode"

  override def correlationId: Option[String] = obj.Header.CorrelationIdentifier

  override def localReferenceNumber: Option[String] = None
}

object IE837Message {
  def apply(message: DataRecord[MessagesOption]): IE837Message =
    IE837Message(message.as[IE837Type], message.key, message.namespace, Delay)

  def createFromXml(xml: NodeSeq): IE837Message = {
    val ie837: IE837Type = scalaxb.fromXML[IE837Type](xml)
    IE837Message(ie837, Some(xml.head.label), Option(xml.head.namespace), Delay)
  }
}
