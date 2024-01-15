/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import com.google.inject.ImplementedBy
import play.api.libs.json.Json
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateMovementIdActionImpl @Inject()
(
  val movementService: MovementService,
  val emcsUtils: EmcsUtils
)(implicit val ec: ExecutionContext)
  extends ValidateMovementIdAction {

  override def apply(id: String): ActionFilter[ValidatedXmlRequest] =
    new ActionFilter[ValidatedXmlRequest] {

      override val executionContext: ExecutionContext = ec

      override def filter[A](request: ValidatedXmlRequest[A]): Future[Option[Result]] = {

        val authorisedErns = request.validErns

        movementService.getMovementById(id).map {
          case Some(movement) =>

            val ernsForMovement = getErnsForMovement(movement)

            if (authorisedErns.intersect(ernsForMovement).isEmpty) {
              Some(NotFoundErrorResponse(id, authorisedErns))
            } else {
              None
            }

          case None => Some(NotFoundErrorResponse(id, authorisedErns))
        }

      }

    }


  private def NotFoundErrorResponse[A](id: String, authorisedErns: Set[String]): Result = {
    NotFound(Json.toJson(
      ErrorResponse(
        emcsUtils.getCurrentDateTime,
        "Movement not found",
        s"Movement $id is not found within the data for ERNs ${authorisedErns.mkString("/")}"
      )
    ))
  }

  private def getErnsForMovement(movement: Movement): Set[String] = {
    Set(Some(movement.consignorId), movement.consigneeId).flatten
  }
}

@ImplementedBy(classOf[ValidateMovementIdActionImpl])
trait ValidateMovementIdAction {
  def apply(id: String): ActionFilter[ValidatedXmlRequest]
}
