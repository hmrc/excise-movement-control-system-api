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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsController @Inject()(
  authAction: AuthAction,
  cc: ControllerComponents,
  getMovementConnector: GetMovementConnector
)(implicit ec: ExecutionContext) extends BackendController(cc)  {

  def getMovements: Action[AnyContent] = {
    authAction.async(parse.default) {
      implicit request =>

        getMovementConnector.get(request.erns.head, "arc").map {
          case Right(response) =>
            //todo get the movement from Mongo using lrn and ern from EIS
            Ok(Json.toJson(Seq(GetMovementResponse(
              response.exciseRegistrationNumber,
              "lrn",
              "consigneeId",
              "arc",
              ACCEPTED
            ))))
        }
    }
  }

}
