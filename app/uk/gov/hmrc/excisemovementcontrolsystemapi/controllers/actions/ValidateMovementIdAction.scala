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
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{ActionFilter, ActionRefiner, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{EmcsUtils, ErnsMapper}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateMovementIdActionImpl @Inject()
(
  val movementService: MovementService,
  val emcsUtils: EmcsUtils,
  val ernMappers: ErnsMapper,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
  extends BackendController(cc)
    with ValidateLRNAction
    with Logging {

  override def apply(id: String): ActionFilter[ValidatedXmlRequest] =
    new ActionFilter[ValidatedXmlRequest] {

      override val executionContext: ExecutionContext = ec

      override def filter[A](request: ValidatedXmlRequest[A]): Future[Option[Result]] = {

      //  Future.successful(None)

        movementService.getMovementById(id).map {
          case Some(_) =>
            //TODO can we get away with this?
          val ernForMessage = ernMappers.getSingleErnFromMessage(request.message, request.validErns)

            if (request.validErns.contains(ernForMessage)) {
              None
            } else {
              Some(NotFoundErrorResponse(id))
            }

          case None => Some(NotFoundErrorResponse(id))
        }

       // movementService.getMovementByLRNAndERNIn(lrn, request.validErns.toList).map {
       //   case Some(_) => Right(request)
     //     case _ => Left(NotFoundErrorResponse(lrn)(request))
     //   }
      }

    }


  private def NotFoundErrorResponse[A](id: String): Result = {
    NotFound(Json.toJson(
      ErrorResponse(
        emcsUtils.getCurrentDateTime,
        "Movement not found",
        s"Movement $id is not found within the data"
      )
      //TODO put erns back in here
    ))
  }
}

@ImplementedBy(classOf[ValidateMovementIdActionImpl])
trait ValidateMovementIdAction  {
  def apply(id: String):ActionRefiner[ValidatedXmlRequest, ValidatedXmlRequest]
}
