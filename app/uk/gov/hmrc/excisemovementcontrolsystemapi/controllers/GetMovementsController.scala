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

import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.mvc.Results.Ok
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.GetMovementConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsController @Inject()(
  authAction: AuthAction,
  cc: ControllerComponents,
  movementService: MovementService
)(implicit ec: ExecutionContext) extends BackendController(cc)  {

  def getMovements: Action[AnyContent] = {
    authAction.async(parse.default) {
      implicit request =>

        /*
        todo:
          1. When submitting IE815 generate an ARC and add it to Mongo for that movement (temporary)
          2. get all the movement for all ERNs from mongo
          3. Return the list of movement
          4. apply filter for LRN, arc and ern
          5. if not arc available just return the movement with no ARC
          6. at the moment we do not call EIS.

        */

        movementService.getMovementByErn(request.erns.toSeq).map { movement: Seq[Movement] =>

         val newMovements: Seq[GetMovementResponse] = movement.map(m => GetMovementResponse(
           m.consignorId,
           m.localReferenceNumber,
           m.consigneeId,
           m.administrativeReferenceCode.get,
           ACCEPTED
         ))
          Ok(Json.toJson(newMovements))
        }

    }
  }

}
