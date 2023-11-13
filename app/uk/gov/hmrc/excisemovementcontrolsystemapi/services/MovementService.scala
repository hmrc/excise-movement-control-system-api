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

import com.google.inject.Singleton
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, GeneralMongoError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementService @Inject()
(
  movementRepository: MovementRepository,
  emcsUtils: EmcsUtils,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) {
  def saveMovementMessage(movementMessage: Movement): Future[Either[GeneralMongoError, Movement]] = {
    movementRepository.saveMovement(movementMessage)
      .map(_ => Right(movementMessage))
      .recover {
        case ex: Throwable => Left(GeneralMongoError(ex.getMessage))
      }
  }

  def getMovementMessagesByLRNAndERNIn(lrn: String, erns: List[String]): Future[Option[Movement]] = {
    movementRepository.getMovementByLRNAndERNIn(lrn, erns).map {
      case Seq()  => None
      case head :: Nil => Some(head)
      case _ => throw new RuntimeException(s"[MovementService] - Multiple movement found for local reference number: $lrn")
    }
  }

  def getMatchingERN(lrn: String, erns: List[String]): Future[Option[String]] = {
    movementRepository.getMovementByLRNAndERNIn(lrn, erns).map {
      case Seq()  => None
      case head :: Nil => matchingERN(head, erns)
      case _ => throw new RuntimeException(s"[MovementService] - Multiple movements found for local reference number: $lrn")
    }
  }

  def getMovementByErn(
    ern: Seq[String],
    filter: MovementFilter = MovementFilter.empty
  ): Future[Seq[Movement]] = {

    movementRepository.getMovementByERN(ern).map {
      movements =>filter.filterMovement(movements)
    }
  }

  def updateMovement(message: IEMessage, consignorId: String): Future[Boolean] = {

    movementRepository.getAllBy(consignorId).flatMap(cachedMovement => {
      val arc = message.administrativeReferenceCode
      //todo get LRN using pattern match
      val lrn = "123" //message.localReferenceNumber.getOrElse("")
      val movementWithArc = cachedMovement.filter(o => o.administrativeReferenceCode.equals(arc)).headOption
      val movementWithLrn = cachedMovement.filter(m => m.localReferenceNumber.equals(lrn)).headOption

      (movementWithArc, movementWithLrn) match {
        case (Some(mArc), _) => saveDistinctMessage(mArc, message)
        case (None, Some(mLrn)) => saveDistinctMessage(mLrn, message)
        case _ => throw new RuntimeException("Cannot retrieve a movement. Local reference number or administration reference code are not present")
      }
    })
  }

  private def saveDistinctMessage(movement: Movement, newMessage: IEMessage): Future[Boolean] = {


    val encodedMessage = emcsUtils.encode(newMessage.toXml.toString)
    val messages = Seq(Message(encodedMessage, newMessage.messageType, Instant.now))

    //todo: remove hash from message class. Hash can calculate on the go in here
    val allMessages = (movement.messages ++ messages).distinctBy(_.hash)
    val newArc = newMessage.administrativeReferenceCode.orElse(movement.administrativeReferenceCode)

    val newMovement = movement.copy(
      administrativeReferenceCode = newArc,
      messages = allMessages
    )

    movementRepository.updateMovement(newMovement).map {
      case true => true
      case _ => false
    }
  }

  private def matchingERN(movement: Movement, erns: List[String]): Option[String] = {
    if (erns.contains(movement.consignorId)) Some(movement.consignorId)
    else movement.consigneeId
  }
}
