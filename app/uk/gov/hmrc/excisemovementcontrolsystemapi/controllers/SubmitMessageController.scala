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

import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation._
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class SubmitMessageController @Inject()(
                                         authAction: AuthAction,
                                         xmlParser: ParseXmlAction,
                                         submissionMessageService: SubmissionMessageService,
                                         movementService: MovementService,
                                         workItemService: WorkItemService,
                                         messageValidator: MessageValidation,
                                         dateTimeService: DateTimeService,
                                         cc: ControllerComponents
                                       )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit(movementId: String): Action[NodeSeq] = {

    (authAction andThen xmlParser).async(parse.xml) {
      implicit request =>

        val movement = movementService.validateMovementId(movementId)

        movement.map {
          case Left(error) => error match {
            case x: MovementIdNotFound => Future.successful(NotFound(Json.toJson(
              ErrorResponse(dateTimeService.timestamp(), "Movement not found", x.errorMessage)
            )))
            case x: MovementIdFormatInvalid => Future.successful(BadRequest(Json.toJson(
              ErrorResponse(dateTimeService.timestamp(), "Movement Id format error", x.errorMessage)
            )))
          }

          case Right(movement) =>
            val b = messageValidator.validateSubmittedMessage(request.erns, movement, request.ieMessage)
            val c: Either[MessageValidationResponse, Future[Result]] = b.map {
              ern =>
                workItemService.addWorkItemForErn(ern, fastMode = true)

                val a: Future[Result] = submissionMessageService.submit(request, ern).flatMap {
                  case Right(_) => Future.successful(Accepted(""))
                  case Left(error) => Future.successful(error)
                }

                a
            }

            val d: Either[Future[Result], Future[Result]] = c.left.map {
              case x: MessageDoesNotMatchMovement =>
                Future.successful(BadRequest(Json.toJson(
                  ErrorResponse(dateTimeService.timestamp(), "Message does not match movement", x.errorMessage)
                )))
              case x: MessageMissingKeyInformation =>
                Future.successful(BadRequest(Json.toJson(
                  ErrorResponse(dateTimeService.timestamp(), "Message missing key information", x.errorMessage)
                )))

              case x: MessageTypeInvalid =>
                Future.successful(BadRequest(Json.toJson(
                  ErrorResponse(dateTimeService.timestamp(), "Message type is invalid", x.errorMessage)
                )))

              case x: MessageIdentifierIsUnauthorised =>
                Future.successful(Forbidden(Json.toJson(
                  ErrorResponse(dateTimeService.timestamp(), "Message cannot be sent", x.errorMessage)
                )))

            }

            d.merge

        }.flatten

    }

  }

}
