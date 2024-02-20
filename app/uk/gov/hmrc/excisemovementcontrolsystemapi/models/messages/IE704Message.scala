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
import generated.{IE704Type, MessagesOption}
import play.api.libs.json.Json
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType.Refused
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE704Message
(
  private val obj: IE704Type,
  private val key: Option[String],
  private val namespace: Option[String],
  auditType: AuditType
) extends IEMessage with GeneratedJsonWriters {
  override def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(for {
      attribute <- obj.Body.GenericRefusalMessage.AttributesValue
      arc <- attribute.AdministrativeReferenceCode
    } yield arc)

  override def messageType: String = MessageTypes.IE704.value

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def toXml: NodeSeq =
    scalaxb.toXML[IE704Type](obj, namespace, key, generated.defaultScope)
  override def toJson = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = {
    (for {
      attribute <- obj.Body.GenericRefusalMessage.AttributesValue
      messageLrn <- attribute.LocalReferenceNumber
    } yield messageLrn).contains(lrn)
  }
}

object IE704Message {
  def apply(message: DataRecord[MessagesOption]): IE704Message = {
    IE704Message(message.as[IE704Type], message.key, message.namespace, Refused)
  }

  def createFromXml(xml: NodeSeq): IE704Message = {
    val ie704: IE704Type = scalaxb.fromXML[IE704Type](xml)
    IE704Message(ie704, Some(ie704.productPrefix), None, Refused)
  }
}
