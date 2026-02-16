/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import generated.NewMessagesDataResponse
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.{Mockito, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector.GetMessagesException
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageReceiptSuccessResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.GetMessagesResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, HttpHeader}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Base64, UUID}

class MessageConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerSuite
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach
    with NewMessagesXml {

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(auditService)
  }

  private val mockDateTimeService = mock[DateTimeService]
  private val auditService        = mock[AuditService]
  private val formatter           = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

  override lazy val app: Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.eis.port"                  -> wireMockPort,
        "microservice.services.eis.messages-bearer-token" -> "some-bearer",
        "featureFlags.oldAuditingEnabled"                 -> "true"
      )
      .overrides(
        bind[DateTimeService].toInstance(mockDateTimeService),
        bind[AuditService].toInstance(auditService)
      )
      .build()

  private lazy val messageFactory = app.injector.instanceOf[IEMessageFactory]
  private lazy val connector      = app.injector.instanceOf[MessageConnector]

  val correlationId = "correlationId"

  val request = FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correlationId)

  implicit val hc = HeaderCarrierConverter.fromRequest(request)
  "getMessages" - {

    val timestamp = LocalDateTime.of(2024, 3, 2, 12, 30, 45, 100).toInstant(ZoneOffset.UTC)
    val ern       = "someErn"
    val url       = s"/emcs/messages/v1/show-new-messages?exciseregistrationnumber=$ern"

    "must return an empty list when the response from the server contains no messages" in {

      val response = EISConsumptionResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        message = base64Encode(emptyNewMessageDataXml.toString)
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val batchId = UUID.randomUUID().toString
      val result  = connector.getNewMessages(ern, batchId, None)(hc).futureValue

      result.messages mustBe empty
      result.messageCount mustBe 0
    }

    "must use the provided correlation id if one exists" in {
      val response = EISConsumptionResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        message = base64Encode(emptyNewMessageDataXml.toString)
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val batchId = UUID.randomUUID().toString
      val result  = connector.getNewMessages(ern, batchId, None)(hc).futureValue

      //This passes as we get an OK from the wiremockserver

    }

    "must generate a new correlation id if none provided" in {
      val response = EISConsumptionResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        message = base64Encode(emptyNewMessageDataXml.toString)
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val batchId = UUID.randomUUID().toString
      val result  = connector.getNewMessages(ern, batchId, None)(HeaderCarrier()).failed.futureValue

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      //If successful, we've matched on *not* correlation id
      connector.getNewMessages(ern, batchId, None)(HeaderCarrier()).futureValue

    }

    "must return messages when the response from the server contains them" in {

      val newMessagesDataResponse =
        scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(newMessageXmlWithIE704.toString))
      val ie704Message            = messageFactory.createIEMessage(newMessagesDataResponse.Messages.messagesoption.head)

      val response = EISConsumptionResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        message = base64Encode(newMessageXmlWithIE704.toString)
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val batchId = UUID.randomUUID().toString
      val result  = connector.getNewMessages(ern, batchId, None)(hc).futureValue

      result.messages mustBe Seq(ie704Message)
      result.messageCount mustBe 1

    }

    "must emit MessageProcessingSuccess when able to process the incoming messages" in {
      val newMessagesDataResponse =
        scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(newMessageXmlWithIE704.toString))
      val ie704Message            = messageFactory.createIEMessage(newMessagesDataResponse.Messages.messagesoption.head)

      val response = EISConsumptionResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        message = base64Encode(newMessageXmlWithIE704.toString)
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val getMessagesResponse = GetMessagesResponse(Seq(ie704Message), 1)

      val batchId = UUID.randomUUID().toString
      connector.getNewMessages(ern, batchId, None)(hc).futureValue

      verify(auditService, times(1))
        .messageProcessingSuccess(eqTo(ern), eqTo(getMessagesResponse), eqTo(batchId), eqTo(None))(any)
    }

    "must emit MessageProcessingFailure when unable to process any messages" in {
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(500)
          )
      )

      val failureReason = "Invalid status returned"

      val batchId = UUID.randomUUID().toString
      connector.getNewMessages(ern, batchId, None)(hc).failed.futureValue

      verify(auditService, times(1))
        .messageProcessingFailure(eqTo(ern), eqTo(failureReason), eqTo(batchId), eqTo(None))(any)
    }

    "must audit each message received from the server - old auditing" in {

      val newMessagesDataResponse =
        scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(newMessageWith818And802.toString))
      val ie818Message            = messageFactory.createIEMessage(newMessagesDataResponse.Messages.messagesoption.head)
      val ie802Message            = messageFactory.createIEMessage(newMessagesDataResponse.Messages.messagesoption(1))

      val response = EISConsumptionResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        message = base64Encode(newMessageWith818And802.toString)
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val batchId = UUID.randomUUID().toString
      connector.getNewMessages(ern, batchId, None)(hc).futureValue

      verify(auditService).auditMessage(ie818Message)(hc)
      verify(auditService).auditMessage(ie802Message)(hc)
    }

    "must fail when the server responds with an unexpected status" in {

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val batchId       = UUID.randomUUID().toString
      val failureReason = "Invalid status returned"

      val result = connector.getNewMessages(ern, batchId, None)(hc).failed.futureValue

      result mustBe a[GetMessagesException]
      result.getMessage mustBe s"Failed to get messages for ern: $ern, cause: $failureReason"
    }

    "must fail when the server responds with a non-json body" in {

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("foobar")
          )
      )

      val batchId = UUID.randomUUID().toString

      val result = connector.getNewMessages(ern, batchId, None)(hc).failed.futureValue

      result mustBe a[GetMessagesException]
      result.getMessage must include("Unrecognized token 'foobar")
    }

    "must fail when the server responds with an invalid json body" in {

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("{}")
          )
      )

      val batchId = UUID.randomUUID().toString

      val result = connector.getNewMessages(ern, batchId, None)(hc).failed.futureValue

      result mustBe a[GetMessagesException]
      result.getMessage must include("JsResultException")

    }

    "must fail when the connection fails" in {

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
          )
      )

      val batchId = UUID.randomUUID().toString

      val result = connector.getNewMessages(ern, batchId, None)(hc).failed.futureValue

      result mustBe a[GetMessagesException]
      result.getMessage must include("Remotely closed")

    }
  }

  "acknowledgeMessages" - {

    val timestamp = LocalDateTime.of(2024, 3, 2, 12, 30, 45, 100).toInstant(ZoneOffset.UTC)
    val ern       = "someErn"
    val url       = s"/emcs/messages/v1/message-receipt?exciseregistrationnumber=$ern"
    val batchId   = UUID.randomUUID().toString
    val jobId     = UUID.randomUUID().toString

    "must return a future of the success response when the server responds with OK" in {
      val response = MessageReceiptSuccessResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        recordsAffected = 1
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      connector.acknowledgeMessages(ern, batchId, Some(jobId))(hc).futureValue mustBe response

    }

    "must use the provided correlation id if one exists" in {
      val response = MessageReceiptSuccessResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        recordsAffected = 1
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      //Passes if we get an OK result as the correlationId has been matched
      connector.acknowledgeMessages(ern, batchId, Some(jobId))(hc).futureValue

    }

    "must generate a new correlation id if none provided" in {
      val response = MessageReceiptSuccessResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        recordsAffected = 1
      )

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      //Should fail as it shouldn't match
      connector.acknowledgeMessages(ern, batchId, Some(jobId))(HeaderCarrier()).failed.futureValue

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      //Should succeed as it matches not being correlationId
      connector.acknowledgeMessages(ern, batchId, Some(jobId))(HeaderCarrier()).futureValue

    }

    "must fail when the server responds with an unexpected status" in {
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      connector.acknowledgeMessages(ern, batchId, Some(jobId))(hc).failed.futureValue

    }

    "must fail when the server responds with a non-json body" in {

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("foobar")
          )
      )

      connector.acknowledgeMessages(ern, batchId, Some(jobId))(hc).failed.futureValue

    }

    "must fail when the server responds with an invalid json body" in {

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("{}")
          )
      )

      connector.acknowledgeMessages(ern, batchId, Some(jobId))(hc).failed.futureValue

    }

    "must fail when the connection fails" in {

      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
          )
      )

      connector.acknowledgeMessages(ern, batchId, Some(jobId))(hc).failed.futureValue
    }
  }

  "getMessageException" - {
    "getStackTrace should return underlying cause stacktrace" in {
      val input  = new Throwable("message")
      val sut    = GetMessagesException("ern", input)
      val result = sut.getStackTrace

      result mustBe input.getStackTrace
    }
  }

  private def base64Encode(string: String): String =
    Base64.getEncoder.encodeToString(string.getBytes(StandardCharsets.UTF_8))
}
