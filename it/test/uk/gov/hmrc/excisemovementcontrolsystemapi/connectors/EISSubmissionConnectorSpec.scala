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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.IE815MessageV1
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{ExciseTraderResponse, ExciseTraderValidationETDSResponse, ExciseTraderValidationResponse, PreValidateTraderEISResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

class EISSubmissionConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerTest
    with MockitoSugar
    with ScalaFutures
    with TestXml
    with IntegrationPatience {

  val response = EISSubmissionResponse("status", "message", "emcsCorrelationId")

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("microservice.services.eis.port" -> wireMockPort)
      .build()

  "submitMessage" - {
    "forward the correlation id if it exists" in {
      val correlationId = "abcdefg"
      val hc            = HeaderCarrierConverter.fromRequest(FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correlationId))
      val ern           = "ern"
      val url           = "/emcs/digital-submit-new-message/v1"
      val connector     = app.injector.instanceOf[EISSubmissionConnector]

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //If this succeeds the wiremock has acted as a matcher
      connector.submitMessage(IE815MessageV1.createFromXml(IE815), "xmlAsString", ern)(hc).futureValue.isRight mustBe true
    }
    "forward a new correlation id if not exists" in {
      val correlationId = "abcdefg"
      val ern           = "ern"
      val url           = "/emcs/digital-submit-new-message/v1"
      val connector     = app.injector.instanceOf[EISSubmissionConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //This should fail as the headers don't match
      val result = connector
        .submitMessage(IE815MessageV1.createFromXml(IE815), "xmlAsString", ern)(HeaderCarrier())
        .futureValue
        .isLeft mustBe true

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //This should succeed as the headers now don't match correlationId
      val result2 = connector
        .submitMessage(IE815MessageV1.createFromXml(IE815), "xmlAsString", ern)(HeaderCarrier())
        .futureValue
        .isRight mustBe true

    }

  }
}
