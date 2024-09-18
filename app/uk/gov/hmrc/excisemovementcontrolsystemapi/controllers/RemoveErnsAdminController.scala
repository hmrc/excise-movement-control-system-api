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
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.RemoveErnsAdminController.{RemoveErnsRequest, RemoveErnsResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RemoveErnsAdminController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  ernSubmissionRepository: ErnSubmissionRepository
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  private val permission = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("admin/erns")),
    IAAction("ADMIN")
  )

  private val authorised = auth.authorizedAction(
    predicate = permission,
    retrieval = Retrieval.EmptyRetrieval
  )

  def removeErns(): Action[JsValue] =
    authorised.async(parse.json) { implicit request =>
      withJsonBody[RemoveErnsRequest] { removeRequest =>
        for {
          total      <- ernSubmissionRepository.removeErns(removeRequest.erns.toSeq)
          persisting <- ernSubmissionRepository.findErns(removeRequest.erns.toSeq)
        } yield Ok(Json.toJson(RemoveErnsResponse(total, persisting.toSet)))
      }
    }
}

object RemoveErnsAdminController {

  final case class RemoveErnsRequest(erns: Set[String])
  object RemoveErnsRequest {

    implicit lazy val format: OFormat[RemoveErnsRequest] = Json.format

  }

  final case class RemoveErnsResponse(total: Int, persisting: Set[String])
  object RemoveErnsResponse {

    implicit lazy val format: OFormat[RemoveErnsResponse] = Json.format

  }
}
