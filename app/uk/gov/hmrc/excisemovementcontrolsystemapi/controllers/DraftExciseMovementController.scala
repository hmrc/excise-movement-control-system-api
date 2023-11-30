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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction, ValidateErnsAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ExciseMovementResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementController @Inject()(
                                               authAction: AuthAction,
                                               xmlParser: ParseXmlAction,
                                               validateErnsAction: ValidateErnsAction,
                                               movementMessageConnector: EISSubmissionConnector,
                                               movementMessageService: MovementService,
                                               workItemService: WorkItemService,
                                               cc: ControllerComponents
                                             )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with Logging {

  def submit: Action[NodeSeq] =
    (authAction andThen xmlParser andThen validateErnsAction).async(parse.xml) {
      implicit request: ValidatedXmlRequest[NodeSeq] =>
        movementMessageConnector.submitMessage(request).flatMap {
          case Right(_) => handleSuccess
          case Left(error) => Future.successful(error)
        }
    }


  private def handleSuccess(implicit request: ValidatedXmlRequest[NodeSeq]): Future[Result] = {

    val ieMessage = request.parsedRequest.ieMessage
    val newMovement = ieMessage match {
      case x: IE815Message => Movement(
        x.localReferenceNumber,
        x.consignorId,
        x.consigneeId,
        Some(generateRandomArc)
      )
      case _ => throw new Exception("invalid message sent to draft excise movement controller")
    }
    val ern = newMovement.consignorId

    addWorkItem(ern)

    movementMessageService.saveNewMovement(newMovement)
      .flatMap {
        case Right(msg) => Future.successful(Accepted(Json.toJson(ExciseMovementResponse("Accepted", msg.localReferenceNumber, msg.consignorId, msg.consigneeId))))
        case Left(error) => Future.successful(error)
      }

  }

  private def addWorkItem(ern: String): Future[Boolean] = {
    try {
      workItemService.addWorkItemForErn(ern, fastMode = true).flatMap { _ => Future.successful(true) }
        .recover {
          case ex: Throwable =>
            logger.error(s"[DraftExciseMovementController] - Failed to create Work Item for ERN $ern: ${ex.getMessage}")
            false
        }
    }
    catch {
      case ex: Exception => logger.error(s"[DraftExciseMovementController] - Database error while creating Work Item for ERN $ern: ${ex.getMessage}")
        Future.successful(false)
    }
  }

  //todo: this will be removed at a later time when we will do the polling
  private def generateRandomArc = {
    val rand = new scala.util.Random

    val digit = rand.nextInt(10).toString + rand.nextInt(10).toString
    val letters = rand.alphanumeric.dropWhile(_.isDigit).take(2).toList.mkString.toUpperCase
    val alphaNumeric = rand.alphanumeric.take(16).toList.mkString.toUpperCase()
    val number = rand.nextInt(10)

    s"$digit$letters$alphaNumeric$number"


  }
}
