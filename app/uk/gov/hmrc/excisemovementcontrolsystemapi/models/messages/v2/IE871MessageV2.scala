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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2

import generated.v2
import generated.v2.{IE871Type, MessagesOption}
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType.ShortageOrExcess
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{ExciseTraderType, IEMessage, SubmitterTypeConverter}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2.MessageTypeFormats.GeneratedJsonWritersV2

import scala.xml.NodeSeq

case class IE871MessageV2(
  obj: IE871Type,
  key: Option[String],
  namespace: Option[String],
  messageAuditType: MessageAuditType
) extends IEMessage
    with SubmitterTypeConverter
    with GeneratedJsonWritersV2 {
  def submitter: ExciseTraderType                  = convertSubmitterTypeV2(
    obj.Body.ExplanationOnReasonForShortage.AttributesValue.SubmitterType
  )
  def optionalLocalReferenceNumber: Option[String] = None

  override def consignorId: Option[String] = Some(
    obj.Body.ExplanationOnReasonForShortage.ConsignorTrader.map(_.TraderExciseNumber)
  ).flatten

  override def consigneeId: Option[String] = obj.Body.ExplanationOnReasonForShortage.ConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.ExplanationOnReasonForShortage.ExciseMovement.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE871.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE871Type](obj, namespace, key, v2.defaultScope)

  override def toJson: JsValue = Json.toJson(obj)

  override def toJsObject: JsObject = Json.toJsObject(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toString: String =
    s"Message type: $messageType, message identifier: $messageIdentifier, ARC: $administrativeReferenceCode"

  override def correlationId: Option[String] = obj.Header.CorrelationIdentifier
}

object IE871MessageV2 {
  def apply(message: DataRecord[MessagesOption]): IE871MessageV2 =
    IE871MessageV2(message.as[IE871Type], message.key, message.namespace, ShortageOrExcess)

  def createFromXml(xml: NodeSeq): IE871MessageV2 = {
    val ie871: IE871Type = scalaxb.fromXML[IE871Type](xml)
    IE871MessageV2(ie871, Some(xml.head.label), Option(xml.head.namespace), ShortageOrExcess)
  }
}
