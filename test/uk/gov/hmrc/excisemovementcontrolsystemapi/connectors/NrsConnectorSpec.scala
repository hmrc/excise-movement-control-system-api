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
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.BAD_REQUEST
import play.api.libs.concurrent.Futures
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.ZonedDateTime
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class NrsConnectorSpec extends PlaySpec with NrsTestData with EitherValues with BeforeAndAfterEach {

  protected implicit val hc: HeaderCarrier    = HeaderCarrier()
  protected implicit val ec: ExecutionContext = ExecutionContext.global
  protected implicit val futures: Futures     = mock[Futures]

  private val httpClient                 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  private val appConfig                  = mock[AppConfig]
  private val emcsUtils                  = mock[EmcsUtils]
  private val metrics                    = mock[MetricRegistry](RETURNS_DEEP_STUBS)
  private val connector                  = new NrsConnector(httpClient, appConfig, metrics)
  private val timerContext               = mock[Timer.Context]
  private val timeStamp                  = ZonedDateTime.now()
  private val nrsUrl                     = "http://localhost:8080/nrs-url"
  private val nrsMetadata                = NrsMetadata(
    businessId = "emcs",
    notableEvent = "excise-movement-control-system",
    payloadContentType = "application/json",
    payloadSha256Checksum = sha256Hash("payload for NRS"),
    userSubmissionTimestamp = timeStamp.toString,
    identityData = testNrsIdentityData,
    userAuthToken = testAuthToken,
    headerData = Map(),
    searchKeys = Map("ern" -> "123")
  )
  private val nrsPayLoad                 = NrsPayload("encodepayload", nrsMetadata)
  private val successFulNrsResponse      = HttpResponse(
    202,
    Json.obj("nrSubmissionId" -> "testNesSubmissionId").toString()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig, emcsUtils, metrics, timerContext, mockRequestBuilder)

    when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse](any(), any()))
      .thenReturn(Future.successful(successFulNrsResponse))

    when(appConfig.getNrsSubmissionUrl).thenReturn(nrsUrl)
    when(appConfig.nrsApiKey).thenReturn("authToken")
    when(appConfig.nrsRetryDelays).thenReturn(
      Seq(
        Duration.create(1L, "seconds"),
        Duration.create(1L, "seconds"),
        Duration.create(1L, "seconds")
      )
    )
    when(futures.delay(any)).thenReturn(Future.successful(Done))
    when(metrics.timer(any).time()) thenReturn timerContext
  }

  "submit" should {
    "return success" in {
      val result = await(connector.sendToNrs(nrsPayLoad, "correlationId"))

      result mustBe NonRepudiationSubmissionAccepted("testNesSubmissionId")
    }

    "return an error" in {

      when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(400, "bad request")))

      val result = await(connector.sendToNrs(nrsPayLoad, "correlationId"))

      result mustBe NonRepudiationSubmissionFailed(400, "bad request")
    }

    "retry 3 time" when {
      "nrs return a non 2xx status" in {
        when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(400, "bad request")))

        val result = await(connector.sendToNrs(nrsPayLoad, "correlationId"))

        result mustBe NonRepudiationSubmissionFailed(BAD_REQUEST, "bad request")
        verifyHttpPostCAll(3)
      }

      "nrs throws" in {

        when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        intercept[RuntimeException] {
          await(connector.sendToNrs(nrsPayLoad, "correlationId"))
          verifyHttpPostCAll(3)
        }
      }
    }

    "return straight after an exception" in {

      when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("error")), Future.successful(successFulNrsResponse))

      val result = await(connector.sendToNrs(nrsPayLoad, "correlationId"))

      result mustBe NonRepudiationSubmissionAccepted("testNesSubmissionId")
      verifyHttpPostCAll(2)
    }

    "start and stop a timer" in {
      await(connector.sendToNrs(NrsPayload("encodepayload", nrsMetadata), "correlationId"))

      verify(metrics).timer(eqTo("emcs.nrs.submission.timer"))
      verify(metrics.timer(eqTo("emcs.nrs.submission.timer"))).time()
      verify(timerContext).stop()
    }
  }

  private def verifyHttpPostCAll(retriedAttempt: Int) =
    verify(mockRequestBuilder, times(retriedAttempt)).execute(any(), any())
}
