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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.GetMovementResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsController @Inject()(
  authAction: AuthAction,
  cc: ControllerComponents,
  movementService: MovementService,
  workItemService: WorkItemService
)(implicit ec: ExecutionContext)
  extends BackendController(cc)
  with Logging {

  def getMovements(ern: Option[String], lrn: Option[String], arc: Option[String]): Action[AnyContent] = {
    authAction.async(parse.default) {
      implicit request =>

        addWorkItem(ern.getOrElse(request.erns.head))

        val filter = MovementFilter.and(Seq("ern" -> ern, "lrn" -> lrn, "arc" -> arc))
        movementService.getMovementByErn(request.erns.toSeq, filter)
          .map { movement: Seq[Movement] =>
            Ok(Json.toJson(movement.map(createResponseFrom)))
        }
    }
  }

  private def createResponseFrom(movement: Movement) = {
    GetMovementResponse(
      movement.consignorId,
      movement.localReferenceNumber,
      movement.consigneeId,
      movement.administrativeReferenceCode,
      "Accepted"
    )
  }

  private def addWorkItem(ern: String) = {
    try {
      workItemService.addWorkItemForErn(ern, fastMode = false).recover {
        case ex: Throwable =>
          logger.error(s"[GetMovementsController] - Failed to create Work Item for ERN $ern: ${ex.getMessage}")
          //TODO compiler warning on type here
          Future.failed(ex)
      }
    }
    catch {
      case ex: Exception => logger.error(s"[GetMovementsController] - Database error while creating Work Item for ERN $ern: ${ex.getMessage}")
    }
  }

}
