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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class GetMessagesController @Inject()(
                                       authAction: AuthAction,
                                       movementService: MovementService,
                                       workItemService: WorkItemService,
                                       cc: ControllerComponents,
                                       dateTimeService: DateTimeService
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def getMessagesForMovement(movementId: String, updatedSince: Option[String]): Action[AnyContent] = {
    // todo: how we handle error here if for example MongoDb throws?
    authAction.async(parse.default) {
      implicit request: EnrolmentRequest[AnyContent] => {
        Try(UUID.fromString(movementId)) match {
          case Success(_) =>

            Try(updatedSince.map(Instant.parse(_))).map { updatedSinceTime => {

              val movement = movementService.getMovementById(movementId)

              movement.map {
                case Some(mvt) =>
                  if (getErnsForMovement(mvt).intersect(request.erns).isEmpty)
                    BadRequest(Json.toJson(ErrorResponse(
                      dateTimeService.timestamp(),
                      "Invalid MovementID supplied for ERN",
                      s"Movement ${mvt._id} is not found within the data for ERNs ${request.erns.mkString("/")}"
                    ))) else {
                    workItemService.addWorkItemForErn(mvt.consignorId, fastMode = false)

                    Ok(Json.toJson(filterMessagesByTime(mvt.messages, updatedSinceTime)))
                  }
                case _ => NotFound(
                  Json.toJson(ErrorResponse(
                    dateTimeService.timestamp(),
                    "No movement found for the MovementID provided",
                    s"MovementID $movementId was not found in the database"
                  )))
              }
            }
            }.getOrElse(
              Future.successful(BadRequest(Json.toJson(ErrorResponse(dateTimeService.timestamp(), "Invalid date format provided in the updatedSince query parameter", "Date format should be like '2020-11-15T17:02:34.00Z'")))))


              case _ =>
            Future.successful(BadRequest(Json.toJson(ErrorResponse(
              dateTimeService.timestamp(),
              "Movement Id format error",
              "Movement Id should be a valid UUID"
            ))))
        }
      }
    }
  }

  private def filterMessagesByTime(messages: Seq[Message], updatedSince: Option[Instant]): Seq[Message] = {
    updatedSince.fold[Seq[Message]](messages)(a =>
      messages.filter(o => o.createdOn.isAfter(a) || o.createdOn.equals(a))
    )
  }

  private def getErnsForMovement(movement: Movement): Set[String] = {
    Set(Some(movement.consignorId), movement.consigneeId).flatten
  }

}
