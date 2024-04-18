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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.EISHeaderTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.typeOf

class GetMovementConnectorSpec extends PlaySpec
  with BeforeAndAfterEach
  with EitherValues
  with EISHeaderTestSupport {

  protected implicit val ec: ExecutionContext = ExecutionContext.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpClient = mock[HttpClient]
  private val appConfig = mock[AppConfig]
  private val metrics = mock[MetricRegistry](RETURNS_DEEP_STUBS)
  private val emcsUtil = mock[EmcsUtils]
  private val dateTimeService = mock[DateTimeService]
  private val sut = new GetMovementConnector(httpClient, appConfig, emcsUtil, metrics, dateTimeService)
  private val timestamp = Instant.parse("2023-02-03T05:06:07.312456Z")
  private val response = EISConsumptionResponse(
    timestamp,
    "ern",
    "message"
  )

  private val timerContext = mock[Timer.Context]

  private val movementBearerToken = "movementBearerToken"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, emcsUtil, metrics, timerContext)

    when(httpClient.GET[Any](any, any, any)(any, any, any))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(response).toString())))
    when(appConfig.traderMovementUrl).thenReturn("/trader-movement-url")
    when(appConfig.movementBearerToken).thenReturn(movementBearerToken)
    when(emcsUtil.generateCorrelationId).thenReturn("1234")
    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(metrics.timer(any).time()) thenReturn timerContext
  }

  "get" should {
    "return a response" in {
      val result = await(sut.get("ern", "arc"))

      result mustBe Right(response)
    }

    "pass the right parameters to the HttpClient" in {
      await(sut.get("ern", "arc"))

      verify(httpClient).GET(
        eqTo("/trader-movement-url"),
        eqTo(Seq("exciseregistrationnumber" -> "ern", "arc" -> "arc")),
        eqTo(expectedConsumptionHeaders("2023-02-03T05:06:07.312Z", "1234", movementBearerToken))
      )(any, any, any)
    }

    "return an error " when {
      "cannot parse Json" in {
        when(httpClient.GET[Any](any, any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(200, "error message")))

        val result = await(sut.get("ern", "arc"))

        result.left.value mustBe InternalServerError(s"Response body could not be read as type ${typeOf[EISConsumptionResponse]}")
      }

      "EIS return an error" in {
        when(httpClient.GET[Any](any, any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(404, "error message")))

        val result = await(sut.get("ern", "arc"))

        result.left.value mustBe InternalServerError("error message")
      }
    }

    "should start a timer" in {
      await(sut.get("123", "arc"))

      verify(metrics).timer(eqTo("emcs.getmovements.timer"))
      verify(metrics.timer(eqTo("emcs.getmovements.timer"))).time()
      verify(timerContext).stop()
    }
  }
}
