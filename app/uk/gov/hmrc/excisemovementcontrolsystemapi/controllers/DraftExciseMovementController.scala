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
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MovementMessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseIE815XmlAction, ValidateConsignorAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ExciseMovementResponse, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.DataRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementMessageService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementController @Inject()(
                                               authAction: AuthAction,
                                               xmlParser: ParseIE815XmlAction,
                                               consignorValidatorAction: ValidateConsignorAction,
                                               movementMessageConnector: MovementMessageConnector,
                                               movementMessageService: MovementMessageService,
                                               cc: ControllerComponents
                                             )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def submit: Action[NodeSeq] =
    (authAction andThen xmlParser andThen consignorValidatorAction).async(parse.xml) {
      implicit request: DataRequest[NodeSeq] =>
        movementMessageConnector.submitExciseMovement(request, MessageTypes.IE815Message).flatMap {
          case Right(_) => handleSuccess
          case Left(error) => Future.successful(error)
        }
    }


  private def handleSuccess(implicit request: DataRequest[NodeSeq]): Future[Result] = {

    movementMessageService.saveMovementMessage(request.movementMessage)
      .flatMap(message =>
        message match {
          case Right(msg) => Future.successful(Accepted(Json.toJson(ExciseMovementResponse("Accepted", msg.localReferenceNumber, msg.consignorId))))
          case Left(error) => Future.successful(InternalServerError(error.message))
        })
  }
}
