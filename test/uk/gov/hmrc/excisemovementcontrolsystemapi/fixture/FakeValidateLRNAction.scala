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

package uk.gov.hmrc.excisemovementcontrolsystemapi.fixture

import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.ValidateLRNAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest

import scala.concurrent.{ExecutionContext, Future}

trait FakeValidateLRNAction {

  object FakeSuccessfulValidateLRNAction extends ValidateLRNAction with TestXml {

    override def apply(lrn: String): ActionRefiner[ValidatedXmlRequest, ValidatedXmlRequest] = {
      new ActionRefiner[ValidatedXmlRequest, ValidatedXmlRequest] {

        override val executionContext: ExecutionContext = ExecutionContext.Implicits.global

        override def refine[A](request: ValidatedXmlRequest[A]): Future[Either[Result, ValidatedXmlRequest[A]]] = {
          Future.successful(Right(request))
        }
      }
    }
  }

  object FakeFailureValidateLRNAction extends ValidateLRNAction {
    override def apply(lrn: String): ActionRefiner[ValidatedXmlRequest, ValidatedXmlRequest] = {
      new ActionRefiner[ValidatedXmlRequest, ValidatedXmlRequest] {

        override val executionContext: ExecutionContext = ExecutionContext.Implicits.global

        override def refine[A](request: ValidatedXmlRequest[A]): Future[Either[Result, ValidatedXmlRequest[A]]] = {
          Future.successful(Left(NotFound("Error")))
        }
      }
    }
  }
}
