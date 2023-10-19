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
import play.api.mvc.Results.{InternalServerError, NotFound}
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.DataRequestIE818
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementMessageService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateLRNImpl @Inject()(
                                 val lrn: String,
                                 val movementMessageService: MovementMessageService,
                                 implicit val executionContext: ExecutionContext,
                                 implicit val emcsUtils: EmcsUtils
                               )
  extends ValidateLRNAction
    with Logging {
  override def refine[A](request: DataRequestIE818[A]): Future[Either[Result, DataRequestIE818[A]]] = {

    movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, request.erns.toList).flatMap {
      case Right(_) => Future.successful(Right(request))
      case Left(_: NotFoundError) => Future.successful(
        Left(
          NotFound(
            Json.toJson(
              ErrorResponse(
                emcsUtils.getCurrentDateTime,
                "Invalid LRN supplied",
                s"LRN $lrn is not valid for ERNs ${request.erns.mkString("/")}",
                emcsUtils.generateCorrelationId
              )
            )
          )
        )
      )
      case Left(error) => Future.successful(
        Left(
          InternalServerError(
            Json.toJson(
              ErrorResponse(
                emcsUtils.getCurrentDateTime,
                "Database error occurred",
                error.message,
                emcsUtils.generateCorrelationId
              )
            )
          )
        )
      )
    }

  }
}

@ImplementedBy(classOf[ValidateLRNImpl])
trait ValidateLRNAction extends ActionRefiner[DataRequestIE818, DataRequestIE818] {

  def refine[A](request: DataRequestIE818[A]): Future[Either[Result, DataRequestIE818[A]]]
}

class ValidateLRNActionFactory @Inject()(implicit val executionContext: ExecutionContext, implicit val emcsUtils: EmcsUtils) {
  def apply(lrn: String, movementMessageService: MovementMessageService): ValidateLRNAction =
    new ValidateLRNImpl(lrn, movementMessageService, executionContext, emcsUtils)
}