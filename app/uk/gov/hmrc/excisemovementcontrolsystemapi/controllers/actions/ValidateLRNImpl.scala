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
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateLRNImpl @Inject()
(
  val movementService: MovementService,
  val emcsUtils: EmcsUtils,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
  extends BackendController(cc)
    with ValidateLRNAction
    with Logging {

  override def apply(lrn: String): ActionRefiner[ParsedXmlRequest, ParsedXmlRequest] =
    new ActionRefiner[ParsedXmlRequest, ParsedXmlRequest] {

      override val executionContext = ec

      override def refine[A](request: ParsedXmlRequest[A]): Future[Either[Result, ParsedXmlRequest[A]]] = {

        movementService.getMovementMessagesByLRNAndERNIn(lrn, request.erns.toList).map {
          case Some(_) => Right(request)
          case _ => Left(NotFoundErrorResponse(lrn)(request))
        }
      }

    }



  private def NotFoundErrorResponse[A](lrn: String)(implicit request: ParsedXmlRequest[A]): Result = {
    NotFound(Json.toJson(
      ErrorResponse(
        emcsUtils.getCurrentDateTime,
        "Local reference number not found",
        s"Local reference number $lrn is not found within the data for ERNs ${request.erns.mkString("/")}"
      )
    ))
  }
}

@ImplementedBy(classOf[ValidateLRNImpl])
trait ValidateLRNAction  {
  def apply(lrn: String):ActionRefiner[ParsedXmlRequest, ParsedXmlRequest]
}
