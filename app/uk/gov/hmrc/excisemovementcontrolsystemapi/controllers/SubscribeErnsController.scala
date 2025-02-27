/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, CorrelationIdAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscribeErnsController @Inject() (
  authAction: AuthAction,
  correlationIdAction: CorrelationIdAction,
  cc: ControllerComponents,
  notificationsService: NotificationsService,
  dateTimeService: DateTimeService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def subscribeErn(ern: String): Action[AnyContent] = (authAction andThen correlationIdAction).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      if (appConfig.subscribeErnsEnabled) {
        if (request.erns.contains(ern)) {
          (for {
            clientId <- retrieveClientIdFromHeader(request)
            boxId    <- EitherT.liftF[Future, Result, String](notificationsService.subscribeErns(clientId, Seq(ern)))
          } yield Accepted(boxId)).valueOrF(Future.successful)
        } else {
          Future.successful(
            Forbidden(
              Json.toJson(
                ErrorResponse(
                  dateTimeService.timestamp(),
                  "Forbidden",
                  "Invalid ERN provided"
                )
              )
            )
          )
        }

      } else {
        Future.successful(NotFound)
      }
  }

  def unsubscribeErn(ern: String): Action[AnyContent] = (authAction andThen correlationIdAction).async {
    implicit request =>
      if (appConfig.subscribeErnsEnabled) {

        (for {
          clientId <- retrieveClientIdFromHeader(request)
          _        <- EitherT.liftF[Future, Result, Done](notificationsService.unsubscribeErns(clientId, Seq(ern)))
        } yield Accepted).valueOrF(Future.successful)
      } else {
        Future.successful(NotFound)
      }
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
