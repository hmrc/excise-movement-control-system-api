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
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateErnParameterActionImpl @Inject()
(
  val emcsUtils: EmcsUtils,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
  extends BackendController(cc)
    with ValidateErnParameterAction {

  override def apply(ernParameter: Option[String]): ActionFilter[EnrolmentRequest] =
    new ActionFilter[EnrolmentRequest] {

      override val executionContext: ExecutionContext = ec

      override def filter[A](request: EnrolmentRequest[A]): Future[Option[Result]] = {

        Future {

          ernParameter match {
            //Ensure if they supplied an ERN it matches one they have logged in as
            case Some(value) =>
              if (request.erns.contains(value)) {
                None
              } else {
                Some(badRequestResponse(value)(request))
              }

            //Nothing to validate
            case None => None
          }

        }

      }

    }

  private def badRequestResponse[A](ern: String)(implicit request: EnrolmentRequest[A]): Result = {
    BadRequest(Json.toJson(
      ErrorResponse(
        emcsUtils.getCurrentDateTime,
        "ERN parameter value error",
        s"The ERN $ern supplied in the parameter is not among the authorised ERNs ${request.erns.mkString("/")}"
      )
    ))
  }
}

@ImplementedBy(classOf[ValidateErnParameterActionImpl])
trait ValidateErnParameterAction {
  def apply(ern: Option[String]): ActionFilter[EnrolmentRequest]
}

