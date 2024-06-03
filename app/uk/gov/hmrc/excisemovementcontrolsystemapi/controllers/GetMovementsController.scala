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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import cats.data.{EitherT, OptionT}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ValidateErnParameterAction, ValidateTraderTypeAction, ValidateUpdatedSinceAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilterBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.MovementIdValidation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, ExciseMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MessageService, MovementService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsController @Inject()(
  authAction: AuthAction,
  validateErnParameterAction: ValidateErnParameterAction,
  validateUpdatedSinceAction: ValidateUpdatedSinceAction,
  validateTraderTypeAction: ValidateTraderTypeAction,
  cc: ControllerComponents,
  movementService: MovementService,
  dateTimeService: DateTimeService,
  messageService: MessageService,
  movementIdValidator: MovementIdValidation
)(implicit ec: ExecutionContext)
  extends BackendController(cc) with Logging {

  def getMovements(ern: Option[String], lrn: Option[String], arc: Option[String], updatedSince: Option[String], traderType: Option[String]): Action[AnyContent] = {
    (authAction andThen validateErnParameterAction(ern)
      andThen validateUpdatedSinceAction(updatedSince)
      andThen validateTraderTypeAction(traderType)).async(parse.default) {
      implicit request =>
        messageService.updateAllMessages(ern.fold(request.erns)(Set(_)))

        val filter = MovementFilterBuilder().withErn(ern)
          .withLrn(lrn)
          .withArc(arc)
          .withUpdatedSince(updatedSince.map(Instant.parse(_)))
          .withTraderType(traderType, request.erns.toSeq).build()

        movementService.getMovementByErn(request.erns.toSeq, filter)
          .map { movement: Seq[Movement] =>
            Ok(Json.toJson(movement.map(createResponseFrom)))
          }
    }
  }
  def getMovement(movementId: String): Action[AnyContent] = {

    authAction.async(parse.default) {
      implicit request =>

        val result = for {
          validatedMovementId <- validateMovementId(movementId)
          _ <- EitherT.right(messageService.updateAllMessages(request.erns))
          movement <- getMovementFromDb(validatedMovementId)
        } yield {
          val authorisedErns = request.erns
          val movementErns = getErnsForMovement(movement)

          if (authorisedErns.intersect(movementErns).isEmpty) {
            NotFound(Json.toJson(ErrorResponse(
              dateTimeService.timestamp(),
              "Movement not found",
              s"Movement $movementId is not found within the data for ERNs ${authorisedErns.mkString("/")}"
            )))
          } else {
            Ok(Json.toJson(createResponseFrom(movement)))
          }

        }

        result.merge
    }
  }

  private def validateMovementId(movementId: String): EitherT[Future, Result, String] = {
    EitherT.fromEither[Future](movementIdValidator.validateMovementId(movementId)).leftMap {
      x => movementIdValidator.convertErrorToResponse(x, dateTimeService.timestamp())
    }
  }

  private def getMovementFromDb(id: String): EitherT[Future, Result, Movement] = {
    OptionT(movementService.getMovementById(id)).toRightF(
      Future.successful(NotFound(Json.toJson(
        ErrorResponse(dateTimeService.timestamp(), "Movement not found", s"Movement $id could not be found")
      )))
    )
  }

  private def getErnsForMovement(movement: Movement): Set[String] = {
    Set(Some(movement.consignorId), movement.consigneeId).flatten
  }

  private def createResponseFrom(movement: Movement) = {
    ExciseMovementResponse(
      movement._id,
      None,
      movement.localReferenceNumber,
      movement.consignorId,
      movement.consigneeId,
      movement.administrativeReferenceCode
    )
  }

}
