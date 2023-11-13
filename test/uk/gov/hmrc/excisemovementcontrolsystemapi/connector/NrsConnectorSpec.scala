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

import akka.Done
import com.codahale.metrics.Timer
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.BAD_REQUEST
import play.api.libs.concurrent.Futures
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NonRepudiationSubmissionAccepted, NrsMetadata, NrsPayload}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class NrsConnectorSpec
  extends PlaySpec
    with ScalaFutures
    with NrsTestData
    with EitherValues
    with BeforeAndAfterEach {

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
  protected implicit val ec: ExecutionContext = ExecutionContext.global
  protected implicit val futures: Futures = mock[Futures]

  private val httpClient = mock[HttpClient]
  private val appConfig = mock[AppConfig]
  private val emcsUtils = mock[EmcsUtils]
  private val metrics = mock[Metrics](RETURNS_DEEP_STUBS)
  private val connector = new NrsConnector(httpClient, appConfig, emcsUtils, metrics)
  private val timerContext = mock[Timer.Context]
  private val timeStamp = ZonedDateTime.now()

  private val nrsMetadata  = NrsMetadata(
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
  private val nrsPayLoad = NrsPayload("encodepayload", nrsMetadata)
  private val successFulNrsResponse = HttpResponse(
    202,
    Json.obj("nrSubmissionId" -> "testNesSubmissionId").toString()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig, emcsUtils, metrics, timerContext)

    when(httpClient.POST[Any,Any](any,any,any)(any,any,any,any))
      .thenReturn(Future.successful(successFulNrsResponse))
    when(appConfig.getNrsSubmissionUrl).thenReturn("/nrs-url")
    when(appConfig.nrsAuthorisationToken).thenReturn("authToken")
    when(appConfig.nrsRetries).thenReturn(Seq(
      Duration.create(1L, "seconds"),
      Duration.create(1L, "seconds"),
      Duration.create(1L, "seconds")
    ))
    when(futures.delay(any)).thenReturn(Future.successful(Done))
    when(metrics.defaultRegistry.timer(any).time()) thenReturn timerContext
    when(emcsUtils.generateCorrelationId).thenReturn(
      UUID.fromString("00000000-0000-0001-0000-000000000001").toString)
  }

  "submit" should {
    "return success" in {
      val result = connector.sendToNrs(nrsPayLoad).futureValue

      result mustBe Right(NonRepudiationSubmissionAccepted("testNesSubmissionId"))
    }


    "return an error" in {
      when(httpClient.POST[Any,Any](any,any,any)(any,any,any,any))
        .thenReturn(Future.successful(HttpResponse(400, JsObject.empty.toString())))

      val result = connector.sendToNrs(nrsPayLoad).futureValue

      result.left.value mustBe 400
    }

    "retry 3 time" when {
      "nrs return a non 2xx status" in {
        when(httpClient.POST[Any, Any](any, any, any)(any, any, any, any))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, JsObject.empty.toString())))

        val result = connector.sendToNrs(nrsPayLoad).futureValue

        result.left.value mustBe BAD_REQUEST
        verifyHttpPostCAll(3)
      }

      "nrs throws" in {
        when(httpClient.POST[Any, Any](any, any, any)(any, any, any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        intercept[RuntimeException] {
          connector.sendToNrs(nrsPayLoad).futureValue
          verifyHttpPostCAll(3)
        }


      }
    }

    "return straight after an exception" in{
      when(httpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(
          Future.failed(new RuntimeException("error")),
          Future.successful(successFulNrsResponse)
        )

      val result = connector.sendToNrs(nrsPayLoad).futureValue

      result mustBe Right(NonRepudiationSubmissionAccepted("testNesSubmissionId"))
      verifyHttpPostCAll(2)
    }

    "start and stop a timer" in {
      connector.sendToNrs(NrsPayload("encodepayload", nrsMetadata)).futureValue

      verify(metrics.defaultRegistry).timer(eqTo("emcs.nrs.submission.timer"))
      verify(metrics.defaultRegistry.timer(eqTo("emcs.nrs.submission.timer"))).time()
      verify(timerContext).stop()
    }
  }

  private def verifyHttpPostCAll(retriedAttempt: Int) = {
    verify(httpClient, times(retriedAttempt)).POST[Any, Any](
      eqTo("/nrs-url"),
      eqTo(nrsPayLoad.toJsObject),
      eqTo(expectedHeader)
    )(any, any, any, any)
  }

  private def expectedHeader: Seq[(String, String)] = {
    Seq(
      "Content-Type" -> "application/json",
      ("X-API-Key", "authToken")
    )
  }
}
