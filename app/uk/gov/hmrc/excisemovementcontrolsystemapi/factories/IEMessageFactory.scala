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

package uk.gov.hmrc.excisemovementcontrolsystemapi.factories

import generated.MessagesOption
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._

import scala.xml.NodeSeq

case class IEMessageFactory() {
  def createIEMessage(message: DataRecord[MessagesOption]): IEMessage = {
    val messageType = message.key.getOrElse(throw new RuntimeException("Could not create Message object. Message type is empty"))

    MessageTypes.withValueOpt(messageType) match {
      case Some(MessageTypes.IE801) => IE801Message(message)
      case Some(MessageTypes.IE810) => IE810Message(message)
      case Some(MessageTypes.IE813) => IE813Message(message)
      case Some(MessageTypes.IE818) => IE818Message(message)
      case Some(MessageTypes.IE837) => IE837Message(message)
      case _ => throw new RuntimeException(s"Could not create Message object. Unsupported message: $messageType")
    }
  }

  def createFromXml(messageType: String, xml: NodeSeq): IEMessage = {
    MessageTypes.withValueOpt(messageType) match {
      case Some(MessageTypes.IE801) => IE801Message.createFromXml(xml)
      case Some(MessageTypes.IE810) => IE810Message.createFromXml(xml)
      case Some(MessageTypes.IE813) => IE813Message.createFromXml(xml)
      case Some(MessageTypes.IE815) => IE815Message.createFromXml(xml)
      case Some(MessageTypes.IE818) => IE818Message.createFromXml(xml)
      case Some(MessageTypes.IE837) => IE837Message.createFromXml(xml)
      case _ => throw new RuntimeException(s"Could not create Message object. Unsupported message: $messageType")
    }
  }
}