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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class GetMessagesController @Inject()(
                                       authAction: AuthAction,
                                       movementService: MovementService,
                                       workItemService: WorkItemService,
                                       cc: ControllerComponents,
                                       implicit val emcsUtils: EmcsUtils
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def getMessagesForMovement(lrn: String, updatedSince: Option[String]): Action[AnyContent] = {
    // todo: how we handle error here if for example MongoDb throws?
    authAction.async(parse.default) {
      implicit request: EnrolmentRequest[AnyContent] => {

        movementService.getMatchingERN(lrn, request.erns.toList).flatMap {
          case None => Future.successful(BadRequest(Json.toJson(ErrorResponse(emcsUtils.getCurrentDateTime, "Invalid LRN supplied for ERN", ""))))
          case Some(ern) => workItemService.addWorkItemForErn(ern, fastMode = false)
            getMessagesAsJson(lrn, ern, updatedSince)
        }
      }
    }
  }

  private def getMessagesAsJson(lrn: String, ern: String, updatedSince: Option[String]): Future[Result] = {


    Try(updatedSince.map(Instant.parse(_))).map { updatedSinceTime =>
      movementService.getMovementByLRNAndERNIn(lrn, List(ern))
        .map(mv => {
          mv.map(m => filterMessagesByTime(m.messages, updatedSinceTime))
        })
        .map {
          case Some(mv) => Ok(Json.toJson(mv))
          case _ => Ok(JsArray())
        }

    }.getOrElse(
      Future.successful(BadRequest(Json.toJson(ErrorResponse(emcsUtils.getCurrentDateTime, "Invalid date format provided in the updatedSince query parameter", "")))))
  }

  private def filterMessagesByTime(messages: Seq[Message], updatedSince: Option[Instant]): Seq[Message] = {
    updatedSince.fold[Seq[Message]](messages)(a =>
        messages.filter(o => o.createdOn.isAfter(a) || o.createdOn.equals(a))
      )
  }

}
