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

import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.SubscribeErnsAdminController.SubscribeErnsRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubscribeErnsAdminController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  notificationsService: NotificationsService
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  private val permission = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("admin/notifications/subscribe")),
    IAAction("ADMIN")
  )

  private val authorised = auth.authorizedAction(
    predicate = permission,
    retrieval = Retrieval.EmptyRetrieval
  )

  def subscribeErns(): Action[JsValue] =
    authorised.async(parse.json) { implicit request =>
      withJsonBody[SubscribeErnsRequest] { subscribeRequest =>
        notificationsService
          .subscribeErns(subscribeRequest.clientId, subscribeRequest.erns.toSeq)
          .map(Ok(_))
      }
    }
}

object SubscribeErnsAdminController {

  final case class SubscribeErnsRequest(clientId: String, erns: Set[String])

  object SubscribeErnsRequest {
    implicit lazy val format: OFormat[SubscribeErnsRequest] = Json.format
  }
}
