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

import cats.syntax.all._
import org.apache.pekko.Done
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE704Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MovementCreator {
  def create(
    boxId: Option[String],
    localReferenceNumber: String,
    consignorId: String,
    consigneeId: Option[String],
    administrativeReferenceCode: Option[String] = None,
    lastUpdated: Instant = Instant.now,
    messages: Seq[Message] = Seq.empty
  ): Movement
}

@Singleton
class MessageService @Inject
(
  movementRepository: MovementRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  messageConnector: MessageConnector,
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils,
  movementCreator: MovementCreator = Movement.apply
)(implicit executionContext: ExecutionContext) {

  def updateMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] = {
    for {
      _ <- ernRetrievalRepository.getLastRetrieved(ern)
      messages <- messageConnector.getNewMessages(ern)
      _ <- updateMovements(ern, messages)
      _ <- ernRetrievalRepository.save(ern)
    } yield Done
  }

  private def updateMovements(ern: String, messages: Seq[IEMessage]): Future[Done] = {
    if (messages.nonEmpty) {
      movementRepository.getAllBy(ern).map { movements =>
        messages.groupBy { message =>
          lazy val arcMovement = findByArc(movements, message)
          lazy val lrnMovement = findByLrn(movements, message)
          arcMovement orElse lrnMovement
        }.toSeq.traverse { case (maybeMovement, messages) =>
          maybeMovement.map { m =>
            val updatedMovement = m.copy(messages = m.messages ++ messages.map(convertMessage))
            movementRepository.updateMovement(updatedMovement)
          }.getOrElse {
            messages.traverse {
              case ie704: IE704Message => createMovementFromIE704(ern, ie704)
            }
          }
        }
      }
    }.as(Done) else {
      Future.successful(Done)
    }
  }

  private def createMovementFromIE704(consignor: String, message: IE704Message): Future[Option[Movement]] = {
    val movement = movementCreator.create(
      None,
      message.localReferenceNumber.get,
      consignor,
      None,
      administrativeReferenceCode = message.administrativeReferenceCode.head,
      messages = Seq(convertMessage(message))
    )
    movementRepository.saveMovement(movement).as(Option(movement))
  }

  private def findByArc(movements: Seq[Movement], message: IEMessage): Option[Movement] = {
    movements.find(movement =>
      movement.administrativeReferenceCode.exists(arc =>
        message.administrativeReferenceCode.flatten.contains(arc)
      )
    )
  }

  private def findByLrn(movements: Seq[Movement], message: IEMessage): Option[Movement] = {
    movements.find(movement => message.lrnEquals(movement.localReferenceNumber))
  }

  private def convertMessage(input: IEMessage): Message = {
    Message(
      encodedMessage = emcsUtils.encode(input.toXml.toString),
      messageType = input.messageType,
      messageId = input.messageIdentifier,
      createdOn = dateTimeService.timestamp()
    )
  }
}
