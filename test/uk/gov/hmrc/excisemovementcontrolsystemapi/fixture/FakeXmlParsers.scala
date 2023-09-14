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

import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.ParseIE815XmlAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{AuthorizedIE815Request, AuthorizedRequest}

import scala.concurrent.{ExecutionContext, Future}

trait FakeXmlParsers {
  object FakeSuccessIE815XMLParser extends ParseIE815XmlAction {
    override def refine[A](request: AuthorizedRequest[A]): Future[Either[Result, AuthorizedIE815Request[A]]] = {
      Future.successful(Right(AuthorizedIE815Request(AuthorizedRequest(request, Set.empty, "123"),null,"123")))
    }

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  }

  object FakeFailureIE815XMLParser extends ParseIE815XmlAction {
    override def refine[A](request: AuthorizedRequest[A]): Future[Either[Result, AuthorizedIE815Request[A]]] = {
      Future.successful(Left(BadRequest("Invalid xml supplied")))
    }

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  }

}
