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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.emcsmovementinjectorfrontend.models.FixMovementRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MiscodedMovementsWorkItemRepo, MovementRepository, MovementWorkItem}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MessageService, MiscodedMovementService}
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MiscodedMovementController @Inject() (
  cc: ControllerComponents,
  movementRepository: MovementRepository,
  workItemRepository: MiscodedMovementsWorkItemRepo,
  auth: BackendAuthComponents,
  miscodedMovementService: MiscodedMovementService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val permission = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("miscoded-movements")),
    IAAction("ADMIN")
  )

  def getCount(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
      movementRepository.getCountOfMiscodedMovements().map(count => Ok(Json.toJson(Json.obj("total" -> count.toInt))))
    }

  def getMovements(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
      movementRepository.getMiscodedMovements().map(movements => Ok(Json.toJson(movements)))
    }

  def buildWorkList(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
      movementRepository.getMiscodedMovements().flatMap { mm =>
        Future
          .traverse(
            mm.map(m => MovementWorkItem(m._id)).grouped(250)
          )(g => workItemRepository.pushNewBatch(g))
          .map(_ => Ok)
      }
    }

  def resolve(): Action[FixMovementRequest] =
    auth.authorizedAction(permission, EmptyRetrieval).async(parse.json[FixMovementRequest]) { implicit request =>
      miscodedMovementService
        .archiveAndRecode(request.body.movementId)
        .map(_ => NoContent)
    }
}
