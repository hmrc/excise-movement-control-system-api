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

import com.google.inject.Inject
import org.apache.pekko.Done
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.InjectController.CsvRow
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MovementRepository, MovementWorkItem, ProblemMovementsWorkItemRepo}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.mongo.workitem.WorkItem
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class InjectController @Inject() (
  cc: ControllerComponents,
  movementRepository: MovementRepository,
  workItemRepository: ProblemMovementsWorkItemRepo,
  auth: BackendAuthComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val permission = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("inject/submit")),
    IAAction("ADMIN")
  )

  def submitBatch(): Action[JsValue] =
    auth.authorizedAction(permission).async(parse.json[JsValue]) { implicit request: AuthenticatedRequest[JsValue, _] =>
      withJsonBody[List[CsvRow]] { csvRows =>
        movementRepository
          .saveMovements(csvRows.map(_.toMovement))
          .map(_ => Accepted)
      }
    }

  def getMovementsWithTooMany801s(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
      movementRepository.getProblemMovements().map(movements => Ok(Json.toJson(movements)))
    }

  def getCountOfMovementsWithTooMany801s(): Action[AnyContent] =
    auth.authorizedAction(permission).async {
      movementRepository.getCountOfProblemMovements().map(_.map(t => Ok(Json.toJson(t))).getOrElse(NotFound))
    }

  def buildWorkItemQueue(): Action[AnyContent] =
    auth
      .authorizedAction(permission)
      .async {
        movementRepository.getProblemMovements().flatMap { mm =>
          Future
            .traverse(
              mm.map(m => MovementWorkItem(m._id)).grouped(250)
            )(g => workItemRepository.pushNewBatch(g))
            .map(_ => Ok)
        }
      }
}

object InjectController {

  case class CsvRow(
    pk: Int,
    arcUk: Option[String],
    localReferenceNumber: String,
    consignorExciseNumber: String,
    sequenceNumber: Int,
    status: String,
    createdOn: Instant,
    consigneeExciseNumber: Option[String]
  ) {
    def toMovement: Movement =
      Movement(
        boxId = None,
        localReferenceNumber = localReferenceNumber,
        consignorId = consignorExciseNumber,
        consigneeId = consigneeExciseNumber,
        administrativeReferenceCode = arcUk,
        lastUpdated = createdOn,
        messages = Seq.empty
      )
  }
  object CsvRow {
    implicit val reads: Reads[CsvRow] = Json.reads[CsvRow]
  }
}
