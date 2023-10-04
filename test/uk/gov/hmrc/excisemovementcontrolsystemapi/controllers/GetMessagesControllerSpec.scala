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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateConsignorAction, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementMessageService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetMessagesControllerSpec extends PlaySpec
  with FakeAuthentication
  with FakeXmlParsers
  with FakeValidateConsignorAction
  with TestXml
  with EitherValues
  with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys = ActorSystem("GetMessagesControllerSpec")
  private val movementMessageService = mock[MovementMessageService]
  private val cc = stubControllerComponents()
  private val lrn = "LRN1234"


  "getMessagesForMovement" should {
    "return 200" in {

      val messages = Seq(Message(lrn, "IE801", Instant.now()))
      when(movementMessageService.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Right(messages)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe OK
    }

    "return 404 when no movement is found for supplied lrn" in {
      when(movementMessageService.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Left(NotFoundError())))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe NOT_FOUND
    }

    "return 500 when mongo error" in {
      when(movementMessageService.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Left(MongoError("error"))))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  private def createWithSuccessfulAuth =
    new GetMessagesController(
      FakeSuccessAuthentication,
      movementMessageService,
      cc
    )

  private def createRequest(): FakeRequest[AnyContent] = {
    FakeRequest("GET", "/foo")
  }
}
