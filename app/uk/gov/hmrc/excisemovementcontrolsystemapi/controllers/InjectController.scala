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
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc.Results.EmptyContent
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.InjectController.CsvRow
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.internalauth.client.{AuthenticatedRequest, BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class InjectController @Inject() (
  cc: ControllerComponents,
  movementService: MovementService,
  ernSubmissionRepository: ErnSubmissionRepository,
  auth: BackendAuthComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  private val permission = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("/inject/submit")),
    IAAction("WRITE")
  )
  def submit(): Action[JsValue] =
    auth.authorizedAction(permission).async(parse.json[JsValue]) { implicit request: AuthenticatedRequest[JsValue, _] =>
      withJsonBody[CsvRow] { csvRow =>
        movementService.saveNewMovement(csvRow.toMovement).flatMap {
          case Left(error)     => Future.successful(error)
          case Right(movement) =>
            movement.consigneeId
              .fold(Future.successful(Accepted))(consignee =>
                ernSubmissionRepository.save(consignee).map(_ => Accepted)
              )
        }
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
