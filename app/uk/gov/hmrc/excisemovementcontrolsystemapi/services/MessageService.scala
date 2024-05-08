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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IE704Message, IE801Message, IE813Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}
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
      _        <- updateMovements(ern, response.messages)
      _        <- acknowledgeAndContinue(response, ern)
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

          val matchedMovements: Seq[Movement] = {
            findByArc(updatedMovements, message) orElse
            findByLrn(updatedMovements, message) orElse
            findByArc(movements, message) orElse
            findByLrn(movements, message)
          }.getOrElse(Seq.empty)

          (
            if (matchedMovements.nonEmpty) {
              matchedMovements.map { movement =>
                val consignee = message match {
                  case ie801: IE801Message => movement.consigneeId orElse ie801.consigneeId
                  case ie813: IE813Message => ie813.consigneeId orElse movement.consigneeId
                  case _ => movement.consigneeId
                }
                val arc = message match {
                  case ie801: IE801Message => movement.administrativeReferenceCode orElse ie801.administrativeReferenceCode.flatten.headOption
                  case _ => movement.administrativeReferenceCode
                }
                movement.copy(messages = movement.messages :+ convertMessage(message),
                  consigneeId = consignee,
                  administrativeReferenceCode = arc
                )
                // sometimes want to do more than this. depending on which message.
              }
            } else {
              message match {
                case ie704: IE704Message => createMovementFromIE704(ern, ie704)
                case ie801: IE801Message => createMovementFromIE801(ern, ie801)
                case _ => ???
              }
            } +: updatedMovements
          ).distinctBy(_._id)
        }.traverse(movement => movementRepository.save(movement))
      }.as(Done)
    } else {
      Future.successful(Done)
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
