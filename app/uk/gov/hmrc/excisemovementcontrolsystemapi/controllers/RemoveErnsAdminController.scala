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

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.RemoveErnsAdminController.{ErnSummary, RemoveErnsRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ErnRetrievalRepository, ErnSubmissionRepository, MovementRepository}
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RemoveErnsAdminController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  ernSubmissionRepository: ErnSubmissionRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  movementRepository: MovementRepository,
  boxIdRepository: BoxIdRepository
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
        ernSubmissionRepository
          .removeErns(removeRequest.erns.toSeq)
          .map(_ => NoContent)
      }
    }

  def findAllReferencesToErn(ern: String): Action[AnyContent] = {

    def getConsignorIds(movements: Seq[Movement]): Seq[String] =
      movements.filter(m => m.consignorId == ern).map(m => m._id)

    def getConsigneeIds(movements: Seq[Movement]): Seq[String] =
      movements.filter(m => m.consigneeId.contains(ern)).map(m => m._id)

    def getMessageRecipientIds(movements: Seq[Movement]): Seq[(String, Seq[String])] = {
      val filteredMovements = movements.filter(m => m.messages.exists(mess => mess.recipient == ern))
      val ids               = filteredMovements.map(m =>
        (m._id, m.messages.filter(mess => mess.recipient == ern).map(mess => mess.messageId))
      )
      ids
    }

    authorised.async { implicit request =>
      for {
        ernSubmissions <- ernSubmissionRepository.findErns(Seq(ern))
        lastRetrieved  <- ernRetrievalRepository.getLastRetrieved(ern)
        boxIdsRecords  <- boxIdRepository.getBoxIds(ern)
        movements      <- movementRepository.getMovementByERN(Seq(ern))
        consignors      = getConsignorIds(movements)
        consignees      = getConsigneeIds(movements)
        recipients      = getMessageRecipientIds(movements)
      } yield Ok(
        Json.toJson(ErnSummary(ern, consignors, consignees, recipients, ernSubmissions, lastRetrieved, boxIdsRecords))
      )
    }
  }

}
object RemoveErnsAdminController {

  final case class RemoveErnsRequest(erns: Set[String])
  object RemoveErnsRequest {

    implicit lazy val format: OFormat[RemoveErnsRequest] = Json.format

  }

  final case class ErnSummary(
    ern: String,
    asConsignor: Seq[String],
    asConsignee: Seq[String],
    asRecipient: Seq[(String, Seq[String])],
    ernSubmissions: Seq[String],
    lastRetrieved: Option[Instant],
    boxIdRecords: Set[String]
  )
  object ErnSummary {
    implicit lazy val format: OFormat[ErnSummary] = Json.format
  }
}
