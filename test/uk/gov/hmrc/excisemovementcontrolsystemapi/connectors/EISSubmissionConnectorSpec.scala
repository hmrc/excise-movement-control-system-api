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
import org.mockito.captor.ArgCaptor
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable, UnprocessableEntity}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.EISHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.StringSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.Headers._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionRequest, EISSubmissionResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class EISSubmissionConnectorSpec
  extends PlaySpec
    with StringSupport
    with BeforeAndAfterEach
    with EitherValues {

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
  protected implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockHttpClient = mock[HttpClient]
  private val emcsUtils = mock[EmcsUtils]
  private val appConfig = mock[AppConfig]
  private val dateTimeService = mock[DateTimeService]

  private val metrics = mock[MetricRegistry](RETURNS_DEEP_STUBS)

  private val connector = new EISSubmissionConnector(mockHttpClient, emcsUtils, appConfig, metrics, dateTimeService)
  private val emcsCorrelationId = "1234566"
  private val xml = <IE815></IE815>
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
  private val timerContext = mock[Timer.Context]
  private val ie815Message = mock[IE815Message]
  private val submissionBearerToken = "submissionBearerToken"

  private val ern = "123"
  private val timestamp = Instant.parse("2023-09-17T09:32:50.345Z")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient, appConfig, metrics, timerContext, emcsUtils)

    when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "Success", emcsCorrelationId))))
    when(dateTimeService.timestampToMilliseconds()).thenReturn(timestamp)
    when(appConfig.emcsReceiverMessageUrl).thenReturn("/eis/path")
    when(appConfig.submissionBearerToken).thenReturn(submissionBearerToken)
    when(metrics.timer(any).time()) thenReturn timerContext
    when(ie815Message.messageType).thenReturn("IE815")
    when(ie815Message.consignorId).thenReturn(ern)
    when(emcsUtils.encode(any)).thenReturn("encode-message")
    when(ie815Message.messageIdentifier).thenReturn("DummyIdentifier")

  }

  "post" should {
    "return successful EISResponse" in {
      val result: Either[Result, EISSubmissionResponse] = await(submitExciseMovementForIE815)

      result mustBe Right(EISSubmissionResponse("ok", "Success", emcsCorrelationId))
    }

    "get URL from appConfig" in {
      submitExciseMovementWithParams(xml, ie815Message, ern)

      verify(appConfig).emcsReceiverMessageUrl
    }

    "send a request with the right parameters" in {
      val expectedRequest = EISSubmissionRequest(ern, "IE815", "encode-message")

      submitExciseMovementWithParams(xml, ie815Message, ern)

      verify(mockHttpClient).POST(
        eqTo("/eis/path"),
        eqTo(expectedRequest),
        eqTo(expectedHeader)
      )(any, any, any, any)
    }

    "wrap the xml in the control document" in {
      submitExciseMovementWithParams(xml, ie815Message, ern)

      val captor = ArgCaptor[String]
      verify(emcsUtils).encode(captor.capture)

      clean(captor.value) mustBe clean(controlWrappedXml.toString)
    }

    "use the right request parameters in http client" in {
      submitExciseMovementWithParams(xml, ie815Message, ern)

      val eisHttpReader: EISHttpReader = verifyHttpHeader

      eisHttpReader.isInstanceOf[EISHttpReader] mustBe true
      eisHttpReader.ern mustBe ern
    }

    "return Bad request error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(BadRequest("any error"))))

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe BadRequest("any error")
    }

    "return 500 if post request fail" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))
      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe InternalServerError(
        Json.toJson(EISErrorResponse(timestamp, "INTERNAL_SERVER_ERROR",
          "Exception", "error", emcsCorrelationId)))
    }

    "return Not found error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(NotFound("error"))))

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe NotFound("error")
    }

    "return service unavailable error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(ServiceUnavailable("any error"))))

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe ServiceUnavailable("any error")
    }

    "return Internal service error error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(InternalServerError("any error"))))

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe InternalServerError("any error")
    }

    "return unprocessable entity error" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(UnprocessableEntity("any error"))))

      val result = await(submitExciseMovementForIE815)

      result.left.value mustBe UnprocessableEntity("any error")
    }

    "start and stop metrics" in {
      when(mockHttpClient.POST[Any, Any](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(BadRequest("any error"))))

      await(submitExciseMovementForIE815)

      verify(metrics).timer(eqTo("emcs.submission.connector.timer"))
      verify(metrics.timer(eqTo("emcs.submission.connector.timer"))).time()
      verify(timerContext).stop()
    }
  }

  private def verifyHttpHeader: EISHttpReader = {
    val captor = ArgCaptor[EISHttpReader]
    verify(mockHttpClient).POST(any, any, any)(any, captor.capture, any, any)

    val eisHttpReader = captor.value
    eisHttpReader.isInstanceOf[EISHttpReader] mustBe true
    eisHttpReader
  }

  private def submitExciseMovementForIE815: Future[Either[Result, EISSubmissionResponse]] = {
    submitExciseMovementWithParams(xml, ie815Message, ern)
  }

  private def submitExciseMovementWithParams(
                                              xml: NodeSeq,
                                              message: IEMessage,
                                              authErn: String
                                            ): Future[Either[Result, EISSubmissionResponse]] = {

    connector.submitMessage(message, xml.toString(), authErn, emcsCorrelationId)
  }

  private def expectedHeader =
    Seq(HeaderNames.ACCEPT -> ContentTypes.JSON,
      HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
      DateTimeName -> timestamp.toString,
      XCorrelationIdName -> emcsCorrelationId,
      XForwardedHostName -> MDTPHost,
      SourceName -> APIPSource,
      Authorization -> authorizationValue(submissionBearerToken)
    )
}
