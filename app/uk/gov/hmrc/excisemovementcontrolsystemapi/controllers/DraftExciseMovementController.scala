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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction, ValidateErnInMessageAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ExciseMovementResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementController @Inject()(
                                               authAction: AuthAction,
                                               xmlParser: ParseXmlAction,
                                               validateErnInMessageAction: ValidateErnInMessageAction,
                                               movementMessageConnector: EISSubmissionConnector,
                                               movementMessageService: MovementService,
                                               workItemService: WorkItemService,
                                               cc: ControllerComponents
                                             )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit: Action[NodeSeq] =
    (authAction andThen xmlParser andThen validateErnInMessageAction).async(parse.xml) {
      implicit request: ValidatedXmlRequest[NodeSeq] =>
        movementMessageConnector.submitMessage(request).flatMap {
          case Right(_) => handleSuccess
          case Left(error) => Future.successful(error)
        }
    }


  private def handleSuccess(implicit request: ValidatedXmlRequest[NodeSeq]): Future[Result] = {

    val ieMessage = request.parsedRequest.ieMessage
    val newMovement = ieMessage match {
      case x: IE815Message => Movement(
        x.localReferenceNumber,
        x.consignorId,
        x.consigneeId,
        None
      )
      case _ => throw new Exception("invalid message sent to draft excise movement controller")
    }
    val ern = newMovement.consignorId

    workItemService.addWorkItemForErn(ern, fastMode = true)

    movementMessageService.saveNewMovement(newMovement)
      .flatMap {
        case Right(msg) => Future.successful(Accepted(Json.toJson(ExciseMovementResponse("Accepted", msg.localReferenceNumber, msg.consignorId, msg.consigneeId))))
        case Left(error) => Future.successful(error)
      }

  }

}
