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
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.EISHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequestCopy}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISRequest, EISSubmissionResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class MovementConnectorSpec extends PlaySpec with BeforeAndAfterEach with EitherValues {

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
  protected implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockHttpClient = mock[HttpClient]
  private val emcsUtils = mock[EmcsUtils]
  private val appConfig = mock[AppConfig]

  private val metrics = mock[Metrics](RETURNS_DEEP_STUBS)

  private val connector = new EISSubmissionConnector(mockHttpClient, emcsUtils, appConfig, metrics)
  private val emcsCorrelationId = "1234566"
  private val message = "<IE815></IE815>"
  private val messageType = "IE815"
  private val encoder = Base64.getEncoder
  private val timerContext = mock[Timer.Context]
  private val ieMessage = mock[IE815Message]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient, appConfig, metrics, timerContext)

    when(emcsUtils.getCurrentDateTimeString).thenReturn("2023-09-17T09:32:50.345")
    when(emcsUtils.generateCorrelationId).thenReturn(emcsCorrelationId)
    when(appConfig.emcsReceiverMessageUrl).thenReturn("/eis/path")
    when(metrics.defaultRegistry.timer(any).time()) thenReturn timerContext
    when(emcsUtils.createEncoder).thenReturn(encoder)

    when(ieMessage.consigneeId).thenReturn(None)
    when(ieMessage.consignorId).thenReturn(Some("123"))
    when(ieMessage.localReferenceNumber).thenReturn(Some("123"))
    when(ieMessage.messageType).thenReturn("IE815")
  }

  "post" should {
    "return successful EISResponse" in {

      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "Success", emcsCorrelationId))))

      val result: Either[Result, EISSubmissionResponse] = await(submitExciseMovement)

      result mustBe Right(EISSubmissionResponse("ok", "Success", emcsCorrelationId))
    }

    "use the right request parameters in http client" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "Success", emcsCorrelationId))))

      when(ieMessage.consignorId).thenReturn(Some("234"))

      val encodeMessage = encoder.encodeToString(message.getBytes(StandardCharsets.UTF_8))
      val eisRequest = EISRequest(emcsCorrelationId, "2023-09-17T09:32:50.345", messageType, "APIP", "user1", encodeMessage)

      await(connector.submitExciseMovement(
        ParsedXmlRequestCopy(
          EnrolmentRequest(FakeRequest().withBody(message), Set.empty, "124"),
          ieMessage,
          Set.empty,
          "124"
        ))
      )

      verify(appConfig).emcsReceiverMessageUrl

      val captor = ArgCaptor[EISHttpReader]
      verify(mockHttpClient).POST(
        eqTo("/eis/path"),
        eqTo(eisRequest),
        eqTo(expectedHeader)
      )(any, captor.capture, any, any)

      captor.value.isInstanceOf[EISHttpReader] mustBe true
    }

    "return Bad request error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(BadRequest("any error"))))

      val result = await(submitExciseMovement)

      result.left.value mustBe BadRequest("any error")
    }

    "return 500 if post request fail" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))
      val result = await(submitExciseMovement)

      result.left.value mustBe InternalServerError(
        Json.toJson(EISErrorResponse(LocalDateTime.parse("2023-09-17T09:32:50.345"),
          "Exception", "error", emcsCorrelationId)))
    }

    "return Not found error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(NotFound("error"))))

      val result = await(submitExciseMovement)

      result.left.value mustBe NotFound("error")
    }

    "return service unavailable error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(ServiceUnavailable("any error"))))

      val result = await(submitExciseMovement)

      result.left.value mustBe ServiceUnavailable("any error")
    }

    "return Internal service error error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(InternalServerError("any error"))))

      val result = await(submitExciseMovement)

      result.left.value mustBe InternalServerError("any error")
    }

    "start and stop metrics" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(BadRequest("any error"))))

      await(submitExciseMovement)

      verify(metrics.defaultRegistry).timer(eqTo("emcs.eiscontroller.timer"))
      verify(metrics.defaultRegistry.timer(eqTo("emcs.eiscontroller.timer"))).time()
      verify(timerContext).stop()
    }

  }

  private def submitExciseMovement: Future[Either[Result, EISSubmissionResponse]] = {
    connector.submitExciseMovement(ParsedXmlRequestCopy(
      EnrolmentRequest(FakeRequest(), Set.empty, "124"),
      ieMessage,
      Set.empty,
      "124"
    )
    )
  }

  private def expectedHeader =
    Seq(HeaderNames.ACCEPT -> ContentTypes.JSON,
      HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
      "dateTime" -> "2023-09-17T09:32:50.345",
      "x-correlation-id" -> "1234566",
      "x-forwarded-host" -> "",
      "source" -> "APIP"
    )
}
