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

import generated.v1.MessagesOption
import play.api.Logging
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._

import scala.xml.NodeSeq

case class IEMessageFactory() extends Logging {
  def createIEMessage(message: DataRecord[MessagesOption]): IEMessage = {
    val messageType = message.key.getOrElse {
      logger.warn(s"[IEMessageFactory] - Could not create Message object. Message type is empty")
      throw new IEMessageFactoryException("Could not create Message object. Message type is empty")
    }

    MessageTypes.withValueOpt(messageType) match {
      case Some(MessageTypes.IE704) => IE704MessageV1(message)
      case Some(MessageTypes.IE801) => IE801MessageV1(message)
      case Some(MessageTypes.IE802) => IE802MessageV1(message)
      case Some(MessageTypes.IE803) => IE803MessageV1(message)
      case Some(MessageTypes.IE807) => IE807MessageV1(message)
      case Some(MessageTypes.IE810) => IE810MessageV1(message)
      case Some(MessageTypes.IE813) => IE813MessageV1(message)
      case Some(MessageTypes.IE818) => IE818MessageV1(message)
      case Some(MessageTypes.IE819) => IE819MessageV1(message)
      case Some(MessageTypes.IE829) => IE829MessageV1(message)
      case Some(MessageTypes.IE837) => IE837MessageV1(message)
      case Some(MessageTypes.IE839) => IE839MessageV1(message)
      case Some(MessageTypes.IE840) => IE840MessageV1(message)
      case Some(MessageTypes.IE871) => IE871MessageV1(message)
      case Some(MessageTypes.IE881) => IE881MessageV1(message)
      case Some(MessageTypes.IE905) => IE905MessageV1(message)
      case _                        =>
        logger.warn(s"[IEMessageFactory] - Could not create Message object. Unsupported message: $messageType")
        throw new IEMessageFactoryException(s"Could not create Message object. Unsupported message: $messageType")
    }
  }

  def createFromXml(messageType: String, xml: NodeSeq): IEMessage =
    MessageTypes.withValueOpt(messageType) match {
      case Some(MessageTypes.IE704) => IE704MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE801) => IE801MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE802) => IE802MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE803) => IE803MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE807) => IE807MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE810) => IE810MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE813) => IE813MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE815) => IE815MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE818) => IE818MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE819) => IE819MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE829) => IE829MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE837) => IE837MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE839) => IE839MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE840) => IE840MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE871) => IE871MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE881) => IE881MessageV1.createFromXml(xml)
      case Some(MessageTypes.IE905) => IE905MessageV1.createFromXml(xml)
      case _                        =>
        logger.warn(s"[IEMessageFactory] - Could not create Message object. Unsupported message: $messageType")
        throw new IEMessageFactoryException(s"Could not create Message object. Unsupported message: $messageType")
    }
}

class IEMessageFactoryException(message: String) extends RuntimeException(message)
