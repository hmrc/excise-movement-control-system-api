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

import generated.{IE801Type, IE818Type, MessagesOption}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes

import scala.xml.NodeSeq


case class IE818Message
(
  private val obj: IE818Type,
  private val key: Option[String],
  private val namespace: Option[String]
) extends IEMessage {
  override def localReferenceNumber: Option[String] = None

  override def consignorId: Option[String] = None

  override def consigneeId: Option[String] =
    obj.Body.AcceptedOrRejectedReportOfReceiptExport.ConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Option[String] =
    Some(obj.Body.AcceptedOrRejectedReportOfReceiptExport.ExciseMovement.AdministrativeReferenceCode)

  override def messageType: String = MessageTypes.IE818.value

  override def toXml: NodeSeq = {
    val ns: String = namespace.fold(generated.defaultScope.uri)(o => o)
    scalaxb.toXML[IE818Type](obj, None, key, scalaxb.toScope(key -> ns))
  }
}

object IE818Message {
  def apply(message: DataRecord[MessagesOption]): IE818Message = {
    IE818Message(message.as[IE818Type], message.key, message.namespace)
  }

  def createFromXml(xml: NodeSeq): IE818Message = {
    val ie818: IE818Type = scalaxb.fromXML[IE818Type](xml)
    IE818Message(ie818, Some(ie818.productPrefix), None)
  }
}