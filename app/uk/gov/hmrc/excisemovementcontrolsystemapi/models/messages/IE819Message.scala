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

import generated.{IE819Type, MessagesOption}
import play.api.libs.json.Json
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq


case class IE819Message
(
  private val obj: IE819Type,
  private val key: Option[String],
  private val namespace: Option[String]
) extends IEMessage with GeneratedJsonWriters {
  def consignorId: Option[String] = None

  override def consigneeId: Option[String] =
    obj.Body.AlertOrRejectionOfEADESAD.ConsigneeTrader.flatMap(_.Traderid)

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.AlertOrRejectionOfEADESAD.ExciseMovement.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE819.value

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE819Type](obj, namespace, key, generated.defaultScope)
  }
  override def toJson = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier
}

object IE819Message {
  def apply(message: DataRecord[MessagesOption]): IE819Message = {
    IE819Message(message.as[IE819Type], message.key, message.namespace)
  }

  def createFromXml(xml: NodeSeq): IE819Message = {
    val ie819: IE819Type = scalaxb.fromXML[IE819Type](xml)
    IE819Message(ie819, Some(ie819.productPrefix), None)
  }
}