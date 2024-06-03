/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.mvc.{ActionFilter, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ValidateUpdatedSinceActionImpl @Inject()
(dateTimeService: DateTimeService,
cc: ControllerComponents
)(implicit val ec: ExecutionContext)
  extends BackendController(cc)
    with ValidateUpdatedSinceAction {
  override def apply(updatedSince: Option[String]): ActionFilter[EnrolmentRequest] =
    new ActionFilter[EnrolmentRequest] {
      override val executionContext: ExecutionContext = ec

      override def filter[A](request: EnrolmentRequest[A]): Future[Option[Result]] = Future.successful {
        updatedSince.flatMap( value =>
            Try(Instant.parse(value)) match {
              case Success(_) => None
              case Failure(_) => Some(badRequestResponse())
            }
        )

      }
    }

  private def badRequestResponse(): Result =
    BadRequest(Json.toJson(ErrorResponse(
      dateTimeService.timestamp(),
      "Invalid date format provided in the updatedSince query parameter",
      "Date format should be like '2020-11-15T17:02:34.00Z'")
    ))
}

@ImplementedBy(classOf[ValidateUpdatedSinceActionImpl])
trait ValidateUpdatedSinceAction {
  def apply(updatedSince: Option[String]): ActionFilter[EnrolmentRequest]

}
