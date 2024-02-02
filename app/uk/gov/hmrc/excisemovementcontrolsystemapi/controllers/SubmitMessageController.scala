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

import cats.data.EitherT
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
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

        val result = for {
          movement <- validateMovementId(movementId)
          authorisedErn <- validateMessage(movement, request.ieMessage, request.erns)
          _ <- sendRequest(request, authorisedErn)
        } yield {
          Accepted("")
        }

        result.merge
    }

  }

  private def validateMovementId(movementId: String): EitherT[Future, Result, Movement] = {
    EitherT(movementService.validateMovementId(movementId)).leftMap {
      case x: MovementIdNotFound => NotFound(Json.toJson(
        ErrorResponse(dateTimeService.timestamp(), "Movement not found", x.errorMessage
        )))
      case x: MovementIdFormatInvalid => BadRequest(Json.toJson(
        ErrorResponse(dateTimeService.timestamp(), "Movement Id format error", x.errorMessage)
      ))
    }
  }

  private def validateMessage(
                               movement: Movement,
                               message: IEMessage,
                               authErns: Set[String]
                             ): EitherT[Future, Result, String] = {

    EitherT.fromEither(messageValidator.validateSubmittedMessage(authErns, movement, message).left.map {
      case x: MessageDoesNotMatchMovement =>
        BadRequest(Json.toJson(
          ErrorResponse(dateTimeService.timestamp(), "Message does not match movement", x.errorMessage)
        ))

      case x: MessageMissingKeyInformation =>
        BadRequest(Json.toJson(
          ErrorResponse(dateTimeService.timestamp(), "Message missing key information", x.errorMessage)
        ))

      case x: MessageTypeInvalid =>
        BadRequest(Json.toJson(
          ErrorResponse(dateTimeService.timestamp(), "Message type is invalid", x.errorMessage)
        ))

      case x: MessageIdentifierIsUnauthorised =>
        Forbidden(Json.toJson(
          ErrorResponse(dateTimeService.timestamp(), "Message cannot be sent", x.errorMessage)
        ))
    })


  }

  private def sendRequest(request: ParsedXmlRequest[_], authorisedErn: String)
                         (implicit hc: HeaderCarrier) : EitherT[Future, Result, EISSubmissionResponse] = {
    workItemService.addWorkItemForErn(authorisedErn, fastMode = true)

    EitherT(submissionMessageService.submit(request, authorisedErn))

  }

}
