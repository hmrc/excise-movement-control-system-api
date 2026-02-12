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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MovementRepository, TransformLogRepository, TransformationRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AdminDetailsController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  movementRepository: MovementRepository,
  transformLogRepository: TransformLogRepository
)(implicit
  ec: ExecutionContext
) extends BackendController(cc) {
  private val permission = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("admin/movement")),
    IAAction("ADMIN")
  )

  private val authorised = auth.authorizedAction(
    predicate = permission,
    retrieval = Retrieval.EmptyRetrieval
  )

  def getMovementDetails(movementId: String): Action[AnyContent] =
    authorised.async {
      movementRepository.getMovementById(movementId).map { m =>
        m match {
          case Some(movement) => Ok(Json.toJson(MovementDetails.createFromMovement(movement)))
          case None           => NotFound(s"No Movement Found with id: $movementId")
        }
      }
    }

  def getTransformLogDetails(movementId: String): Action[AnyContent] =
    authorised.async {
      transformLogRepository.findLog(movementId).map {
        case Some(transformLog) => Ok(Json.toJson(transformLog))
        case None               => NotFound(s"No Transform Log Found with id: $movementId")
      }
    }

}

case class MovementDetails(
  id: String,
  boxId: Option[String],
  lrn: String,
  consignorId: String,
  consigneeId: Option[String],
  arc: Option[String],
  lastUpdated: Instant,
  messageDetails: Seq[MessageDetails]
)

object MovementDetails {
  def createFromMovement(m: Movement): MovementDetails =
    MovementDetails(
      m._id,
      m.boxId,
      m.localReferenceNumber,
      m.consignorId,
      m.consigneeId,
      m.administrativeReferenceCode,
      m.lastUpdated,
      MessageDetails.createFromMovement(m)
    )

  implicit lazy val format: OFormat[MovementDetails] = Json.format
}
case class MessageDetails(id: String, messageType: String, recipient: String, boxes: Set[String], createdOn: Instant)

object MessageDetails {
  def createFromMovement(movement: Movement): Seq[MessageDetails] =
    movement.messages.map { m =>
      MessageDetails(m.messageId, m.messageType, m.recipient, m.boxesToNotify, m.createdOn)
    }
  implicit lazy val format: OFormat[MessageDetails]               = Json.format

}
