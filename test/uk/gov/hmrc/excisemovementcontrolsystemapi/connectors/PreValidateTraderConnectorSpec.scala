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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.codahale.metrics.{MetricRegistry, Timer}
import com.fasterxml.jackson.core.JsonParseException
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.EISHeaderTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisErrorResponsePresentation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{PreValidateTraderEISResponse, PreValidateTraderMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PreValidateTraderConnectorSpec
    extends PlaySpec
    with EISHeaderTestSupport
    with BeforeAndAfterEach
    with EitherValues {

  private val emcsCorrelationId = "1234566"

  private val request = FakeRequest().withHeaders(HttpHeader.xCorrelationId -> emcsCorrelationId)

  implicit val hc: HeaderCarrier              = HeaderCarrierConverter.fromRequest(request)
  protected implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockHttpClient  = mock[HttpClientV2]
  private val dateTimeService = mock[DateTimeService]
  private val appConfig       = mock[AppConfig]

  private val metrics = mock[MetricRegistry](RETURNS_DEEP_STUBS)

  private val connector                    =
    new PreValidateTraderConnector(mockHttpClient, appConfig, metrics, dateTimeService)
  private val timerContext                 = mock[Timer.Context]
  private val preValidateTraderBearerToken = "preValidateTraderBearerToken"
  private val mockRequestBuilder           = mock[RequestBuilder]

  private val validRequest  = getPreValidateTraderRequest
  private val validResponse = getPreValidateTraderSuccessEISResponse
  private val businessError = getPreValidateTraderErrorEISResponse

  private val timestamp = Instant.parse("2023-09-17T09:32:50Z")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient, appConfig, metrics, timerContext)

    when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[PreValidateTraderEISResponse](any(), any()))
      .thenReturn(Future.successful(validResponse))

    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(appConfig.preValidateTraderUrl).thenReturn("http://localhost:8080/eis/path")
    when(appConfig.preValidateTraderBearerToken).thenReturn(preValidateTraderBearerToken)
    when(metrics.timer(any).time()) thenReturn timerContext
  }

  "post" should {
    "return successful response" in {
      val result = await(submitPreValidateTrader())

      result mustBe Right(validResponse)
    }

    "return business error response" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[PreValidateTraderEISResponse](any(), any()))
        .thenReturn(Future.successful(businessError))

      val result = await(submitPreValidateTrader())

      result mustBe Right(businessError)
    }

    "get URL from appConfig" in {
      submitPreValidateTrader()

      verify(appConfig).preValidateTraderUrl
    }

    "return Bad request error" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[Result, PreValidateTraderMessageResponse]](any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("any error", BAD_REQUEST)))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe BadRequest(
        Json.toJson(
          EisErrorResponsePresentation(
            timestamp,
            "PreValidateTrader error",
            "Error occurred during PreValidateTrader request",
            emcsCorrelationId
          )
        )
      )
    }

    "return 500 if post request fail" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[Result, PreValidateTraderMessageResponse]](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe InternalServerError(
        Json.toJson(
          EisErrorResponsePresentation(
            timestamp,
            "Internal Server Error",
            "Unexpected error occurred while processing PreValidateTrader request",
            emcsCorrelationId
          )
        )
      )
    }

    "return 500 if the response JSON cannot be parsed" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[Result, PreValidateTraderMessageResponse]](any(), any()))
        .thenReturn(Future.failed(new JsonParseException("Invalid JSON")))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe InternalServerError(
        Json.toJson(
          EisErrorResponsePresentation(
            timestamp,
            "Internal Server Error",
            "Unexpected error occurred while processing PreValidateTrader request",
            emcsCorrelationId
          )
        )
      )
    }

    "return 500 if the response JSON cannot be deserialized" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[Result, PreValidateTraderMessageResponse]](any(), any()))
        .thenReturn(Future.failed(JsResultException(Seq.empty)))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe InternalServerError(
        Json.toJson(
          EisErrorResponsePresentation(
            timestamp,
            "Internal Server Error",
            "Unexpected error occurred while processing PreValidateTrader request",
            emcsCorrelationId
          )
        )
      )
    }

    "return Not found error" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[Result, PreValidateTraderMessageResponse]](any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("error", NOT_FOUND)))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe NotFound(
        Json.toJson(
          EisErrorResponsePresentation(
            timestamp,
            "PreValidateTrader error",
            "Error occurred during PreValidateTrader request",
            emcsCorrelationId
          )
        )
      )
    }

    "return service unavailable error" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[Result, PreValidateTraderMessageResponse]](any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("any error", SERVICE_UNAVAILABLE)))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe ServiceUnavailable(
        Json.toJson(
          EisErrorResponsePresentation(
            timestamp,
            "PreValidateTrader error",
            "Error occurred during PreValidateTrader request",
            emcsCorrelationId
          )
        )
      )
    }

    "return Internal service error error" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[PreValidateTraderMessageResponse](any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("any error", INTERNAL_SERVER_ERROR)))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe InternalServerError(
        Json.toJson(
          EisErrorResponsePresentation(
            timestamp,
            "PreValidateTrader error",
            "Error occurred during PreValidateTrader request",
            emcsCorrelationId
          )
        )
      )
    }

    "start and stop metrics" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "any error")))

      await(submitPreValidateTrader())

      verify(metrics).timer(eqTo("emcs.prevalidatetrader.connector.timer"))
      verify(metrics.timer(eqTo("emcs.prevalidatetrader.connector.timer"))).time()
      verify(timerContext).stop()
    }
  }

  private def submitPreValidateTrader(): Future[Either[Result, PreValidateTraderEISResponse]] =
    connector.submitMessage(validRequest, "ern123")
}
