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
import dispatch.Future
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.Headers._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe.typeOf

class ShowNewMessagesConnectorSpec
  extends PlaySpec
    with BeforeAndAfterEach
    with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpClient = mock[HttpClient]
  private val appConfig = mock[AppConfig]
  private val metrics = mock[MetricRegistry](RETURNS_DEEP_STUBS)
  private val eisUtil = mock[EmcsUtils]
  private val dateTimeService = mock[DateTimeService]
  private val sut = new ShowNewMessagesConnector(httpClient, appConfig, eisUtil, metrics, dateTimeService)

  private val timestamp = Instant.parse("2023-02-03T05:06:07Z")
  private val response = EISConsumptionResponse(
    timestamp,
    "123",
    "message"
  )
  private val timerContext = mock[Timer.Context]

  private val messagesBearerToken = "messagesBearerToken"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig, metrics, timerContext)

    when(httpClient.PUTString[Any](any, any, any)(any, any, any))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(response).toString())))
    when(eisUtil.generateCorrelationId).thenReturn("1234")
    when(dateTimeService.timestampToMilliseconds()).thenReturn(timestamp)
    when(appConfig.showNewMessageUrl(any)).thenReturn("/showNewMessage")
    when(appConfig.messagesBearerToken).thenReturn(messagesBearerToken)
    when(metrics.timer(any).time()) thenReturn timerContext
  }

  "get" should {
    "get the show new message" in {
      await(sut.get("123"))

      verify(httpClient).PUTString[Any](
        eqTo("/showNewMessage"),
        eqTo(""),
        eqTo(expectedHeader)
      )(any, any, any)
    }

    "return a successful response" in {
      val result = await(sut.get("123"))

      result mustBe Right(response)
    }

    "should start a timer" in {
      await(sut.get("123"))

      verify(metrics).timer(eqTo("emcs.shownewmessage.timer"))
      verify(metrics.timer(eqTo("emcs.shownewmessage.timer"))).time()
      verify(timerContext).stop()
    }

    "should return an error" when {
      "eis return an error" in {
        when(httpClient.PUTString[Any](any, any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(422, "error message")))

        val result = await(sut.get("123"))

        result.left.value mustBe InternalServerError("error message")
      }

      "cannot parse json" in {
        when(httpClient.PUTString[Any](any, any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(200, "error message")))

        val result = await(sut.get("123"))

        result.left.value mustBe InternalServerError(s"Response body could not be read as type ${typeOf[EISConsumptionResponse]}")
      }
    }
  }

  private def expectedHeader: Seq[(String, String)] = {
    Seq(
      XForwardedHostName -> MDTPHost,
      XCorrelationIdName -> "1234",
      SourceName -> APIPSource,
      DateTimeName -> timestamp.toString,
      Authorization -> authorizationValue(messagesBearerToken)
    )
  }
}

