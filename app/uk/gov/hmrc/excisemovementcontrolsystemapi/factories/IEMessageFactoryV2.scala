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

import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2._

import scala.xml.NodeSeq

case class IEMessageFactoryV2() extends Logging with IEMessageFactory {
  def createIEMessage(message: MessageOption): IEMessage = {
    val (messageType, record) = message match {
      case Left(m)  =>
        logger.warn(s"[IEMessageFactory] - Invalid Control flow.")
        throw new IllegalStateException("Invalid state - should not have reached.")
      case Right(m) =>
        (
          m.key.getOrElse {
            logger.warn(s"[IEMessageFactory] - Could not create Message object. Message type is empty")
            throw new IEMessageFactoryException("Could not create Message object. Message type is empty")
          },
          m
        )

    }

    MessageTypes.withValueOpt(messageType) match {
      case Some(MessageTypes.IE704) => IE704MessageV2(record)
      case Some(MessageTypes.IE801) => IE801MessageV2(record)
      case Some(MessageTypes.IE802) => IE802MessageV2(record)
      case Some(MessageTypes.IE803) => IE803MessageV2(record)
      case Some(MessageTypes.IE807) => IE807MessageV2(record)
      case Some(MessageTypes.IE810) => IE810MessageV2(record)
      case Some(MessageTypes.IE813) => IE813MessageV2(record)
      case Some(MessageTypes.IE818) => IE818MessageV2(record)
      case Some(MessageTypes.IE819) => IE819MessageV2(record)
      case Some(MessageTypes.IE829) => IE829MessageV2(record)
      case Some(MessageTypes.IE837) => IE837MessageV2(record)
      case Some(MessageTypes.IE839) => IE839MessageV2(record)
      case Some(MessageTypes.IE840) => IE840MessageV2(record)
      case Some(MessageTypes.IE871) => IE871MessageV2(record)
      case Some(MessageTypes.IE881) => IE881MessageV2(record)
      case Some(MessageTypes.IE905) => IE905MessageV2(record)
      case _                        =>
        logger.warn(s"[IEMessageFactory] - Could not create Message object. Unsupported message: $messageType")
        throw new IEMessageFactoryException(s"Could not create Message object. Unsupported message: $messageType")
    }
  }

  def createFromXml(messageType: String, xml: NodeSeq): IEMessage =
    MessageTypes.withValueOpt(messageType) match {
      case Some(MessageTypes.IE704) => IE704MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE801) => IE801MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE802) => IE802MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE803) => IE803MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE807) => IE807MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE810) => IE810MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE813) => IE813MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE815) => IE815MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE818) => IE818MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE819) => IE819MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE829) => IE829MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE837) => IE837MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE839) => IE839MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE840) => IE840MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE871) => IE871MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE881) => IE881MessageV2.createFromXml(xml)
      case Some(MessageTypes.IE905) => IE905MessageV2.createFromXml(xml)
      case _                        =>
        logger.warn(s"[IEMessageFactory] - Could not create Message object. Unsupported message: $messageType")
        throw new IEMessageFactoryException(s"Could not create Message object. Unsupported message: $messageType")
    }

}
