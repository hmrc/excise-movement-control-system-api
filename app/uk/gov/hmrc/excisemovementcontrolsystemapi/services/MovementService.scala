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
import com.mongodb.MongoWriteException
import org.apache.pekko.Done
import org.mongodb.scala.MongoCommandException
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.{MovementFilter, TraderType}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class MovementService @Inject() (
  movementRepository: MovementRepository,
  dateTimeService: DateTimeService,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends Logging {

  def saveNewMovement(movement: Movement)(implicit hc: HeaderCarrier): Future[Either[Result, Movement]] =
    movementRepository
      .findDraftMovement(movement)
      .flatMap { draftMovement =>
        draftMovement.map(movement => Future.successful(Right(movement))).getOrElse {
          saveMovement(movement, None).map(_ => Right(movement))
        }
      }
      .recover {
        case _: MongoCommandException =>
          logger.warn(
            s"[MovementService] - The local reference number has already been used for another movement"
          )
          createDuplicateErrorResponse(movement)
        case NonFatal(e)              =>
          logger.error(s"[MovementService] - Error occurred while saving movement, ${e.getMessage}", e)
          Left(
            InternalServerError(
              Json.toJson(
                ErrorResponse(
                  dateTimeService.timestamp(),
                  "Database error",
                  e.getMessage
                )
              )
            )
          )
      }

  def saveMovement(movement: Movement, jobId: Option[String] = None)(implicit hc: HeaderCarrier): Future[Done] = {

    val messagesAdded = movement.messages.length
    val totalMessages = movement.messages.length

    movementRepository
      .saveMovement(movement)
      .map { _ =>
        auditService.movementSavedSuccess(messagesAdded, totalMessages, movement, "123", jobId)
        Done
      }
      .recoverWith { case e =>
        auditService.movementSavedFailure(messagesAdded, totalMessages, movement, e.getMessage, "123", jobId)
        Future.failed(e)
      }
  }

  def getMovementById(id: String): Future[Option[Movement]] =
    movementRepository.getMovementById(id)

  def getMovementByLRNAndERNIn(lrn: String, erns: List[String]): Future[Option[Movement]] =
    movementRepository.getMovementByLRNAndERNIn(lrn, erns).map {
      case Seq()       => None
      case head :: Nil => Some(head)
      case _           =>
        logger.warn(s"[MovementService] - Multiple movement found for local reference number")
        throw new RuntimeException(s"[MovementService] - Multiple movement found for local reference number")
    }

  def getMovementByErn(
    ern: Seq[String],
    filter: MovementFilter = MovementFilter.emptyFilter
  ): Future[Seq[Movement]] =
    movementRepository
      .getMovementByERN(ern, filter)
      .map(movements => filterMovementByTraderType(movements, filter.traderType))

  private def filterMovementByTraderType(movements: Seq[Movement], traderType: Option[TraderType]) =
    traderType.fold[Seq[Movement]](movements) { trader =>
      if (trader.traderType.equalsIgnoreCase("consignor")) {
        movements.filter(m => trader.erns.contains(m.consignorId))
      } else if (trader.traderType.equalsIgnoreCase("consignee")) {
        movements.filter(m => m.consigneeId.exists(trader.erns.contains(_)))
      } else {
        movements
      }
    }

  private def createDuplicateErrorResponse(movement: Movement): Either[Result, Movement] =
    Left(
      BadRequest(
        Json.toJson(
          ErrorResponse(
            dateTimeService.timestamp(),
            "Duplicate LRN error",
            s"The local reference number ${movement.localReferenceNumber} has already been used for another movement"
          )
        )
      )
    )

}
