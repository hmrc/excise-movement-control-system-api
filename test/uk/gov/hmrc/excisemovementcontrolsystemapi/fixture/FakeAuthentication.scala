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

import play.api.mvc.Results.Forbidden
import play.api.mvc.{AnyContent, BodyParser, Request, Result}
import play.api.test.Helpers.stubBodyParser
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest

import scala.concurrent.{ExecutionContext, Future}

trait FakeAuthentication {

  object FakeSuccessAuthentication extends AuthAction {

    override def parser: BodyParser[AnyContent] = stubBodyParser()

    override def invokeBlock[A](request: Request[A], block: EnrolmentRequest[A] => Future[Result]): Future[Result] = {
      block(EnrolmentRequest(request, Set("testErn"), "testInternalId"))
    }

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  }

  object FakeFailingAuthentication extends AuthAction {

    override def parser: BodyParser[AnyContent] = stubBodyParser()

    override def invokeBlock[A](request: Request[A], block: EnrolmentRequest[A] => Future[Result]): Future[Result] = {
      Future.successful(Forbidden("Invalid header parameters supplied"))
    }

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  }

}
