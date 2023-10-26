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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageReceiptConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, MessageReceiptResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe.typeOf

class MessageReceiptConnectorSpec
  extends PlaySpec
    with BeforeAndAfterEach
    with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val timerContext = mock[Timer.Context]
  private val httpClient = mock[HttpClient]
  private val appConfig = mock[AppConfig]
  private val metrics = mock[Metrics](RETURNS_DEEP_STUBS)
  private val emcsUtil = mock[EmcsUtils]
  private val sut = new MessageReceiptConnector(httpClient, appConfig, emcsUtil, metrics)

  private val dateTime = LocalDateTime.of(2023, 1,2,3,4,5)
  private val response = MessageReceiptResponse(dateTime, "123", 10)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig, metrics, timerContext)

    when(httpClient.PUTString[Any](any, any, any)(any,any,any))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(response).toString())))
    when(emcsUtil.getCurrentDateTimeString).thenReturn(dateTime.toString)
    when(emcsUtil.generateCorrelationId).thenReturn("12345")
    when(appConfig.messageReceiptUrl(any)).thenReturn("/messageReceipt")
    when(appConfig.systemApplication).thenReturn("system.application")
    when(metrics.defaultRegistry.timer(any).time()) thenReturn timerContext
  }

  "put" should {
    "return a response" in {
      val result = await(sut.put("123"))

      result mustBe Right(response)
    }

    "should sent a request with the right parameters" in {
      await(sut.put("123"))

      val headers = Seq(
        "x-forwarded-host" -> "system.application",
        "x-correlation-id" -> "12345",
        "source" -> "APIP",
        "dateTime" -> dateTime.toString
      )
      verify(httpClient).PUTString[Any](
        eqTo("/messageReceipt"),
        eqTo(""),
        eqTo(headers)
      )(any,any,any)
    }

    "should start a timer" in {
      await(sut.put("123"))

      verify(metrics.defaultRegistry).timer(eqTo("emcs.messagereceipt.timer"))
      verify(metrics.defaultRegistry.timer(eqTo("emcs.messagereceipt.timer"))).time()
      verify(timerContext).stop()
    }

    "return an error" when {
      "eis api return an error" in {
        when(httpClient.PUTString[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(404, "error")))

        val result = await(sut.put("123"))

        result.left.value mustBe InternalServerError("error")
      }

      "can parse Json" in {
        when(httpClient.PUTString[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(200, "error")))

        val result = await(sut.put("123"))

        result.left.value mustBe InternalServerError(s"Response body could not be read as type ${typeOf[MessageReceiptResponse]}")

      }
    }
  }
}