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
import org.mockito.Mockito.times
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{InternalServerError, NotFound}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeJsonParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PreValidateTraderService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
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
  private val dateTimeService       = mock[DateTimeService]
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

    "return 200 when validation fails downstream" in {

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

  "determineTraderType" should {

    "return Some(1) when Warehouse Keeper (WK) and validTrader is true" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBWK123123123", true)
      result mustBe Some("1")
    }
    "return None when Warehouse Keeper (WK) and validTrader is false" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBWK123123123", false)
      result mustBe None
    }
    "return Some(2) when Tax Warehouse (00)and validTrader is true" in {
      val result = createWithSuccessfulAuth.determineTraderType("GB00123123123", true)
      result mustBe Some("2")
    }
    "return None when Tax Warehouse (00)and validTrader is false" in {
      val result = createWithSuccessfulAuth.determineTraderType("GB00123123123", false)
      result mustBe None
    }
    "return Some(3) when Registered Consignor (RC)and validTrader is true" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBRC123123123", true)
      result mustBe Some("3")
    }
    "return None when Registered Consignor (RC)and validTrader is false" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBRC123123123", false)
      result mustBe None
    }
    "return Some(4) when Registered Consignee (RT) and validTrader is true" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBRT123123123", true)
      result mustBe Some("4")
    }
    "return None when Registered Consignee (RT) and validTrader is false" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBRT123123123", false)
      result mustBe None
    }
    "return Some(5) when Temporary Registered Consignee (TC) and validTrader is true" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBTC123123123", true)
      result mustBe Some("5")
    }
    "return None when Temporary Registered Consignee (TC) and validTrader is false" in {
      val result = createWithSuccessfulAuth.determineTraderType("GBTC123123123", false)
      result mustBe None
    }
    //TODO: Clarify this and correct test
    "return Some(6) when ????? and validTrader is true" ignore {
      val result = createWithSuccessfulAuth.determineTraderType("GB??123123123", false)
      result mustBe Some("6")
    }
    "return None when ????? and validTrader is false" ignore {
      val result = createWithSuccessfulAuth.determineTraderType("GB??123123123", false)
      result mustBe None
    }

    "return Some(7) as TraderType when validTrader is true with test ern" in {
      val result = createWithSuccessfulAuth.determineTraderType(ern, true)

      result mustBe Some("7")
    }

    "return no TraderType when validTrader is false" in {
      val result = createWithSuccessfulAuth.determineTraderType(ern, false)

      result mustBe None
    }
  }

  "submit (ETDS)" should {

    "return 200 when validated and match original response using new service" in {

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Right(getExciseTraderValidationETDSResponse)))
      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      val result = createWithSuccessfulAuth.submit(ETDSrequest)

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(getPreValidateTraderSuccessResponse)
      verify(service, times(1)).submitETDSMessage(any)(any)
      verify(service, times(0)).submitMessage(any)(any)
    }

    "return 200 when validation fails downstream" in {

      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Right(getPreValidateTraderETDSMessageResponseAllFail)))

      val result = createWithSuccessfulAuth.submit(ETDSrequest)

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(getPreValidateTraderErrorResponse)
      verify(service, times(1)).submitETDSMessage(any)(any)
      verify(service, times(0)).submitMessage(any)(any)
    }

    "send a request to EIS" in {

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Right(getExciseTraderValidationETDSResponse)))
      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      await(createWithSuccessfulAuth.submit(ETDSrequest))

      verify(service).submitETDSMessage(any)(any)

    }

    "return a 400 error when NOT_FOUND" in {

      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Left(NotFound("not found"))))

      val result = createWithSuccessfulAuth.submit(ETDSrequest)

      status(result) mustBe NOT_FOUND
    }

    "return a 500 error when INTERNAL_SERVER_ERROR" in {

      when(appConfig.etdsPreValidateTraderEnabled).thenReturn(true)

      when(service.submitETDSMessage(any)(any))
        .thenReturn(Future.successful(Left(InternalServerError("not found"))))

      val result = createWithSuccessfulAuth.submit(ETDSrequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
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
