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
import org.mockito.{Mockito, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, UNPROCESSABLE_ENTITY}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, NewMessagesXml, TraderMovementXmlGenerator, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.{IE801, IE818}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE801Message, IE818Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, CorrelationIdService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Base64

class TraderMovementConnectorSpec
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
    Mockito.reset(correlationIdService)
  }

  private val correlationIdService = mock[CorrelationIdService]
  private val mockDateTimeService = mock[DateTimeService]
  private val auditService = mock[AuditService]
  private val xmlGenerator = new TraderMovementXmlGenerator(XmlMessageGeneratorFactory)
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

  override lazy val app: Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.eis.port" -> wireMockPort,
        "microservice.services.eis.movement-bearer-token" -> "some-bearer"
      )
      .overrides(
        bind[CorrelationIdService].toInstance(correlationIdService),
        bind[DateTimeService].toInstance(mockDateTimeService),
        bind[AuditService].toInstance(auditService)
      )
      .build()
  }

  private lazy val connector = app.injector.instanceOf[TraderMovementConnector]

  "getMovementMessages" - {

    val hc = HeaderCarrier()
    val correlationId = "correlationId"
    val timestamp = LocalDateTime.of(2024, 3, 2, 12, 30, 45, 100).toInstant(ZoneOffset.UTC)
    val ern = "someErn"
    val arc = "someArc"
    val url = s"/emcs/movements/v1/trader-movement?exciseregistrationnumber=$ern&arc=$arc"

    "must return an empty list when the response from the server is a 422" in {

      when(correlationIdService.generateCorrelationId()).thenReturn(correlationId)
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .withHeader("X-Forwarded-Host", equalTo("MDTP"))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .withHeader("Source", equalTo("APIP"))
          .withHeader("DateTime", equalTo(formatter.format(timestamp.atZone(ZoneOffset.UTC))))
          .withHeader("Authorization", equalTo("Bearer some-bearer"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
          )
      )

      val result = connector.getMovementMessages(ern, arc)(hc).futureValue

      result mustBe empty
    }

    "must return messages when the response from the server contains them" in {
      val ie801Params = MessageParams(
        IE801,
        "GB00001",
        Some("testConsignee"),
        Some("23XI00000000000000012"),
        Some("lrnie8158976912")
      )
      val ie818Params = MessageParams(
        IE818,
        "GB00002",
        Some("testConsignee"),
        Some("23XI00000000000000012"),
        Some("lrnie8158976912")
      )
      val traderMovementXml = xmlGenerator.generate(ern, Seq(ie801Params, ie818Params))

      val ie801Message = IE801Message.createFromXml(XmlMessageGeneratorFactory.generate(ern, ie801Params))
      val ie818Message = IE818Message.createFromXml(XmlMessageGeneratorFactory.generate(ern, ie818Params))

      val response = EISConsumptionResponse(
        dateTime = timestamp,
        exciseRegistrationNumber = ern,
        message = base64Encode(traderMovementXml.toString)
      )

      when(correlationIdService.generateCorrelationId()).thenReturn(correlationId)
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(Json.toJson(response)))
          )
      )

      val result = connector.getMovementMessages(ern, arc)(hc).futureValue

      result mustBe Seq(ie801Message, ie818Message)
    }

    "must fail when the server responds with an unexpected status" in {

      when(correlationIdService.generateCorrelationId()).thenReturn(correlationId)
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      connector.getMovementMessages(ern, arc)(hc).failed.futureValue
    }

    "must fail when the server responds with a non-json body" in {

      when(correlationIdService.generateCorrelationId()).thenReturn(correlationId)
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("foobar")
          )
      )

      connector.getMovementMessages(ern, arc)(hc).failed.futureValue
    }

    "must fail when the server responds with an invalid json body" in {

      when(correlationIdService.generateCorrelationId()).thenReturn(correlationId)
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("{}")
          )
      )

      connector.getMovementMessages(ern, arc)(hc).failed.futureValue
    }

    "must fail when the connection fails" in {

      when(correlationIdService.generateCorrelationId()).thenReturn(correlationId)
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
          )
      )

      connector.getMovementMessages(ern, arc)(hc).failed.futureValue
    }
  }

  private def base64Encode(string: String): String =
    Base64.getEncoder.encodeToString(string.getBytes(StandardCharsets.UTF_8))
}
