/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.XML

@Singleton
class MiscodedMovementService @Inject() (
  movementRepository: MovementRepository,
  emcsUtils: EmcsUtils,
  messageFactory: IEMessageFactory
)(implicit ec: ExecutionContext)
    extends Logging {

  def recodeMessages(movementId: String): Future[Done] =
    movementRepository.getMovementById(movementId).flatMap {
      _.map { movement =>
        val updatedMessages = movement.messages.map(recodeMessage)
        movementRepository.save(movement.copy(messages = updatedMessages))
      }.getOrElse {
        logger.warn(s"No movement found for id: $movementId")
        Future.successful(Done)
      }
    }

  private def recodeMessage(message: Message): Message = {

    val xml = XML.loadString(emcsUtils.decode(message.encodedMessage))

    val updatedMessage = xml.headOption.flatMap { node =>
      if (node.label.endsWith("Type")) {
        messageFactory.createFromXml(message.messageType, xml) match {
          case ie704: IE704Message =>
            Some(
              scalaxb.toXML(
                ie704.obj,
                Some("http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3"),
                Some(IE704.value),
                generated.defaultScope
              )
            )
          case ie801: IE801Message =>
            Some(
              scalaxb.toXML(
                ie801.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.13"),
                Some(IE801.value),
                generated.defaultScope
              )
            )
          case ie802: IE802Message =>
            Some(
              scalaxb.toXML(
                ie802.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.13"),
                Some(IE802.value),
                generated.defaultScope
              )
            )
          case ie803: IE803Message =>
            Some(
              scalaxb.toXML(
                ie803.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE803:V3.13"),
                Some(IE803.value),
                generated.defaultScope
              )
            )
          case ie807: IE807Message =>
            Some(
              scalaxb.toXML(
                ie807.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE807:V3.13"),
                Some(IE807.value),
                generated.defaultScope
              )
            )
          case ie810: IE810Message =>
            Some(
              scalaxb.toXML(
                ie810.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.13"),
                Some(IE810.value),
                generated.defaultScope
              )
            )
          case ie813: IE813Message =>
            Some(
              scalaxb.toXML(
                ie813.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE813:V3.13"),
                Some(IE813.value),
                generated.defaultScope
              )
            )
          case ie818: IE818Message =>
            Some(
              scalaxb.toXML(
                ie818.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.13"),
                Some(IE818.value),
                generated.defaultScope
              )
            )
          case ie819: IE819Message =>
            Some(
              scalaxb.toXML(
                ie819.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE819:V3.13"),
                Some(IE819.value),
                generated.defaultScope
              )
            )
          case ie829: IE829Message =>
            Some(
              scalaxb.toXML(
                ie829.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13"),
                Some(IE829.value),
                generated.defaultScope
              )
            )
          case ie837: IE837Message =>
            Some(
              scalaxb.toXML(
                ie837.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.13"),
                Some(IE837.value),
                generated.defaultScope
              )
            )
          case ie839: IE839Message =>
            Some(
              scalaxb.toXML(
                ie839.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.13"),
                Some(IE839.value),
                generated.defaultScope
              )
            )
          case ie840: IE840Message =>
            Some(
              scalaxb.toXML(
                ie840.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE840:V3.13"),
                Some(IE840.value),
                generated.defaultScope
              )
            )
          case ie871: IE871Message =>
            Some(
              scalaxb.toXML(
                ie871.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE871:V3.13"),
                Some(IE871.value),
                generated.defaultScope
              )
            )
          case ie881: IE881Message =>
            Some(
              scalaxb.toXML(
                ie881.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE881:V3.13"),
                Some(IE881.value),
                generated.defaultScope
              )
            )
          case ie905: IE905Message =>
            Some(
              scalaxb.toXML(
                ie905.obj,
                Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE905:V3.13"),
                Some(IE905.value),
                generated.defaultScope
              )
            )
          case _                   => None
        }
      } else {
        None
      }
    }

    updatedMessage
      .map { updatedMessage =>
        val recodedMessage = emcsUtils.encode(updatedMessage.toString)
        message.copy(encodedMessage = recodedMessage, hash = recodedMessage.hashCode)
      }
      .getOrElse(message)
  }
}
