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
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.ParseJsonAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.{ParsedPreValidateTraderETDSRequest, ParsedPreValidateTraderRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderETDSRequest, getPreValidateTraderRequest}

import scala.concurrent.{ExecutionContext, Future}

trait FakeJsonParsers {
  object FakeSuccessJsonParser extends ParseJsonAction {

    override def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedPreValidateTraderRequest[A]]] =
      Future.successful(
        Right(
          preValidateTrader.request.ParsedPreValidateTraderRequest(
            EnrolmentRequest(request, Set("ern123"), UserDetails("123", "abc")),
            getPreValidateTraderRequest
          )
        )
      )

    override def refineETDS[A](
      request: EnrolmentRequest[A]
    ): Future[Either[Result, ParsedPreValidateTraderETDSRequest[A]]] = Future.successful(
      Right(
        preValidateTrader.request.ParsedPreValidateTraderETDSRequest(
          EnrolmentRequest(request, Set("ern123"), UserDetails("123", "abc")),
          getPreValidateTraderETDSRequest
        )
      )
    )

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  }

  object FakeFailureJsonParser extends ParseJsonAction {
    override def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedPreValidateTraderRequest[A]]] =
      Future.successful(Left(BadRequest("error")))

    override def refineETDS[A](
      request: EnrolmentRequest[A]
    ): Future[Either[Result, ParsedPreValidateTraderETDSRequest[A]]] =
      Future.successful(Left(BadRequest("error")))

    override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  }

}
