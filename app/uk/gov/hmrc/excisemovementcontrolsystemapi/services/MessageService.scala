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

import cats.data.OptionT
import cats.syntax.all._
import com.codahale.metrics.MetricRegistry
import org.apache.pekko.Done
import org.bson.BsonMaximumSizeExceededException
import org.mongodb.scala.MongoCommandException
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageConnector, TraderMovementConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService.{EnrichedError, UpdateOutcome}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class MessageService @Inject() (
  configuration: Configuration,
  movementRepository: MovementRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  boxIdRepository: BoxIdRepository,
  messageConnector: MessageConnector,
  traderMovementConnector: TraderMovementConnector,
  dateTimeService: DateTimeService,
  correlationIdService: CorrelationIdService,
  emcsUtils: EmcsUtils,
  auditService: AuditService,
  mongoLockRepository: MongoLockRepository,
  metricRegistry: MetricRegistry
)(implicit executionContext: ExecutionContext)
    extends Logging {

  private val messageSizes = metricRegistry.histogram("message-size")
  private val messageCount = metricRegistry.histogram("message-count")
  private val totalMessageSize = metricRegistry.histogram("total-message-size")

  private val throttleCutoff: FiniteDuration =
    configuration.get[FiniteDuration]("microservice.services.eis.throttle-cutoff")

  private val migrateLastUpdatedCutoff: Instant =
    Instant.parse(configuration.get[String]("migrateLastUpdatedCutoff"))

  def updateAllMessages(erns: Set[String])(implicit hc: HeaderCarrier): Future[Done] =
    erns.toSeq
      .traverse { ern =>
        ernRetrievalRepository.getLastRetrieved(ern).flatMap { lastRetrieved =>
          updateMessages(ern, lastRetrieved).recover { case NonFatal(error) =>
            logger.warn(s"[MessageService]: Failed to update messages", error)
            Done
          }
        }
      }
      .as(Done)

  def updateMessages(ern: String, lastRetrieved: Option[Instant])(implicit hc: HeaderCarrier): Future[UpdateOutcome] = {
    val lockService = LockService(mongoLockRepository, ern, throttleCutoff)
    lockService
      .withLock {
        val now = dateTimeService.timestamp()
        // Temporary migration code to update movement's lastUpdated
        if (shouldMigrateLastUpdated(lastRetrieved)) {
          movementRepository.migrateLastUpdated(ern).recover { case NonFatal(error) =>
            logger.warn(s"[MessageService]: Failed to migrate lastUpdated", error)
            Done
          }
        }
        if (shouldProcessNewMessages(lastRetrieved, now)) {
          for {
            _ <- processNewMessages(ern)
            _ <- ernRetrievalRepository.setLastRetrieved(ern, now)
          } yield UpdateOutcome.Updated
        } else {
          Future.successful(UpdateOutcome.NotUpdatedThrottled)
        }
      }
      .map(_.getOrElse(UpdateOutcome.Locked))
  }

  private def shouldMigrateLastUpdated(maybeLastRetrieved: Option[Instant]): Boolean =
    //noinspection MapGetOrElseBoolean
    maybeLastRetrieved.map(_.isBefore(migrateLastUpdatedCutoff)).getOrElse(false)

  private def shouldProcessNewMessages(maybeLastRetrieved: Option[Instant], now: Instant): Boolean = {
    val cutoffTime = now.minus(throttleCutoff.length, throttleCutoff.unit.toChronoUnit)
    //noinspection MapGetOrElseBoolean
    maybeLastRetrieved.map(_.isBefore(cutoffTime)).getOrElse(true)
  }

  private def getBoxIds(ern: String): Future[Set[String]] =
    boxIdRepository.getBoxIds(ern)

  private def processNewMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] = {
    logger.info(s"[MessageService]: Processing new messages")
    for {
      response <- messageConnector.getNewMessages(ern)
      _        <- updateMovements(ern, response.messages)
      _        <- acknowledgeAndContinue(response, ern)
    } yield Done
  }

  private def acknowledgeAndContinue(response: GetMessagesResponse, ern: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
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

  private def updateMovements(ern: String, messages: Seq[IEMessage])(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    logger.info(s"[MessageService]: Updating movements")
    if (messages.nonEmpty) {
      movementRepository
        .getAllBy(ern)
        .flatMap { movements =>
          messages
            .foldLeft(Future.successful(Seq.empty[Movement])) { (accumulated, message) =>
              for {
                accumulatedMovements <- accumulated
                updatedMovements     <- updateOrCreateMovements(ern, movements, accumulatedMovements, message)
              } yield (updatedMovements ++ accumulatedMovements)
                .distinctBy { movement =>
                  (movement.localReferenceNumber, movement.consignorId, movement.administrativeReferenceCode)
                }
            }
            .flatMap {
              _.traverse { movement =>
                messageCount.update(movement.messages.length)
                totalMessageSize.update(movement.messages.map(_.encodedMessage.length).sum)
                movementRepository.save(movement).recoverWith { case NonFatal(e) =>
                  createEnrichedError(e, ern, movements, movement)
                }
              }
            }
        }
        .as(Done)
    } else {
      Future.successful(Done)
    }
  }

  private def updateOrCreateMovements(
    ern: String,
    movements: Seq[Movement],
    updatedMovements: Seq[Movement],
    message: IEMessage
  )(implicit hc: HeaderCarrier): Future[Seq[Movement]] =
    findMovementsForMessage(movements, updatedMovements, message).flatMap { matchedMovements =>
      if (matchedMovements.nonEmpty) {
        matchedMovements.traverse { movement =>
          updateMovement(movement, ern, message)
        }
      } else {
        createMovements(ern, message, movements, updatedMovements)
          .map(movement => (movement +: updatedMovements.map(Some(_))).flatten)
      }
    }

  private def updateMovement(movement: Movement, recipient: String, message: IEMessage): Future[Movement] =
    if (
      movement.messages.exists(m =>
        m.messageId == message.messageIdentifier
          && m.recipient.equalsIgnoreCase(recipient)
      )
    ) {
      Future.successful(movement)
    } else {
      val timestamp = dateTimeService.timestamp()
      getUpdatedMessages(recipient, movement, message, timestamp).map { updatedMessages =>
        movement.copy(
          messages = updatedMessages,
          administrativeReferenceCode = getArc(movement, message),
          consigneeId = getConsignee(movement, message),
          lastUpdated = timestamp
        )
      }
    }

  private def updateMovement(movement: Movement, messages: Seq[(String, IEMessage)]): Future[Movement] =
    messages.foldLeft(Future.successful(movement)) { case (movement, (recipient, message)) =>
      movement.flatMap(updateMovement(_, recipient, message))
    }

  private def getUpdatedMessages(
    recipient: String,
    movement: Movement,
    message: IEMessage,
    timestamp: Instant
  ): Future[Seq[Message]] =
    message match {
      case ie801: IE801Message =>
        Seq(
          Some(convertMessage(ie801.consignorId, ie801, timestamp)),
          ie801.consigneeId.map(convertMessage(_, ie801, timestamp))
        ).flatten.sequence.map { messages =>
          val newMessages = messages.filterNot { m1 =>
            movement.messages.exists { m2 =>
              m1.messageId == m2.messageId &&
              m1.recipient == m2.recipient
            }
          }
          movement.messages ++ newMessages
        }
      case _                   =>
        convertMessage(recipient, message, timestamp).map(movement.messages :+ _)
    }

  private def findMovementsForMessage(
    movements: Seq[Movement],
    updatedMovements: Seq[Movement],
    message: IEMessage
  ): Future[Seq[Movement]] =
    (findByArc(updatedMovements, message) orElse
      findByLrn(updatedMovements, message) orElse
      findByArc(movements, message) orElse
      findByLrn(movements, message) orElse
      findByArcInMessage(message)).getOrElse(Seq.empty)

  private def getConsignee(movement: Movement, message: IEMessage): Option[String] =
    message match {
      case ie801: IE801Message => ie801.consigneeId
      case ie813: IE813Message => ie813.consigneeId orElse movement.consigneeId
      case _                   => movement.consigneeId
    }

  private def getArc(movement: Movement, message: IEMessage): Option[String] =
    message match {
      case ie801: IE801Message =>
        movement.administrativeReferenceCode orElse ie801.administrativeReferenceCode.flatten.headOption
      case _                   => movement.administrativeReferenceCode
    }

  private def createMovements(
    ern: String,
    message: IEMessage,
    movements: Seq[Movement],
    updatedMovements: Seq[Movement]
  )(implicit
    hc: HeaderCarrier
  ): Future[Seq[Movement]] =
    message match {
      case ie704: IE704Message if ie704.localReferenceNumber.isDefined =>
        createMovementFromIE704(ern, ie704)
      case ie801: IE801Message                                         =>
        createMovementFromIE801(ie801).map(Seq(_))
      case _                                                           =>
        createMovementsFromTraderMovement(ern, message, movements, updatedMovements)
    }

  private def createMovementsFromTraderMovement(
    ern: String,
    message: IEMessage,
    movements: Seq[Movement],
    updatedMovements: Seq[Movement]
  )(implicit
    hc: HeaderCarrier
  ): Future[Seq[Movement]] =
    message.administrativeReferenceCode.flatten.headOption
      .map { arc =>
        traderMovementConnector.getMovementMessages(ern, arc).flatMap { messages =>
          buildMovementFromTraderMovement(messages, message, ern, movements, updatedMovements)
        }
      }
      .getOrElse {
        // Auditing here because we only want to audit on the message we've picked up rather than the messages from `getMovementMessages`
        auditMessageForNoMovement(message)
        Future.successful(Seq.empty)
      }

  private def buildMovementFromTraderMovement(
    messages: Seq[IEMessage],
    originatingMessage: IEMessage,
    originatingErn: String,
    movements: Seq[Movement],
    updatedMovements: Seq[Movement]
  )(implicit
    hc: HeaderCarrier
  ): Future[Seq[Movement]] =
    messages
      .collectFirst { case ie801: IE801Message =>
        findMovementsForMessage(movements, updatedMovements, ie801).flatMap { movements =>
          if (movements.nonEmpty) {
            movements.traverse { movement =>
              val messagesToAdd = Seq(
                Some(ie801.consignorId -> ie801),
                ie801.consigneeId.map(consignee => consignee -> ie801),
                Some(originatingErn    -> originatingMessage)
              ).flatten

              updateMovement(movement, messagesToAdd)
            }
          } else {

            // For a new movement from trader-movement call, add the IE801 for the consignor and consignee
            // also add the originating message all at once
            val timestamp = dateTimeService.timestamp()
            Seq(
              Some(convertMessage(ie801.consignorId, ie801, timestamp)),
              ie801.consigneeId.map(convertMessage(_, ie801, timestamp)),
              Some(convertMessage(originatingErn, originatingMessage, timestamp))
            ).flatten.sequence.map { messagesToAdd =>
              Seq(
                Movement(
                  correlationIdService.generateCorrelationId(),
                  None,
                  ie801.localReferenceNumber,
                  ie801.consignorId,
                  ie801.consigneeId,
                  administrativeReferenceCode = ie801.administrativeReferenceCode.head,
                  timestamp,
                  messages = messagesToAdd
                )
              )
            }
          }
        }
      }
      .getOrElse {
        auditMessageForNoMovement(originatingMessage)
        Future.successful(Seq.empty)
      }

  private def auditMessageForNoMovement(message: IEMessage)(implicit
    hc: HeaderCarrier
  ): Unit = {
    val errorMessage =
      s"An ${message.messageType} message has been retrieved with no movement, unable to create movement"
    auditService.auditMessage(message, errorMessage)
    logger.error(errorMessage)
  }

  private def createMovementFromIE704(consignor: String, message: IE704Message)(implicit
    hc: HeaderCarrier
  ): Future[Seq[Movement]] = {
    val timestamp = dateTimeService.timestamp()
    convertMessage(consignor, message, timestamp).map { convertedMessage =>
      message.localReferenceNumber
        .map { lrn =>
          Seq(
            Movement(
              correlationIdService.generateCorrelationId(),
              None,
              lrn,
              consignor,
              None,
              administrativeReferenceCode = message.administrativeReferenceCode.head,
              timestamp,
              messages = Seq(convertedMessage)
            )
          )
        }
        .getOrElse {
          auditMessageForNoMovement(message)
          Seq.empty
        }
    }
  }

  private def createMovementFromIE801(message: IE801Message): Future[Movement] = {
    val timestamp = dateTimeService.timestamp()
    Seq(
      Some(convertMessage(message.consignorId, message, timestamp)),
      message.consigneeId.map(convertMessage(_, message, timestamp))
    ).flatten.sequence.map { messages =>
      Movement(
        correlationIdService.generateCorrelationId(),
        None,
        message.localReferenceNumber,
        message.consignorId,
        message.consigneeId,
        administrativeReferenceCode = message.administrativeReferenceCode.head,
        timestamp,
        messages = messages
      )
    }
  }

  private def findByArc(movements: Seq[Movement], message: IEMessage): OptionT[Future, Seq[Movement]] = {

    val matchedMovements = message.administrativeReferenceCode.flatten.flatMap { arc =>
      movements.find(_.administrativeReferenceCode.contains(arc))
    }

    if (matchedMovements.isEmpty) OptionT.none else OptionT.pure(matchedMovements)
  }

  private def findByArcInMessage(message: IEMessage): OptionT[Future, Seq[Movement]] =
    message.administrativeReferenceCode.flatten.traverse { arc =>
      OptionT(movementRepository.getByArc(arc))
    }

  private def findByLrn(movements: Seq[Movement], message: IEMessage): OptionT[Future, Seq[Movement]] =
    OptionT.fromOption(
      movements
        .find(movement => message.lrnEquals(movement.localReferenceNumber))
        .map(Seq(_))
    )

  private def convertMessage(recipient: String, input: IEMessage, timestamp: Instant): Future[Message] =
    boxIdRepository.getBoxIds(recipient).map { boxIds =>
      val encodedMessage = emcsUtils.encode(input.toXml.toString)
      messageSizes.update(encodedMessage.length)
      Message(
        encodedMessage = encodedMessage,
        messageType = input.messageType,
        messageId = input.messageIdentifier,
        recipient = recipient,
        boxesToNotify = boxIds,
        createdOn = timestamp
      )
    }

  private def createEnrichedError[A](
    e: Throwable,
    ern: String,
    movements: Seq[Movement],
    movement: Movement
  ): Future[A] =
    e match {
      case e: MongoCommandException if e.getErrorCode == 11000 =>
        movementRepository
          .getMovementByLRNAndERNIn(movement.localReferenceNumber, List(movement.consignorId))
          .flatMap { movementByLrn =>
            movement.administrativeReferenceCode.flatTraverse(movementRepository.getByArc).map { movementByArc =>
              val movementMessage      =
                s"id: ${movement._id}, consignor: ${movement.consignorId}, lrn: ${movement.localReferenceNumber}, consignee: ${movement.consigneeId}, arc: ${movement.administrativeReferenceCode}, messages: ${movement.messages
                  .map(_.messageType)}, currentTime: ${Instant.now()})"
              val movementByLrnMessage = movementByLrn.headOption
                .map(m =>
                  s"Some(id: ${m._id}, consignor: ${m.consignorId}, lrn: ${m.localReferenceNumber}, consignee: ${m.consigneeId}, arc: ${m.administrativeReferenceCode}, exists in initially retrieved movements: ${movements
                    .exists(_._id == m._id)}, lastUpdated: ${m.lastUpdated}, latestMessageUpdate: ${m.messages
                    .maxByOption(_.createdOn)
                    .map(_.createdOn)}, messages: ${m.messages.map(_.messageType)})"
                )
                .getOrElse("None")
              val movementByArcMessage = movementByArc
                .map(m =>
                  s"Some(id: ${m._id}, consignor: ${m.consignorId}, lrn: ${m.localReferenceNumber}, consignee: ${m.consigneeId}, arc: ${m.administrativeReferenceCode}, lastUpdated: ${m.lastUpdated}, latestMessage: ${m.messages
                    .maxByOption(_.createdOn)
                    .map(_.createdOn)}, messages: ${m.messages.map(_.messageType)})"
                )
                .getOrElse("None")
              logger.warn(
                s"Failed to save movement because of duplicate key violation: \n\nMovement - $movementMessage \n\nExisting movement by LRN - $movementByLrnMessage \n\nExisting movement by ARC - $movementByArcMessage",
                e
              )
            }
            Future.failed(e)
          }
      case _: BsonMaximumSizeExceededException                 =>
        val messageSizes = movement.messages.map(_.encodedMessage.length).mkString("[", ", ", "]")
        val message      =
          s"ern: $ern, movementId: ${movement._id}, numberOfMessages: ${movement.messages.size}, messageSizes:$messageSizes inner: ${e.getMessage}"
        Future.failed(EnrichedError(message, e))
      case _                                                   =>
        val message = s"ern: $ern, movementId: ${movement._id}, inner: ${e.getMessage}"
        Future.failed(EnrichedError(message, e))
    }
}

object MessageService {
  sealed trait UpdateOutcome

  object UpdateOutcome {
    case object Updated extends UpdateOutcome
    case object Locked extends UpdateOutcome
    case object NotUpdatedThrottled extends UpdateOutcome
  }

  final case class EnrichedError(message: String, cause: Throwable) extends Throwable {
    override def getStackTrace: Array[StackTraceElement] = cause.getStackTrace
    override def getMessage: String                      = message
  }
}
