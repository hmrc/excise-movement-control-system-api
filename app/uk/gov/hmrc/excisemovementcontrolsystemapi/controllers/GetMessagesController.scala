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
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementMessageService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetMessagesController @Inject()(
                                       authAction: AuthAction,
                                       movementMessageService: MovementMessageService,
                                       cc: ControllerComponents
                                     )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getMessagesForMovement(lrn: String): Action[AnyContent] =
    authAction.async(parse.default) {
      implicit request: EnrolmentRequest[AnyContent] => {
        getMovementMessagesByLRNAndERNList(lrn, request.erns.toList)
      }
    }

  private def getMovementMessagesByLRNAndERNList(lrn: String, ern: List[String]): Future[Result] = {
    movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, ern).flatMap {
      case Right(msg) => Future.successful(Ok(Json.toJson(msg)))
      case Left(error: NotFoundError) => Future.successful(NotFound(error.message))
      case Left(error: MongoError) => Future.successful(InternalServerError(error.message))
    }
  }

}
