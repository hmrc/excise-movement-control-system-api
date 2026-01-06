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
import generated.v1.{IE819Type, MessagesOption}
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.AlertRejection
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.MessageTypeFormats.GeneratedJsonWritersV1

import scala.xml.NodeSeq

case class IE819MessageV1(
  obj: IE819Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with GeneratedJsonWritersV1 {
  override def consignorId: Option[String] = None

  override def consigneeId: Option[String] =
    obj.Body.AlertOrRejectionOfEADESAD.ConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.AlertOrRejectionOfEADESAD.ExciseMovement.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE819.value

  override def toXml: NodeSeq  =
    scalaxb.toXML[IE819Type](obj, namespace, key, v1.defaultScope)
  override def toJson: JsValue = Json.toJson(obj)

  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, ARC: $administrativeReferenceCode"

  override def correlationId: Option[String] = obj.Header.CorrelationIdentifier

  def optionalLocalReferenceNumber: Option[String] = None
}

object IE819MessageV1 {
  def apply(message: DataRecord[MessagesOption]): IE819MessageV1 =
    IE819MessageV1(message.as[IE819Type], message.key, message.namespace, AlertRejection)

  def createFromXml(xml: NodeSeq): IE819MessageV1 = {
    val ie819: IE819Type = scalaxb.fromXML[IE819Type](xml)
    IE819MessageV1(ie819, Some(xml.head.label), Option(xml.head.namespace), AlertRejection)
  }
}
