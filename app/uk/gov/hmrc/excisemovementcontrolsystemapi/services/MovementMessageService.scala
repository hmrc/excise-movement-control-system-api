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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementMessageService @Inject()(
    messageParser: ShowNewMessageParser,
    movementMessageRepository: MovementMessageRepository
)(implicit ec: ExecutionContext) {

  def updateMovement(lrn: String, exciseNumber: String, encodedMessage: String): Future[Boolean] = {

    movementMessageRepository.get(lrn, List(exciseNumber)).flatMap {
      case Some(movement) => saveDistinctMessage(encodedMessage, movement)
      case None => Future.successful(false)
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

  private def saveDistinctMessage(encodedMessage: String, movement: Movement): Future[Boolean] = {

    val messages = messageParser.parseEncodedMessage(encodedMessage)
    val allMessages = movement.messages ++ messages.diff(movement.messages)

    movementMessageRepository.save(movement copy (messages = allMessages)).map {
      case true => true
      case _ => false
    }
  }
}
