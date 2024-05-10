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
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageService @Inject
(
  configuration: Configuration,
  movementRepository: MovementRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  messageConnector: MessageConnector,
  dateTimeService: DateTimeService,
  correlationIdService: CorrelationIdService,
  emcsUtils: EmcsUtils,
)(implicit executionContext: ExecutionContext) {

  private val throttleCutoff: FiniteDuration = configuration.get[FiniteDuration]("microservice.services.eis.throttle-cutoff")

  def updateMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] = {
    ernRetrievalRepository.getLastRetrieved(ern).flatMap { maybeLastRetrieved =>
      if (shouldProcessNewMessages(maybeLastRetrieved)) {
        for {
          _ <- processNewMessages(ern)
          _ <- ernRetrievalRepository.save(ern)
        } yield Done
      } else {
        Future.successful(Done)
      }
    }
  }

  private def shouldProcessNewMessages(maybeLastRetrieved: Option[Instant]): Boolean = {
    val cutoffTime = dateTimeService.timestamp().minus(throttleCutoff.length, throttleCutoff.unit.toChronoUnit)
    //noinspection MapGetOrElseBoolean
    maybeLastRetrieved.map(_.isBefore(cutoffTime)).getOrElse(true)
  }

  private def processNewMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] =
    for {
      response <- messageConnector.getNewMessages(ern)
      _ <- updateMovements(ern, response.messages)
      _ <- acknowledgeAndContinue(response, ern)
    } yield Done

  private def acknowledgeAndContinue(response: GetMessagesResponse, ern: String)(implicit hc: HeaderCarrier): Future[Done] =
    if (response.messageCount == 0) {
      Future.successful(Done)
    } else {
      messageConnector.acknowledgeMessages(ern).flatMap { _ =>
        if (response.messageCount > response.messages.size) {
          processNewMessages(ern)
        } else {
          Future.successful(Done)
        }
      }
    }

  private def updateMovements(ern: String, messages: Seq[IEMessage]): Future[Done] = {
    if (messages.nonEmpty) {
      movementRepository.getAllBy(ern).map { movements =>
        messages.foldLeft(Seq.empty[Movement]) { (updatedMovements, message) =>
          updateOrCreateMovements(ern, movements, updatedMovements, message)
        }.traverse(movementRepository.save)
      }.as(Done)
    } else {
      Future.successful(Done)
    }
  }

  private def updateOrCreateMovements(ern: String, movements: Seq[Movement], updatedMovements: Seq[Movement], message: IEMessage): Seq[Movement] = {
    val matchedMovements: Seq[Movement] = findMovementsForMessage(movements, updatedMovements, message)

    (if (matchedMovements.nonEmpty) matchedMovements.map {
        updateMovement(_, message)
      } else {
        createMovement(ern, message)
      } +: updatedMovements
      ).distinctBy(_._id)
  }

  private def updateMovement(movement: Movement, message: IEMessage): Movement = {
    movement.copy(messages = getUpdatedMessages(movement, message),
      administrativeReferenceCode = getArc(movement, message),
      consigneeId = getConsignee(movement, message)
    )
  }

  private def getUpdatedMessages(movement: Movement, message: IEMessage) = {
    (movement.messages :+ convertMessage(message)).distinctBy(_.messageId)
  }

  private def findMovementsForMessage(movements: Seq[Movement], updatedMovements: Seq[Movement], message: IEMessage): Seq[Movement] = {
    findByArc(updatedMovements, message) orElse
      findByLrn(updatedMovements, message) orElse
      findByArc(movements, message) orElse
      findByLrn(movements, message)
  }.getOrElse(Seq.empty)

  private def getConsignee(movement: Movement, message: IEMessage): Option[String] = {
    message match {
      case ie801: IE801Message => movement.consigneeId orElse ie801.consigneeId
      case ie813: IE813Message => ie813.consigneeId orElse movement.consigneeId
      case _ => movement.consigneeId
    }
  }

  private def getArc(movement: Movement, message: IEMessage): Option[String] = {
    message match {
      case ie801: IE801Message => movement.administrativeReferenceCode orElse ie801.administrativeReferenceCode.flatten.headOption
      case _ => movement.administrativeReferenceCode
    }
  }

  private def createMovement(ern: String, message: IEMessage): Movement = {
    message match {
      case ie704: IE704Message => createMovementFromIE704(ern, ie704)
      case ie801: IE801Message => createMovementFromIE801(ern, ie801)
      case _ => ???
    }
  }

  private def createMovementFromIE704(consignor: String, message: IE704Message): Movement = {
    Movement(
      correlationIdService.generateCorrelationId(),
      None,
      message.localReferenceNumber.get, // TODO remove .get
      consignor,
      None,
      administrativeReferenceCode = message.administrativeReferenceCode.head, // TODO remove .head
      dateTimeService.timestamp(),
      messages = Seq(convertMessage(message))
    )
  }

  private def createMovementFromIE801(consignor: String, message: IE801Message): Movement = {
    Movement(
      correlationIdService.generateCorrelationId(),
      None,
      message.localReferenceNumber.get, // TODO remove .get
      consignor,
      message.consigneeId,
      administrativeReferenceCode = message.administrativeReferenceCode.head, // TODO remove .head
      dateTimeService.timestamp(),
      messages = Seq(convertMessage(message))
    )
  }

  private def findByArc(movements: Seq[Movement], message: IEMessage): Option[Seq[Movement]] = {

    val matchedMovements = message.administrativeReferenceCode.flatten.flatMap { arc =>
      movements.find(_.administrativeReferenceCode.contains(arc))
    }

    if (matchedMovements.isEmpty) None else Some(matchedMovements)
  }

  private def findByLrn(movements: Seq[Movement], message: IEMessage): Option[Seq[Movement]] =
    movements.find(movement => message.lrnEquals(movement.localReferenceNumber))
      .map(Seq(_))

  private def convertMessage(input: IEMessage): Message = {
    Message(
      encodedMessage = emcsUtils.encode(input.toXml.toString),
      messageType = input.messageType,
      messageId = input.messageIdentifier,
      createdOn = dateTimeService.timestamp()
    )
  }
}
