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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{EISHeaderTestSupport, StringSupport}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EISErrorResponseDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class EISSubmissionConnectorSpec
    extends PlaySpec
    with StringSupport
    with EISHeaderTestSupport
    with BeforeAndAfterEach
    with EitherValues {

  protected implicit val hc: HeaderCarrier    = HeaderCarrier()
  protected implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockHttpClient     = mock[HttpClientV2]
  private val emcsUtils          = mock[EmcsUtils]
  private val appConfig          = mock[AppConfig]
  private val dateTimeService    = mock[DateTimeService]
  private val mockRequestBuilder = mock[RequestBuilder]
  private val mockResponse       = mock[HttpResponse]

  private val metrics = mock[MetricRegistry](RETURNS_DEEP_STUBS)

  private val connector               = new EISSubmissionConnector(mockHttpClient, emcsUtils, appConfig, metrics, dateTimeService)
  private val emcsCorrelationId       = "1234566"
  private val xml                     = <IE815></IE815>
  private val controlWrappedXml: Elem =
    <con:Control xmlns:con="http://www.govtalk.gov.uk/taxation/InternationalTrade/Common/ControlDocument">
      <con:MetaData>
        <con:MessageId>DummyIdentifier</con:MessageId>
        <con:Source>APIP</con:Source>
      </con:MetaData>
      <con:OperationRequest>
        <con:Parameters>
          <con:Parameter Name="ExciseRegistrationNumber">123</con:Parameter>
          <con:Parameter Name="message">
            <![CDATA[<IE815></IE815>]]>
          </con:Parameter>
        </con:Parameters>
        <con:ReturnData>
          <con:Data Name="schema"/>
        </con:ReturnData>
      </con:OperationRequest>
    </con:Control>
  private val timerContext            = mock[Timer.Context]
  private val ie815Message            = mock[IE815Message]
  private val submissionBearerToken   = "submissionBearerToken"

  private val ern       = "123"
  private val timestamp = Instant.parse("2023-09-17T09:32:50.345456Z")

  def createTestError(status: Int) = EISErrorResponseDetails(status, timestamp, "", "", "", None)

  private val happyResponse = EISSubmissionResponse("ok", "Success", emcsCorrelationId)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient, appConfig, metrics, timerContext, emcsUtils, mockResponse)

    when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse](any(), any()))
      .thenReturn(Future.successful(mockResponse))
    when(mockResponse.json)
      .thenReturn(Json.toJson(happyResponse))
    when(mockResponse.status)
      .thenReturn(OK)

    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(appConfig.emcsReceiverMessageUrl).thenReturn("http://localhost:8080/eis/path")
    when(appConfig.submissionBearerToken).thenReturn(submissionBearerToken)
    when(metrics.timer(any).time()) thenReturn timerContext
    when(ie815Message.messageType).thenReturn("IE815")
    when(ie815Message.consignorId).thenReturn(ern)
    when(emcsUtils.encode(any)).thenReturn("encode-message")
    when(ie815Message.messageIdentifier).thenReturn("DummyIdentifier")

  }

  "post" should {
    "return successful EISResponse" in {
      val result: Either[EISErrorResponseDetails, EISSubmissionResponse] = await(submitExciseMovementForIE815)

      result mustBe Right(happyResponse)
    }

    "get URL from appConfig" in {
      submitExciseMovementWithParams(xml, ie815Message, ern)

      verify(appConfig).emcsReceiverMessageUrl
    }

    "wrap the xml in the control document" in {
      submitExciseMovementWithParams(xml, ie815Message, ern)

      val captor = ArgCaptor[String]
      verify(emcsUtils).encode(captor.capture)

      clean(captor.value) mustBe clean(controlWrappedXml.toString)
    }

    "return error when a bad request response returned" in {

      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockResponse.json).thenReturn(Json.toJson(EISErrorResponse(timestamp, "400", "", "", "")))
      when(mockResponse.status).thenReturn(BAD_REQUEST)

      val result = await(submitExciseMovementForIE815)
      result.left.value mustBe EISErrorResponseDetails(BAD_REQUEST, timestamp, "", "", "", None)
    }

    //Should this instead be a Future.failed?
    "return EISErrorResponseDetails outlining a 500 if post request fail" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe EISErrorResponseDetails(
        INTERNAL_SERVER_ERROR,
        timestamp,
        "Internal server error",
        "Unexpected error occurred while processing Submission request",
        emcsCorrelationId
      )
    }

    //Can probably refactor a number of these to a single iterative test
    "return Service Unavailable error" in {

      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockResponse.json).thenReturn(Json.toJson(EISErrorResponse(timestamp, "503", "service error", "", "")))
      when(mockResponse.status).thenReturn(SERVICE_UNAVAILABLE)

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe EISErrorResponseDetails(SERVICE_UNAVAILABLE, timestamp, "service error", "", "", None)
    }

    "return Internal service error" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockResponse.json).thenReturn(Json.toJson(EISErrorResponse(timestamp, "internal server error", "", "", "")))
      when(mockResponse.status).thenReturn(INTERNAL_SERVER_ERROR)

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe EISErrorResponseDetails(
        INTERNAL_SERVER_ERROR,
        timestamp,
        "",
        "",
        "",
        None
      )
    }

    "return unprocessable entity error" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockResponse.json).thenReturn(Json.toJson(EISErrorResponse(timestamp, "unprocessable", "", "", "")))
      when(mockResponse.status).thenReturn(UNPROCESSABLE_ENTITY)

      val result = await(submitExciseMovementForIE815)
      result.left.value mustBe EISErrorResponseDetails(UNPROCESSABLE_ENTITY, timestamp, "", "", "", None)
    }

    "start and stop metrics" in {
      when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)

      await(submitExciseMovementForIE815)

      verify(metrics).timer(eqTo("emcs.submission.connector.timer"))
      verify(metrics.timer(eqTo("emcs.submission.connector.timer"))).time()
      verify(timerContext).stop()
    }
  }

  private def submitExciseMovementForIE815: Future[Either[EISErrorResponseDetails, EISSubmissionResponse]] =
    submitExciseMovementWithParams(xml, ie815Message, ern)

  private def submitExciseMovementWithParams(
    xml: NodeSeq,
    message: IEMessage,
    authErn: String
  ): Future[Either[EISErrorResponseDetails, EISSubmissionResponse]] =
    connector.submitMessage(message, xml.toString(), authErn, emcsCorrelationId)
}
