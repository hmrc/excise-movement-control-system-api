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
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseJsonAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.{ParsedPreValidateTraderETDSRequest, ParsedPreValidateTraderRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{ExciseTraderValidationETDSResponse, PreValidateTraderETDS400ErrorMessageResponse, PreValidateTraderETDS500ErrorMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PreValidateTraderService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreValidateTraderController @Inject() (
  authAction: AuthAction,
  parseJsonAction: ParseJsonAction,
  preValidateTraderService: PreValidateTraderService,
  cc: ControllerComponents,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def submit: Action[JsValue] = authAction(parse.json).async { implicit authRequest: EnrolmentRequest[JsValue] =>
    if (appConfig.etdsPreValidateTraderEnabled) {
      // ETDS request
      parseJsonAction.refineETDS(authRequest).flatMap {
        case Right(parsedRequest: ParsedPreValidateTraderETDSRequest[JsValue]) =>
          preValidateTraderService.submitETDSMessage(parsedRequest).map {
            case Right(response) =>
              response match {
                case response200: ExciseTraderValidationETDSResponse           => Ok(Json.toJson(response200))
                case response400: PreValidateTraderETDS400ErrorMessageResponse =>
                  BadRequest(Json.toJson(response400))
                case response500: PreValidateTraderETDS500ErrorMessageResponse =>
                  InternalServerError(Json.toJson(response500))
              }
            case Left(error)     =>
              error
          }
        case Left(result)                                                      =>
          Future.successful(result)
      }
    } else {
      // Legacy request
      parseJsonAction.refine(authRequest).flatMap {
        case Right(parsedRequest: ParsedPreValidateTraderRequest[JsValue]) =>
          preValidateTraderService.submitMessage(parsedRequest).map {
            case Right(response) => Ok(Json.toJson(response))
            case Left(error)     => error
          }
        case Left(result)                                                  =>
          Future.successful(result)
      }
    }
  }
}
