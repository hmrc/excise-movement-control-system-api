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

import generated.{IE802Type, MessagesOption}
import play.api.libs.json.Json
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE802Message
(
  private val obj: IE802Type,
  private val key: Option[String],
  private val namespace: Option[String]
) extends IEMessage with GeneratedJsonWriters{

  def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.ReminderMessageForExciseMovement.ExciseMovement.AdministrativeReferenceCode))

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE802Type](obj, namespace, key, generated.defaultScope)
  }
  override def toJson = Json.toJson(obj)

  override def messageType: String = MessageTypes.IE802.value

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def lrnEquals(lrn: String): Boolean = false
}

object IE802Message {
  def apply(message: DataRecord[MessagesOption]): IE802Message = {
    IE802Message(message.as[IE802Type], message.key, message.namespace)
  }

  def createFromXml(xml: NodeSeq): IE802Message = {
    val ie802: IE802Type = scalaxb.fromXML[IE802Type](xml)
    IE802Message(ie802, Some(ie802.productPrefix), None)
  }
}
