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
import org.apache.pekko.Done
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.{NrsCircuitBreaker, UnexpectedResponseException}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NrsMetadata, NrsPayload}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.ZonedDateTime
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt

class NrsConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerTest
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with NrsTestData {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.nrs.port"                       -> wireMockPort,
        "microservice.services.nrs.api-key"                    -> "some-bearer",
        "microservice.services.nrs.max-failures"               -> 1,
        "microservice.services.nrs.reset-timeout"              -> "1 second",
        "microservice.services.nrs.call-timeout"               -> "30 seconds",
        "microservice.services.nrs.max-reset-timeout"          -> "30 seconds",
        "microservice.services.nrs.exponential-backoff-factor" -> 2.0
      )
      .build()

  "sendToNrs" - {
    val correlationId = "correlationId"
    val hc            = HeaderCarrierConverter.fromRequest(FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correlationId))
    val url           = "/submission"
    val timeStamp     = ZonedDateTime.now()
    val nrsMetadata   = NrsMetadata(
      businessId = "emcs",
      notableEvent = "excise-movement-control-system",
      payloadContentType = "application/json",
      payloadSha256Checksum = sha256Hash("payload for NRS"),
      userSubmissionTimestamp = timeStamp.toString,
      identityData = testNrsIdentityData,
      userAuthToken = testAuthToken,
      headerData = Map(),
      searchKeys = Map("ern" -> "ERN123")
    )
    val nrsPayLoad    = NrsPayload("encodepayload", nrsMetadata)

    "must return a Future.successful" - {
      "when the call to NRS succeeds" in {
        val connector = app.injector.instanceOf[NrsConnector]
        wireMockServer.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(ACCEPTED)
            )
        )

        val result = connector.sendToNrs(nrsPayLoad)(hc).futureValue

        result mustBe Done
      }
    }
    "must return a failed future" - {
      "when the call to NRS fails with a 4xx error" - {
        "with circuit breaker present" in {
          val connector = app.injector.instanceOf[NrsConnector]
          wireMockServer.stubFor(
            post(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withBody("body")
                  .withStatus(BAD_REQUEST)
              )
          )

          val exception = connector.sendToNrs(nrsPayLoad)(hc).failed.futureValue

          exception mustBe UnexpectedResponseException(BAD_REQUEST, "body")
        }
        "and NOT trip the circuit breaker" in {
          val connector      = app.injector.instanceOf[NrsConnector]
          val circuitBreaker = app.injector.instanceOf[NrsCircuitBreaker].breaker

          circuitBreaker.resetTimeout mustEqual 1.second

          wireMockServer.stubFor(
            post(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withBody("body")
                  .withStatus(BAD_REQUEST)
              )
          )

          circuitBreaker.isOpen mustBe false
          connector.sendToNrs(nrsPayLoad)(hc).failed.futureValue
          circuitBreaker.isOpen mustBe false
          connector.sendToNrs(nrsPayLoad)(hc).failed.futureValue

          wireMockServer.verify(2, postRequestedFor(urlMatching(url)))
        }
      }
      "when the call to NRS fails with a 5xx error" - {
        "with circuit breaker present" in {
          val connector      = app.injector.instanceOf[NrsConnector]
          val circuitBreaker = app.injector.instanceOf[NrsCircuitBreaker].breaker

          circuitBreaker.resetTimeout mustEqual 1.second

          wireMockServer.stubFor(
            post(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withBody("body")
                  .withStatus(INTERNAL_SERVER_ERROR)
              )
          )

          val exception = connector.sendToNrs(nrsPayLoad)(hc).failed.futureValue

          exception mustBe UnexpectedResponseException(INTERNAL_SERVER_ERROR, "body")
        }
        "and trip the circuit breaker to fail fast" in {
          val connector      = app.injector.instanceOf[NrsConnector]
          val circuitBreaker = app.injector.instanceOf[NrsCircuitBreaker].breaker

          circuitBreaker.resetTimeout mustEqual 1.second

          wireMockServer.stubFor(
            post(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withBody("body")
                  .withStatus(INTERNAL_SERVER_ERROR)
              )
          )

          val onOpen = Promise[Unit]()
          circuitBreaker.onOpen(onOpen.success(()))

          circuitBreaker.isOpen mustBe false
          connector.sendToNrs(nrsPayLoad)(hc).failed.futureValue
          onOpen.future.futureValue
          circuitBreaker.isOpen mustBe true
          connector.sendToNrs(nrsPayLoad)(hc).failed.futureValue

          wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
        }
      }
    }
    "must forward the correlation id if it exists" in {
      val connector = app.injector.instanceOf[NrsConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
      )

      //If this succeeds the wiremock has acted as a matcher
      val result = connector.sendToNrs(nrsPayLoad)(hc).futureValue
    }
    "must forward a new correlation id if not exists" in {
      val connector = app.injector.instanceOf[NrsConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
      )

      //This should fail as the headers don't match
      val result = connector.sendToNrs(nrsPayLoad)(HeaderCarrier()).failed.futureValue

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
      )

      //This should succeed as the headers now don't match correlationId
      val result2 = connector.sendToNrs(nrsPayLoad)(HeaderCarrier()).futureValue

    }
  }
}
