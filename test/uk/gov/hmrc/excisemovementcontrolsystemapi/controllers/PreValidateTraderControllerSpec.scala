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

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.NotFound
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeJsonParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PreValidateTraderService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils._

import scala.concurrent.{ExecutionContext, Future}

class PreValidateTraderControllerSpec
    extends PlaySpec
    with FakeAuthentication
    with FakeJsonParsers
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys: ActorSystem     = ActorSystem("DraftExciseMovementControllerSpec")
  private val service               = mock[PreValidateTraderService]
  private val appConfig             = mock[AppConfig]
  private val cc                    = stubControllerComponents()
  private val request               = createRequest(Json.toJson(getPreValidateTraderRequest))
  private val ETDSrequest           = createRequest(Json.toJson(getPreValidateTraderETDSRequest))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(service)
  }

  "submit" should {

    "return 200 when validated" in {

      when(service.submitMessage(any)(any)).thenReturn(Future.successful(Right(getPreValidateTraderSuccessResponse)))
      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(false)

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(getPreValidateTraderSuccessResponse)

    }

    "return 200 when business error" in {

      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(false)

      when(service.submitMessage(any)(any)).thenReturn(Future.successful(Right(getPreValidateTraderErrorResponse)))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(getPreValidateTraderErrorResponse)

    }

    "send a request to EIS" in {

      when(service.submitMessage(any)(any)).thenReturn(Future.successful(Right(getPreValidateTraderSuccessResponse)))
      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(false)

      await(createWithSuccessfulAuth.submit(request))

      verify(service).submitMessage(any)(any)

    }

    "return an error when EIS error" in {

      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(false)

      when(service.submitMessage(any)(any))
        .thenReturn(Future.successful(Left(NotFound("not found"))))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe NOT_FOUND
    }

    "a validation parser error" when {
      "xml cannot be parsed" in {
        val result = createWithFailingJsonParserAction.submit(request)

        status(result) mustBe BAD_REQUEST
      }
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.submit(request)

        status(result) mustBe FORBIDDEN
      }
    }

  }

  "submit (ETDS)" should {

    "return 200 when validated" in {

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Right(getExciseTraderValidationETDSResponse)))
      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      val result = createWithSuccessfulAuth.submit(ETDSrequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(getPreValidateTraderSuccessETDSEISResponse)

    }

    "return 400 when business error" in {

      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Right(getPreValidateTraderErrorETDSEISResponse400)))

      val result = createWithSuccessfulAuth.submit(ETDSrequest)

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(getPreValidateTraderErrorETDSEISResponse400)

    }

    "send a request to EIS" in {

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Right(getExciseTraderValidationETDSResponse)))
      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      await(createWithSuccessfulAuth.submit(ETDSrequest))

      verify(service).submitETDSMessage(any)(any)

    }

    "return an error when EIS error" in {

      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Left(NotFound("not found"))))

      val result = createWithSuccessfulAuth.submit(ETDSrequest)

      status(result) mustBe NOT_FOUND
    }

    "a validation parser error" when {
      "xml cannot be parsed" in {
        val result = createWithFailingJsonParserAction.submit(ETDSrequest)

        status(result) mustBe BAD_REQUEST
      }
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.submit(ETDSrequest)

        status(result) mustBe FORBIDDEN
      }
    }

  }

  private def createWithAuthActionFailure =
    new PreValidateTraderController(
      FakeFailingAuthentication,
      FakeSuccessJsonParser,
      service,
      cc,
      appConfig
    )

  private def createWithFailingJsonParserAction =
    new PreValidateTraderController(
      FakeSuccessAuthentication(Set(ern)),
      FakeFailureJsonParser,
      service,
      cc,
      appConfig
    )

  private def createWithSuccessfulAuth =
    new PreValidateTraderController(
      FakeSuccessAuthentication(Set(ern)),
      FakeSuccessJsonParser,
      service,
      cc,
      appConfig
    )

  private def createRequest(body: JsValue): FakeRequest[JsValue] =
    FakeRequest("POST", "/foo")
      .withHeaders(FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/json")))
      .withBody(body)
}
