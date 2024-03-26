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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import cats.syntax.all._
import com.google.inject.Singleton
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementService @Inject()(
  movementRepository: MovementRepository,
  emcsUtils: EmcsUtils,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) extends Logging {

  def saveNewMovement(movement: Movement): Future[Either[Result, Movement]] = {

    getMovementByLRNAndERNIn(movement.localReferenceNumber, List(movement.consignorId)).
      flatMap {
        case Some(m) if isLrnAlreadyUsed(movement, m) => createDuplicateErrorResponse(movement)
        case Some(m) => Future(Right(m))
        case _ => movementRepository.saveMovement(movement).map(_ => Right(movement))
      }
  }.recover {
    case ex: Throwable =>
      logger.error(s"[MovementService] - Error occurred while saving movement, ${ex.getMessage}")
      Left(InternalServerError(Json.toJson(
        ErrorResponse(
          dateTimeService.timestamp(),
          "Database error",
          ex.getMessage
        )
      )))
  }

  def getMovementById(id: String): Future[Option[Movement]] = {
    movementRepository.getMovementById(id)
  }

  def getMovementByLRNAndERNIn(lrn: String, erns: List[String]): Future[Option[Movement]] = {
    movementRepository.getMovementByLRNAndERNIn(lrn, erns).map {
      case Seq() => None
      case head :: Nil => Some(head)
      case _ => throw new RuntimeException(s"[MovementService] - Multiple movement found for local reference number: $lrn")
    }
  }

  def getMovementByErn(
    ern: Seq[String],
    filter: MovementFilter = MovementFilter.empty
  ): Future[Seq[Movement]] = {

    movementRepository.getMovementByERN(ern).map {
      movements => filter.filterMovement(movements)
    }
  }

  def updateMovement(message: IEMessage, ern: String): Future[Seq[Movement]] = {

    movementRepository.getAllBy(ern).map(cachedMovements => {

      message.administrativeReferenceCode
        .map { messageArc =>
          updateMovementForIndividualArc(message, ern, cachedMovements, messageArc)
        }
        .sequence
        .map((o: Seq[Either[String, Movement]]) =>
          transformAndLogAnyError(o, ern, message.messageType)
        )
    }).flatten
  }

  private def transformAndLogAnyError(
    movements: Seq[Either[String, Movement]],
    ern: String,
    messageType: String
  ): Seq[Movement] = {
    movements.foldLeft[Seq[Movement]](Seq()) {
      case (acc: Seq[Movement], mv: Either[String, Movement]) =>
        mv match {
          case Right(m) => acc :+ m
          case Left(error) =>
            logger.warn(s"[MovementService] - Could not update movement with excise number $ern and message: $messageType. Error was $error")
            acc
        }
    }
  }

  private def createDuplicateErrorResponse(movement: Movement) = {
    Future.successful(Left(BadRequest(Json.toJson(
      ErrorResponse(
        dateTimeService.timestamp(),
        "Duplicate LRN error",
        s"The local reference number ${movement.localReferenceNumber} has already been used for another movement"
      )
    ))))
  }

  private def updateMovementForIndividualArc(
    message: IEMessage,
    ern: String,
    cachedMovements: Seq[Movement],
    messageArc: Option[String]
  ): Future[Either[String, Movement]] = {
    val movementWithArc = cachedMovements.find(o => o.administrativeReferenceCode.equals(messageArc))
    val movementWithLrn = cachedMovements.find(m => message.lrnEquals(m.localReferenceNumber))

    (movementWithArc, movementWithLrn) match {
      case (Some(mArc), _) => saveDistinctMessage(mArc, message, messageArc)
      case (None, Some(mLrn)) => saveDistinctMessage(mLrn, message, messageArc)
      case _ => throw new RuntimeException(s"[MovementService] - Cannot find movement for ERN: $ern, ${message.toString}")
    }
  }

  private def saveDistinctMessage(
                                   movement: Movement,
                                   newMessage: IEMessage,
                                   messageArc: Option[String]
                                 ): Future[Either[String,Movement]] = {

    val message = Message(
      encodedMessage = emcsUtils.encode(newMessage.toXml.toString),
      messageType = newMessage.messageType,
      messageId = newMessage.messageIdentifier,
      createdOn = dateTimeService.timestamp()
    )

    //todo EMCS-382: remove hash from message class. Hash can calculate on the go in here
    movement.messages.find(o => o.hash.equals(message.hash)) match {
      case Some(_) => successful(Left(s"Message has already been saved against movement ${movement._id}"))
      case _ =>
        val newArc = messageArc.orElse(movement.administrativeReferenceCode)

        val newMovement = movement.copy(
          administrativeReferenceCode = movement.administrativeReferenceCode.fold(newArc)(Some(_)),
          messages = movement.messages :+ message
        )

        val updateResult = movementRepository.updateMovement(newMovement)

        updateResult.map {
          case Some(x) => Right(x)
          case None => Left(s"Failed to update movement ${newMovement._id}")
        }
    }
  }

  private def isLrnAlreadyUsed(movement: Movement, movementFromDb: Movement) = {
    movement.consignorId == movementFromDb.consignorId &&
      (movementFromDb.administrativeReferenceCode.isDefined
        || movementFromDb.consigneeId != movement.consigneeId)
  }
}

