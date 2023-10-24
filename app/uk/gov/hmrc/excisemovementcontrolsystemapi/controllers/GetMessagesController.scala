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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.ShowNewMessagesConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetMessagesController @Inject()(
                                       authAction: AuthAction,
                                       messagesConnector: ShowNewMessagesConnector,
                                       movementService: MovementService,
                                       cc: ControllerComponents
                                     )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getMessagesForMovement(lrn: String): Action[AnyContent] =
    authAction.async(parse.default) {
      implicit request: EnrolmentRequest[AnyContent] => {

        //get the erns from movement by LRN and ERN combination (consignor/consigneeId)
        // then for each erns call messagesConnector.get
        //TODO multiple erns in list

        val erns = request.erns.toList
        movementService.getMovementByLRNAndERNIn(lrn, erns).flatMap {
          case None => Future.successful(BadRequest(Json.toJson(ErrorResponse(LocalDateTime.now(), "Invalid LRN supplied for ERN", ""))))
          case Some(movement) =>
            val ern = matchingERN(movement, erns)

            messagesConnector.get(ern).map {
              case Right(messages) => Ok(Json.toJson(messages))
              case Left(error) => error
            }
        }

        //getMovementMessagesByLRNAndERNList(lrn, request.erns.toList)
      }
    }

  private def matchingERN(movement: Movement, erns: List[String]) = {
    if (erns.contains(movement.consignorId)) movement.consignorId else movement.consigneeId.get
  }

  private def getMovementMessagesByLRNAndERNList(lrn: String, ern: List[String]): Future[Result] = {
    movementService.getMovementMessagesByLRNAndERNIn(lrn, ern).flatMap {
      case Right(msg) => Future.successful(Ok(Json.toJson(msg)))
      case Left(error: NotFoundError) => Future.successful(NotFound(error.message))
      case Left(error) => Future.successful(InternalServerError(error.message))
    }
  }

}
