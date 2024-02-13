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

import generated.{IE905Type, MessagesOption}
import play.api.libs.json.Json
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters

import scala.xml.NodeSeq

case class IE905Message(
                         private val obj: IE905Type,
                         private val key: Option[String],
                         private val namespace: Option[String]
                       ) extends IEMessage with GeneratedJsonWriters{

  override def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.StatusResponse.AttributesValue.AdministrativeReferenceCode))

  override def messageType: String = MessageTypes.IE905.value

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE905Type](obj, namespace, key, generated.defaultScope)
  }
  override def toJson = Json.toJson(obj)

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = obj.Header.MessageIdentifier
}

object IE905Message {
  def apply(message: DataRecord[MessagesOption]): IE905Message = {
    IE905Message(message.as[IE905Type], message.key, message.namespace)
  }

  def createFromXml(xml: NodeSeq): IE905Message = {
    val ie905: IE905Type = scalaxb.fromXML[IE905Type](xml)
    IE905Message(ie905, Some(ie905.productPrefix), None)
  }
}