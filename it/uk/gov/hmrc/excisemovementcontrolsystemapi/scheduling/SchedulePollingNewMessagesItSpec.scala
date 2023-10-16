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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito
import org.mockito.MockitoSugar.{verify, when, reset => mockitoReset}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Waiters.timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Configuration}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data._
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageReceiptResponse, MessageTypes, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumber, Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.DateTimeService

import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDateTime}
import java.util.Base64
import scala.concurrent.Future
import scala.xml.NodeSeq


class SchedulePollingNewMessagesItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with WireMockServerSpec
  with RepositoryTestStub
  with GetNewMessagesXml
  with MockitoSugar
  with BeforeAndAfterEach  {

  private val showNewMessageUrl = "/apip-emcs/messages/v1/show-new-messages"
  private val messageReceiptUrl = "/apip-emcs/messages/v1/message-receipt?exciseregistrationnumber="
  private val expectedMessages: Seq[String] = Seq(
    cleanUpString(Ie704XmlMessage.IE704.toString),
    cleanUpString(Ie801XmlMessage.IE801.toString),
    cleanUpString(Ie802XmlMessage.IE802.toString)
  )

  private val expectedMessages1: Seq[String] =
    expectedMessages :+
      cleanUpString(Ie810XmlMessage.IE810.toString) :+
      cleanUpString(Ie818XmlMessage.IE818.toString)


  private lazy val timeService = mock[DateTimeService]

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())
    when(movementMessageRepository.getMovements).thenReturn(Future.successful(Seq.empty))

    GuiceApplicationBuilder()
      .configure(configureServer)
      .loadConfig(env => Configuration.load(env, Map("config.resource" -> "application.test.conf")))
      .overrides(
        bind[MovementMessageRepository].to(movementMessageRepository),
        bind[DateTimeService].to(timeService)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMock.resetAll()
    mockitoReset(movementMessageRepository, timeService)

    stubMultipleShowNewMessageRequest("1")
    stubShowNewMessageRequest("3")
    stubShowNewMessageRequest("4")
    stubMessageReceiptRequest1("1")
    stubMessageReceiptRequest("3")
    stubMessageReceiptRequest("4")

    when(timeService.now).thenReturn(Instant.parse("2018-11-30T18:35:24.00Z"))
    when(movementMessageRepository.getMovements).thenReturn(Future.successful(createSource))


  }

  "Scheduler" should {
    "start Polling show new message copy" in {
      setUp

      eventually (timeout(Span(1L, Seconds))){wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=1")))}
      eventually { wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=3")))}
      eventually { wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=4")))}

      withClue("save the message to DB") {

        val captor = ArgCaptor[Movement]
        eventually(timeout(Span(3L, Seconds))) {
          verify(movementMessageRepository,Mockito.atLeast(4)).save(captor.capture)
        }

        val movements: Map[String, List[Movement]] = captor.values.groupBy(m => m.consignorId)
        assertResultForErn(movements.get("1").get(1), expectedMessages1, "1", "123")
        assertResultForErn(movements.get("3").get(0), expectedMessages, "3", "123")
        assertResultForErn(movements.get("4").get(0), expectedMessages, "4", "123")
      }
    }

    "poll message receipt api" in {
      eventually(timeout(Span(2L, Seconds))) {
        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}1")))
        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}3")))
        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}4")))
      }
    }
  }

  private def setUp: Unit = {
    val message1 = createMessage(Ie704XmlMessage.IE704, MessageTypes.IE704.value)
    val message2 = createMessage(Ie801XmlMessage.IE801, MessageTypes.IE801.value )
    val message3 = createMessage(Ie802XmlMessage.IE802, MessageTypes.IE802.value)

    val m0 = Movement("123", "1", None, None, Seq.empty)
    val m1 = Movement("123", "1", None, None, Seq(message1, message2, message3))
    val m2 = Movement("123", "3", None, None, Seq.empty)
    val m3 = Movement("123", "4", None, None, Seq.empty)

    when(movementMessageRepository.get(any, eqTo(List("1"))))
      .thenReturn(
        Future.successful(Some(m0)),
        Future.successful(Some(m1)),
        Future.successful(None)
      )

    when(movementMessageRepository.get(any, eqTo(List("3"))))
      .thenReturn(
        Future.successful(Some(m2)),
        Future.successful(None)
      )
    when(movementMessageRepository.get(any, eqTo(List("4"))))
      .thenReturn(
        Future.successful(Some(m3)),
        Future.successful(None)
      )
  }

  private def createMessage(xml: NodeSeq, messageType: String): Message = {
    Message(
      Base64.getEncoder.encodeToString(xml.toString.getBytes(StandardCharsets.UTF_8)),
      messageType,
      timeService
    )
  }
  private def assertResultForErn(
    actual: Movement,
    expectedMessages: Seq[String],
    ern: String,
    lrn: String): Unit = {

    actual.localReferenceNumber mustBe lrn
    actual.consignorId mustBe ern
    val actualMessages = actual.messages.map(m =>
      cleanUpString(new String(Base64.getDecoder.decode(m.encodeMessage), StandardCharsets.UTF_8))
    )
    actualMessages mustBe expectedMessages

  }

  private def stubShowNewMessageRequest(exciseNumber: String) = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .willReturn(ok().withBody(Json.toJson(
          ShowNewMessageResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            exciseNumber,
            Base64.getEncoder.encodeToString(newMessageXml.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
    )
  }

  private def stubMultipleShowNewMessageRequest(exciseNumber: String) = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .inScenario("requesting-new-message")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(ok().withBody(Json.toJson(
          ShowNewMessageResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            exciseNumber,
            Base64.getEncoder.encodeToString(newMessageXml.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
        .willSetStateTo("new-message-response")
    )

    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .inScenario("requesting-new-message")
        .whenScenarioStateIs("new-message-response")
        .willReturn(ok().withBody(Json.toJson(
          ShowNewMessageResponse(
            LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            exciseNumber,
            Base64.getEncoder.encodeToString(newMessageXml2.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
    )
  }

  private def stubMessageReceiptRequest(exciseNumber: String): StubMapping = {
    wireMock.stubFor(
      put(s"$messageReceiptUrl$exciseNumber")
        .willReturn(ok().withBody(Json.toJson(
          MessageReceiptResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            exciseNumber,
            3
          )).toString()
        ))
    )
  }

  private def stubMessageReceiptRequest1(exciseNumber: String): StubMapping = {
    wireMock.stubFor(
      put(s"$messageReceiptUrl$exciseNumber")
        .inScenario("requesting-message-receipt")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(ok().withBody(Json.toJson(
          MessageReceiptResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            exciseNumber,
            3
          )).toString()
        ))
        .willSetStateTo("message-receipt-response")
    )

    wireMock.stubFor(
      put(s"$messageReceiptUrl$exciseNumber")
        .inScenario("requesting-message-receipt")
        .whenScenarioStateIs("message-receipt-response")
        .willReturn(ok().withBody(Json.toJson(
          MessageReceiptResponse(
            LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            exciseNumber,
            2
          )).toString()
        ))
    )
  }

  private def createSource: Seq[Movement] = {
    Seq(
      Movement("2", "1", None),
      Movement("3", "3", None),
      Movement("4", "4", None)
    )
  }

  private def cleanUpString(str: String): String = {
    str.replaceAll("[\\t\\n\\r\\s]+", "")
  }
}
