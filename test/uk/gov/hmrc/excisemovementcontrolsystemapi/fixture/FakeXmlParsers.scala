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

import generated.IE815Type
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.ParseXmlAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequestCopy}

import scala.concurrent.{ExecutionContext, Future}

trait FakeXmlParsers {
  case class FakeSuccessIE815XMLParser(ieMessage: IE815Type) extends ParseXmlAction {
    override def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedXmlRequestCopy[A]]] = {
      Future.successful(Right(ParsedXmlRequestCopy(EnrolmentRequest(request, Set.empty, "123"),null, Set.empty, "123")))
    }

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  }

  object FakeFailureIE815XMLParser extends ParseXmlAction {
    override def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedXmlRequestCopy[A]]] = {
      Future.successful(Left(BadRequest("Invalid xml supplied")))
    }

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  }

}
