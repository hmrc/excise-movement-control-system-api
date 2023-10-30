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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.GeneralMongoError
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementService @Inject()(
                                 movementRepository: MovementRepository
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
      case _ => throw new RuntimeException(s"Multiple movement found for local reference number: $lrn")
    }
  }

  def getMatchingERN(lrn: String, erns: List[String]): Future[Option[String]] = {
    movementRepository.getMovementByLRNAndERNIn(lrn, erns).map {
      case Seq()  => None
      case head :: Nil => matchingERN(head, erns)
      case _ => throw new RuntimeException(s"Multiple movement found for local reference number: $lrn")
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

  private def matchingERN(movement: Movement, erns: List[String]): Option[String] = {
    if (erns.contains(movement.consignorId)) Some(movement.consignorId)
    else movement.consigneeId
  }
}
