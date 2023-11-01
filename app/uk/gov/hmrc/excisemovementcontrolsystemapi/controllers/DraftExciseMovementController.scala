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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction, ValidateConsignorAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ExciseMovementResponse, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.DataRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementController @Inject()(
                                               authAction: AuthAction,
                                               xmlParser: ParseXmlAction,
                                               consignorValidatorAction: ValidateConsignorAction,
                                               movementMessageConnector: EISSubmissionConnector,
                                               movementMessageService: MovementService,
                                               cc: ControllerComponents
                                             )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def submit: Action[NodeSeq] =
    (authAction andThen xmlParser andThen consignorValidatorAction).async(parse.xml) {
      implicit request: DataRequest[NodeSeq] =>
        movementMessageConnector.submitExciseMovement(request, MessageTypes.IE815.value).flatMap {
          case Right(_) => handleSuccess
          case Left(error) => Future.successful(error)
        }
    }


  private def handleSuccess(implicit request: DataRequest[NodeSeq]): Future[Result] = {

    val newMovement = request.movementMessage.copy(administrativeReferenceCode = Some(generateRandomArc))
    movementMessageService.saveMovementMessage(newMovement)
      .flatMap {
        case Right(msg) => Future.successful(Accepted(Json.toJson(ExciseMovementResponse("Accepted", msg.localReferenceNumber, msg.consignorId, msg.consigneeId))))
        case Left(error) => Future.successful(InternalServerError(error.message))
      }
  }

  //todo: this will be removed at a later time when we will do the polling
  private def generateRandomArc = {
    val rand = new scala.util.Random

    val digit = rand.nextInt(10).toString + rand.nextInt(10).toString
    val letters = rand.alphanumeric.dropWhile(_.isDigit).take(2).toList.mkString
    val alphaNumeric = rand.alphanumeric.take(16).toList.mkString
    val number = rand.nextInt(10)

    s"$digit$letters$alphaNumeric$number"


  }
}
