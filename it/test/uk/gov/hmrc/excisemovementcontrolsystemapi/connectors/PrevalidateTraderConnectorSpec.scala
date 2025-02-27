/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, notMatching, post, urlEqualTo}
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status.{ACCEPTED, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{EISHeaderTestSupport, StringSupport}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.{ExciseTraderETDSRequest, ExciseTraderRequest, ExciseTraderValidationRequest, PreValidateTraderRequest, ValidateProductAuthorisationRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{ExciseTraderResponse, ExciseTraderValidationETDSResponse, ExciseTraderValidationResponse, PreValidateTraderEISResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

class PrevalidateTraderConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerTest
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience {

  val seedResponse = PreValidateTraderEISResponse(
    ExciseTraderValidationResponse("timestamp", Array.empty[ExciseTraderResponse])
  )

  val etdsResponse = ExciseTraderValidationETDSResponse("timestamp", "1", "exciseId", "Success", None)

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("microservice.services.eis.port" -> wireMockPort)
      .build()

  "submitMessage" - {
    "forward the correlation id if it exists" in {
      val correlationId = "abcdefg"
      val hc            = HeaderCarrierConverter.fromRequest(FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correlationId))
      val ern           = "ern"
      val url           = "/emcs/pre-validate-trader/v1"
      val connector     = app.injector.instanceOf[PreValidateTraderConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(seedResponse).toString())
              .withStatus(OK)
          )
      )

      val request = PreValidateTraderRequest(
        ExciseTraderValidationRequest(
          ExciseTraderRequest(ern, "entityGroup", Seq.empty[ValidateProductAuthorisationRequest])
        )
      )

      //If this succeeds the wiremock has acted as a matcher
      val result = connector.submitMessage(request, ern)(hc).futureValue.isRight mustBe true
    }
    "forward a new correlation id if not exists" in {
      val correlationId = "abcdefg"
      val ern           = "ern"
      val url           = "/emcs/pre-validate-trader/v1"
      val connector     = app.injector.instanceOf[PreValidateTraderConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(seedResponse).toString())
              .withStatus(OK)
          )
      )

      val request = PreValidateTraderRequest(
        ExciseTraderValidationRequest(
          ExciseTraderRequest(ern, "entityGroup", Seq.empty[ValidateProductAuthorisationRequest])
        )
      )

      //This should fail as the headers don't match
      val result = connector.submitMessage(request, ern)(HeaderCarrier()).futureValue.isLeft mustBe true

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(seedResponse).toString())
              .withStatus(OK)
          )
      )

      //This should succeed as the headers now don't match correlationId
      val result2 = connector.submitMessage(request, ern)(HeaderCarrier()).futureValue.isRight mustBe true

    }

  }

  "submitMessageETDS" - {
    "forward the correlation id if it exists" in {
      val correlationId = "abcdefg"
      val hc            = HeaderCarrierConverter.fromRequest(FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correlationId))
      val ern           = "ern"
      val url           = "/etds/traderprevalidation/v1"
      val connector     = app.injector.instanceOf[PreValidateTraderConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(etdsResponse).toString())
              .withStatus(OK)
          )
      )

      val request = ExciseTraderETDSRequest(ern, "entityGroup", None)

      //If this succeeds the wiremock has acted as a matcher
      val result = connector.submitMessageETDS(request, ern)(hc).futureValue.isRight mustBe true
    }
    "forward a new correlation id if not exists" in {
      val correlationId = "abcdefg"
      val ern           = "ern"
      val url           = "/etds/traderprevalidation/v1"
      val connector     = app.injector.instanceOf[PreValidateTraderConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(etdsResponse).toString())
              .withStatus(OK)
          )
      )

      val request = ExciseTraderETDSRequest(ern, "entityGroup", None)

      //This should fail as the headers don't match
      val result = connector.submitMessageETDS(request, ern)(HeaderCarrier()).futureValue.isLeft mustBe true

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(etdsResponse).toString())
              .withStatus(OK)
          )
      )

      //This should succeed as the headers now don't match correlationId
      val result2 = connector.submitMessageETDS(request, ern)(HeaderCarrier()).futureValue.isRight mustBe true

    }
  }
}
