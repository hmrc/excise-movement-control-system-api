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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ValidateErnParameterAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilterBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, GetMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GetMovementsController @Inject()(
                                        authAction: AuthAction,
                                        validateErnParameterAction: ValidateErnParameterAction,
                                        cc: ControllerComponents,
                                        movementService: MovementService,
                                        workItemService: WorkItemService,
                                        emcsUtils: EmcsUtils
                                      )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def getMovements(ern: Option[String], lrn: Option[String], arc: Option[String], updatedSince: Option[String]): Action[AnyContent] = {
    (authAction andThen validateErnParameterAction(ern)).async(parse.default) {
      implicit request =>
        //IF movement Id query, look at request ERNS, and do like the action thingy thing from the last one...
        // still need to allow for applying the other filters, since they're there. If no movementID query, then nothing changes

        workItemService.addWorkItemForErn(ern.getOrElse(request.erns.head), fastMode = false)

        Try(updatedSince.map(Instant.parse(_))).map { updatedSinceTime =>
          val filter = MovementFilterBuilder().withErn(ern).withLrn(lrn).withArc(arc).withUpdatedSince(updatedSinceTime).build()

          movementService.getMovementByErn(request.erns.toSeq, filter)
            .map { movement: Seq[Movement] =>
              Ok(Json.toJson(movement.map(createResponseFrom)))
            }
        }.getOrElse(
          Future.successful(BadRequest(Json.toJson(ErrorResponse(emcsUtils.getCurrentDateTime, "Invalid date format provided in the updatedSince query parameter", "Date format should be like '2020-11-15T17:02:34.00Z'"))))
        )
    }
  }

  private def createResponseFrom(movement: Movement) = {
    GetMovementResponse(
      movement._id,
      movement.consignorId,
      movement.localReferenceNumber,
      movement.consigneeId,
      movement.administrativeReferenceCode,
      "Accepted"
    )
  }

}
