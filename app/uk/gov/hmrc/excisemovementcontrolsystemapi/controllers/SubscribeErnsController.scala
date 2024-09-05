/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import org.apache.pekko.Done
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscribeErnsController @Inject() (
  authAction: AuthAction,
  cc: ControllerComponents,
  notificationsService: NotificationsService,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def subscribeErn(ernId: String): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      clientId <- retrieveClientIdFromHeader(request)
      _        <- EitherT.liftF[Future, Result, String](notificationsService.subscribeErns(clientId, Seq(ernId)))
    } yield Ok).valueOrF(Future.successful)
  }

  def unsubscribeErn(ernId: String): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      clientId <- retrieveClientIdFromHeader(request)
      _        <- EitherT.liftF[Future, Result, Done](notificationsService.unsubscribeErns(clientId, Seq(ernId)))
    } yield Ok).valueOrF(Future.successful)
  }

  private def retrieveClientIdFromHeader(implicit request: EnrolmentRequest[_]): EitherT[Future, Result, String] =
    EitherT.fromOption(
      request.headers.get(Constants.XClientIdHeader),
      BadRequest(
        Json.toJson(
          ErrorResponse(
            dateTimeService.timestamp(),
            s"ClientId error",
            s"Request header is missing ${Constants.XClientIdHeader}"
          )
        )
      )
    )
}