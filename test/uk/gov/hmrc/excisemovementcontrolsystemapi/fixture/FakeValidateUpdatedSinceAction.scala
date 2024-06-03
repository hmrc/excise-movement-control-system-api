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

package uk.gov.hmrc.excisemovementcontrolsystemapi.fixture

import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.ValidateUpdatedSinceAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait FakeValidateUpdatedSinceAction {

  object FakeValidateUpdatedSinceSuccessAction extends ValidateUpdatedSinceAction {
    override def apply(ern: Option[String]): ActionFilter[EnrolmentRequest] = {
      new ActionFilter[EnrolmentRequest] {

        override val executionContext: ExecutionContext = ExecutionContext.Implicits.global

        override def filter[A](request: EnrolmentRequest[A]): Future[Option[Result]] = {
          Future.successful(None)
        }
      }
    }
  }

  object FakeValidateUpdatedSinceFailureAction extends ValidateUpdatedSinceAction{
    override def apply(ern: Option[String]): ActionFilter[EnrolmentRequest] = {
      new ActionFilter[EnrolmentRequest] {

        override val executionContext: ExecutionContext = ExecutionContext.Implicits.global

        override def filter[A](request: EnrolmentRequest[A]): Future[Option[Result]] = {
          Future.successful(Some(BadRequest(Json.toJson(ErrorResponse(
            Instant.parse("2020-01-01T01:01:01.123Z"),
            "Invalid date format provided in the updatedSince query parameter",
            "Date format should be like '2020-11-15T17:02:34.00Z'")
          ))))
        }
      }
    }  }

}
