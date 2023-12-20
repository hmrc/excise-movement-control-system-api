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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connector


import com.codahale.metrics.Timer
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PreValidateTraderConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.PreValidateTraderHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.Headers._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.PreValidateTraderResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderErrorResponse, getPreValidateTraderRequest, getPreValidateTraderSuccessResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class PreValidateTraderConnectorSpec extends PlaySpec with BeforeAndAfterEach with EitherValues {

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
  protected implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockHttpClient = mock[HttpClient]
  private val emcsUtils = mock[EmcsUtils]
  private val appConfig = mock[AppConfig]

  private val metrics = mock[Metrics](RETURNS_DEEP_STUBS)

  private val connector = new PreValidateTraderConnector(mockHttpClient, emcsUtils, appConfig, metrics)
  private val emcsCorrelationId = "1234566"
  private val timerContext = mock[Timer.Context]
  private val preValidateTraderBearerToken = "preValidateTraderBearerToken"

  private val validRequest = getPreValidateTraderRequest
  private val validResponse = getPreValidateTraderSuccessResponse
  private val businessError = getPreValidateTraderErrorResponse


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient, appConfig, metrics, timerContext)

    when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
      .thenReturn(Future.successful(Right(Right(validResponse))))

    when(emcsUtils.getCurrentDateTimeString).thenReturn("2023-09-17T09:32:50.345")
    when(emcsUtils.generateCorrelationId).thenReturn(emcsCorrelationId)
    when(appConfig.preValidateTraderUrl).thenReturn("/eis/path")
    when(appConfig.preValidateTraderBearerToken).thenReturn(preValidateTraderBearerToken)
    when(metrics.defaultRegistry.timer(any).time()) thenReturn timerContext
  }

  "post" should {
    "return successful response" in {
      val result = await(submitPreValidateTrader())

      result mustBe Right(Right(validResponse))
    }

    "return business error response" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Right(Left(businessError))))

      val result = await(submitPreValidateTrader())

      result mustBe Right(Left(businessError))
    }


    "get URL from appConfig" in {
      submitPreValidateTrader()

      verify(appConfig).preValidateTraderUrl
    }

    "send a request with the right parameters" in {
      submitPreValidateTrader()

      verify(mockHttpClient).POST(
        eqTo("/eis/path"),
        eqTo(validRequest),
        eqTo(expectedHeader)
      )(any, any, any, any)
    }

    "use the right request parameters in http client" in {
      submitPreValidateTrader()

      val preValidateTraderHttpReader = verifyHttpHeader

      preValidateTraderHttpReader.ern mustBe "ern123"
    }

    "return Bad request error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(BadRequest("any error"))))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe BadRequest("any error")
    }

    "return 500 if post request fail" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))
      val result = await(submitPreValidateTrader())

      result.left.value mustBe InternalServerError(
        Json.toJson(EISErrorResponse(LocalDateTime.parse("2023-09-17T09:32:50.345"),
          "Exception", "error", emcsCorrelationId)))
    }

    "return Not found error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(NotFound("error"))))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe NotFound("error")
    }

    "return service unavailable error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(ServiceUnavailable("any error"))))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe ServiceUnavailable("any error")
    }

    "return Internal service error error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(InternalServerError("any error"))))

      val result = await(submitPreValidateTrader())

      result.left.value mustBe InternalServerError("any error")
    }

    "start and stop metrics" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(BadRequest("any error"))))

      await(submitPreValidateTrader())

      verify(metrics.defaultRegistry).timer(eqTo("emcs.prevalidatetrader.connector.timer"))
      verify(metrics.defaultRegistry.timer(eqTo("emcs.prevalidatetrader.connector.timer"))).time()
      verify(timerContext).stop()
    }
  }

  private def verifyHttpHeader: PreValidateTraderHttpReader = {
    val captor = ArgCaptor[PreValidateTraderHttpReader]
    verify(mockHttpClient).POST(any, any, any)(any, captor.capture, any, any)

    val preValidateTraderHttpReader = captor.value
    preValidateTraderHttpReader.isInstanceOf[PreValidateTraderHttpReader] mustBe true
    preValidateTraderHttpReader
  }

  private def submitPreValidateTrader(): Future[Either[Result, PreValidateTraderResponse]] = {
    connector.submitMessage(validRequest, "ern123")
  }

  private def expectedHeader =
    Seq(HeaderNames.ACCEPT -> ContentTypes.JSON,
      HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
      DateTimeName -> "2023-09-17T09:32:50.345",
      XCorrelationIdName -> "1234566",
      XForwardedHostName -> MDTPHost,
      SourceName -> APIPSource,
      Authorization -> authorizationValue(preValidateTraderBearerToken)
    )
}
