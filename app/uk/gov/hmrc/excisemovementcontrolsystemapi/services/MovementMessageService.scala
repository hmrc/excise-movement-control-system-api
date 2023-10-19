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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{DatabaseError, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, MovementMessage}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementMessageService @Inject()(
                                        movementMessageRepository: MovementMessageRepository
                                      )(implicit ec: ExecutionContext) {

  def saveMovementMessage(movementMessage: MovementMessage): Future[Either[MongoError, MovementMessage]] = {
    movementMessageRepository.saveMovementMessage(movementMessage)
      .map(_ => Right(movementMessage))
      .recover {
        case ex: Throwable => Left(MongoError(ex.getMessage))
      }
  }

  def getMovementMessagesByLRNAndERNIn(lrn: String, erns: List[String]): Future[Either[DatabaseError, Seq[Message]]] = {
    movementMessageRepository.getMovementMessagesByLRNAndERNIn(lrn, erns).map {
        case Nil => Left(NotFoundError())
        case f@_ :: Nil =>
          f.head.messages match {
            case Some(m) => Right(m)
            case None => Right(Seq.empty)
          }
        case _ => Left(MongoError("Multiple movements found for lrn and ern combination"))
      }
      .recover {
        case ex: Throwable => Left(MongoError(ex.getMessage))
      }
  }

}