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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, GetMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class GetMovementController @Inject()(
                                       authAction: AuthAction,
                                       movementService: MovementService,
                                       dateTimeService: DateTimeService,
                                       cc: ControllerComponents
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def getMovement(movementId: String): Action[AnyContent] = {

    authAction.async(parse.default) {
      implicit request =>

        Try(UUID.fromString(movementId)) match {
          case Success(_) =>
            movementService.getMovementById(movementId).map {
              case Some(movement) =>

                val authorisedErns = request.erns
                val movementErns = getErnsForMovement(movement)

                if (authorisedErns.intersect(movementErns).isEmpty) {
                  NotFound(Json.toJson(ErrorResponse(
                    dateTimeService.timestamp(),
                    "Movement not found",
                    s"Movement $movementId is not found within the data for ERNs ${authorisedErns.mkString("/")}"
                  )))
                } else {
                  Ok(Json.toJson(GetMovementResponse(
                    movement._id,
                    movement.consignorId,
                    movement.localReferenceNumber,
                    movement.consigneeId,
                    movement.administrativeReferenceCode,
                    "Accepted"
                  )))
                }

              case None => NotFound(Json.toJson(ErrorResponse(
                dateTimeService.timestamp(),
                "Movement not found",
                s"Movement $movementId is not found"
              )))

            }

          case _ =>
            Future.successful(BadRequest(Json.toJson(ErrorResponse(
              dateTimeService.timestamp(),
              "Movement Id format error",
              "Movement Id should be a valid UUID"
            ))))
        }
    }
    
  }

  private def getErnsForMovement(movement: Movement): Set[String] = {
    Set(Some(movement.consignorId), movement.consigneeId).flatten
  }
}
