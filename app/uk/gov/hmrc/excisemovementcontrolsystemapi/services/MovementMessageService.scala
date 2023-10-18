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

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Singleton
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EisUtils, ErrorResponse, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}

import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementMessageService @Inject()(
    movementMessageRepository: MovementMessageRepository,
    eisUtils: EisUtils,
    dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) {
  def updateMovement(message: IEMessage, consignorId: String): Future[Boolean] = {

      getMovement(message, consignorId).flatMap {
        case Some(movement) => saveDistinctMessage(movement, message)
        case None => Future.successful(false)
      }
  }

  private def getMovement(message: IEMessage, consignorId: String): Future[Option[Movement]] = {
    (message.administrativeRefCode, message.localReferenceNumber) match {
      case (Some(arc), _) => movementMessageRepository.getByArc(arc, List(consignorId))
      case (None, Some(lrn)) => movementMessageRepository.get(lrn, List(consignorId))
      case _ => throw new IllegalArgumentException("Cannot retrieve a movement. Local reference number or administration reference code may be invalid")
    }
  }

  def saveMovementMessage(movementMessage: Movement): Future[Either[MongoError, Movement]] = {
    movementMessageRepository.save(movementMessage)
      .map(_ => Right(movementMessage))
      .recover {
        case ex: Throwable => Left(MongoError(ex.getMessage))
      }
  }

  def getMovementMessagesByLRNAndERNIn(lrn: String, erns: List[String]): Future[Either[ErrorResponse, Seq[Message]]] = {
    movementMessageRepository.get(lrn, erns).map {
        case Some(movement) => Right(movement.messages)
        case _ => Left(NotFoundError())
      }
      .recover {
        case ex: Throwable => Left(MongoError(ex.getMessage))
      }
  }

  def getUniqueConsignorId: Future[Source[Movement, NotUsed]] = {
    movementMessageRepository.getMovements
      .map(m => m.distinctBy(o => o.consignorId))
      .map(o => Source(o))
  }

  private def saveDistinctMessage(movement: Movement, newMessage: IEMessage): Future[Boolean] = {

    val encodedMessage = eisUtils.createEncoder.encodeToString(newMessage.toXml.toString.getBytes(StandardCharsets.UTF_8))
    val messages = Seq(Message(encodedMessage, newMessage.getType, dateTimeService))

    val allMessages = movement.messages ++ messages.diff(movement.messages)

    val newMovement = movement copy (
      administrativeReferenceCode = newMessage.administrativeRefCode,
      messages = allMessages
    )
    movementMessageRepository.save(newMovement).map {
      case true => true
      case _ => false
    }
  }
}
