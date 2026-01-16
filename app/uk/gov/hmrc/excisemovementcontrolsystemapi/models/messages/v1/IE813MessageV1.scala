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
import generated.v1.{IE813Type, MessagesOption}
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.ChangeOfDestination
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.MessageTypeFormats.GeneratedJsonWritersV1

import scala.xml.NodeSeq

case class IE813MessageV1(
  obj: IE813Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with GeneratedJsonWritersV1 {

  override def consignorId: Option[String] = None

  override def consigneeId: Option[String] =
    obj.Body.ChangeOfDestination.DestinationChanged.NewConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.ChangeOfDestination.UpdateEadEsad.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE813.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE813Type](obj, namespace, key, v1.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, ARC: $administrativeReferenceCode"

  override def correlationId: Option[String] = obj.Header.CorrelationIdentifier

  def optionalLocalReferenceNumber: Option[String] = None
}

object IE813MessageV1 {
  def apply(message: DataRecord[MessagesOption]): IE813MessageV1 =
    IE813MessageV1(message.as[IE813Type], message.key, message.namespace, ChangeOfDestination)

  def createFromXml(xml: NodeSeq): IE813MessageV1 = {
    val ie813: IE813Type = scalaxb.fromXML[IE813Type](xml)
    IE813MessageV1(ie813, Some(xml.head.label), Option(xml.head.namespace), ChangeOfDestination)
  }
}
