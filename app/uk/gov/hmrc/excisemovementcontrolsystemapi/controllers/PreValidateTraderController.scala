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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PreValidateTraderConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseJsonAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.ParsedPreValidateTraderRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreValidateTraderController @Inject()(
                                             authAction: AuthAction,
                                             parseJsonAction: ParseJsonAction,
                                             connector: PreValidateTraderConnector,
                                             cc: ControllerComponents
                                           )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit: Action[JsValue] = {

    (authAction andThen parseJsonAction).async(parse.json) {
      implicit request: ParsedPreValidateTraderRequest[JsValue] =>
        connector.submitMessage(request.json, "request.request.erns.head").flatMap {
          case Right(response) => response match {
            case Left(errorResponse) => Future.successful(Ok(Json.toJson(errorResponse)))
            case Right(successResponse) => Future.successful(Ok(Json.toJson(successResponse)))
          }
          case Left(error) => Future.successful(error)
        }
    }

  }
}
