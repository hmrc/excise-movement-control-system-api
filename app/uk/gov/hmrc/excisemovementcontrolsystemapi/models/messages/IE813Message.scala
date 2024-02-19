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

import generated.{IE813Type, MessagesOption}
import play.api.libs.json.Json
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType.ChangeOfDestination
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE813Message
(
  private val obj: IE813Type,
  private val key: Option[String],
  private val namespace: Option[String],
  auditType: AuditType
) extends IEMessage with GeneratedJsonWriters  {

  def consignorId: Option[String] = None

  override def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.ChangeOfDestination.UpdateEadEsad.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE813.value

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE813Type](obj, namespace, key, generated.defaultScope)
  }
  override def toJson = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier
}

object IE813Message {
  def apply(message: DataRecord[MessagesOption]): IE813Message = {
    IE813Message(message.as[IE813Type], message.key, message.namespace, ChangeOfDestination)
  }

  def createFromXml(xml: NodeSeq): IE813Message = {
    val ie813: IE813Type = scalaxb.fromXML[IE813Type](xml)
    IE813Message(ie813, Some(ie813.productPrefix), None, ChangeOfDestination)
  }
}