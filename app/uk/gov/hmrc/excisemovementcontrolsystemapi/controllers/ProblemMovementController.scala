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
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.emcsmovementinjectorfrontend.models.FixMovementRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MovementRepository, MovementWorkItem, ProblemMovementsWorkItemRepo}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import uk.gov.hmrc.internalauth.client.{AuthenticatedRequest, BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProblemMovementController @Inject()(
    cc: ControllerComponents,
    movementRepository: MovementRepository,
    workItemRepository: ProblemMovementsWorkItemRepo,
    auth: BackendAuthComponents,
    messageService: MessageService
  )(implicit ec: ExecutionContext)
    extends BackendController(cc)
      with Logging {

  private val permission = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("problem-movements")),
    IAAction("ADMIN")
  )

  def getMovementsWithTooMany801s(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
      movementRepository.getProblemMovements().map(movements => Ok(Json.toJson(movements)))
    }

  def getCountOfMovementsWithTooMany801s(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
      movementRepository.getCountOfProblemMovements().map(_.map(t => Ok(Json.toJson(t))).getOrElse(NotFound))
    }

  def buildWorkItemQueue(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
        movementRepository.getProblemMovements().flatMap { mm =>
          Future
            .traverse(
              mm.map(m => MovementWorkItem(m._id)).grouped(250)
            )(g => workItemRepository.pushNewBatch(g))
            .map(_ => Ok)
        }
      }

  def resolveProblemMovement(): Action[FixMovementRequest] =
    auth.authorizedAction(permission, EmptyRetrieval).async(parse.json.map(_.as[FixMovementRequest])) { implicit request =>
      messageService.archiveAndFixProblemMovement(request.body.movementId)
        .map(_ => NoContent)
    }
}
