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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, NotFound}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class MovementIdValidation @Inject()(movementService: MovementService)(implicit ec: ExecutionContext) {

  def validateMovementId(id: String): Future[Either[MovementValidationError, Movement]] = {

    Try(UUID.fromString(id)).toEither match {
      case Left(_) => Future(Left(MovementIdFormatInvalid()))
      case Right(_) =>
        movementService.getMovementById(id).map {
          case Some(movement) => Right(movement)
          case None => Left(MovementIdNotFound(id))
        }
    }

  }

  def convertErrorToResponse(error: MovementValidationError, timestamp: Instant): Result = {
    error match {
      case x: MovementIdNotFound => NotFound(Json.toJson(
        ErrorResponse(timestamp, "Movement not found", x.errorMessage
        )))
      case x: MovementIdFormatInvalid => BadRequest(Json.toJson(
        ErrorResponse(timestamp, "Movement Id format error", x.errorMessage)
      ))
    }
  }

}

sealed trait MovementValidationError {
  def errorMessage: String
}

case class MovementIdFormatInvalid() extends MovementValidationError {
  override def errorMessage: String = "The movement ID should be a valid UUID"
}

case class MovementIdNotFound(movementId: String) extends MovementValidationError {
  override def errorMessage: String = s"Movement $movementId could not be found"
}
