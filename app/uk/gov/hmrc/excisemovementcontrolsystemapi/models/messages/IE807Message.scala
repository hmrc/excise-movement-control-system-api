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

import generated.{IE807Type, MessagesOption}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes

import scala.xml.NodeSeq

case class IE807Message
(
  private val obj: IE807Type,
  private val key: Option[String],
  private val namespace: Option[String]
) extends IEMessage {

  def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] =
    Seq(Some(obj.Body.InterruptionOfMovement.AttributesValue.AdministrativeReferenceCode))

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE807Type](obj, namespace, key, generated.defaultScope)
  }

  override def messageType: String = MessageTypes.IE807.value

  override def messageIdentifier: String = obj.Header.MessageIdentifier

  override def lrnEquals(lrn: String): Boolean = false
}

object IE807Message {
  def apply(message: DataRecord[MessagesOption]): IE807Message = {
    IE807Message(message.as[IE807Type], message.key, message.namespace)
  }

  def createFromXml(xml: NodeSeq): IE807Message = {
    val ie807: IE807Type = scalaxb.fromXML[IE807Type](xml)
    IE807Message(ie807, Some(ie807.productPrefix), None)
  }
}
