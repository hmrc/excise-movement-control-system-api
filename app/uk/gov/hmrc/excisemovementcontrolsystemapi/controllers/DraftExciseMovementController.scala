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
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction, ValidateErnInMessageAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, ExciseMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, PushNotificationService, SubmissionMessageService, WorkItemService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementController @Inject()(
  authAction: AuthAction,
  xmlParser: ParseXmlAction,
  validateErnInMessageAction: ValidateErnInMessageAction,
  movementMessageService: MovementService,
  workItemService: WorkItemService,
  submissionMessageService: SubmissionMessageService,
  notificationService: PushNotificationService,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit: Action[NodeSeq] =
    (authAction andThen xmlParser andThen validateErnInMessageAction).async(parse.xml) {
      implicit request: ValidatedXmlRequest[NodeSeq] =>

        request.headers.get(Constants.XClientIdHeader) match {
          case Some(clientId) =>
            notificationService.getBoxId(clientId).flatMap {
              case Right(box) => submitMessage(box.boxId)
              case Left(error) => Future.successful(error)
            }
          case _ => Future.successful(BadRequest(Json.toJson(ErrorResponse(Instant.now, s"ClientId error", s"Request header is missing ${Constants.XClientIdHeader}"))))
        }
    }

  private def submitMessage(boxId: String)(implicit request: ValidatedXmlRequest[NodeSeq]): Future[Result] = {
    submissionMessageService.submit(request).flatMap {
      case Right(_) => handleSuccess(boxId)
      case Left(error) => Future.successful(error)
    }
  }

  private def handleSuccess(boxId: String)(implicit request: ValidatedXmlRequest[NodeSeq]): Future[Result] = {

    val newMovement: Movement = createMovementFomMessage(request.message, boxId)
    workItemService.addWorkItemForErn(newMovement.consignorId, fastMode = true)

    movementMessageService.saveNewMovement(newMovement).map {
      case Right(m) =>
        Accepted(Json.toJson(ExciseMovementResponse("Accepted", boxId, m._id, m.localReferenceNumber, m.consignorId, m.consigneeId)))
      case Left(error) =>
        error
    }
  }

  private def createMovementFomMessage(message: IEMessage, boxId: String): Movement = {
    message match {
      case x: IE815Message => Movement(
        boxId,
        x.localReferenceNumber,
        x.consignorId,
        x.consigneeId,
        None
      )
      case _ =>
        throw new Exception(s"[DraftExciseMovementController] - invalid message sent to draft excise movement controller, message type: ${message.messageType}")
    }
  }

}
