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
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, ok, put, reset, putRequestedFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset => mockitoReset, times, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Waiters.timeout
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Configuration}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.GetNewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.{IE704, IE801, IE802}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageReceiptResponse, MessageTypes, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumber, Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ExciseNumberRepository, MovementMessageRepository}

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class SchedulePollingNewMessagesItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with WireMockServerSpec
  with RepositoryTestStub
  with GetNewMessagesXml
  with BeforeAndAfterEach  {

  private val showNewMessageUrl = "/apip-emcs/messages/v1/show-new-messages"
  private val messageReceiptUrl = "/apip-emcs/messages/v1/message-receipt?exciseregistrationnumber="

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())
    when(exciseNumberRepository.getAll).thenReturn(createSource)

    //This need to be set before the scheduler start
    stubShowNewMessageRequest("1")
    stubShowNewMessageRequest("3")
    stubShowNewMessageRequest("4")
    stubMessageReceiptRequest("1")
    stubMessageReceiptRequest("3")
    stubMessageReceiptRequest("4")

    GuiceApplicationBuilder()
      .configure(configureServer)
      .loadConfig(env => Configuration.load(env, Map("config.resource" -> "application.test.conf")))
      .overrides(
        bind[MovementMessageRepository].to(movementMessageRepository),
        bind[ExciseNumberRepository].to(exciseNumberRepository)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockitoReset(movementMessageRepository)
  }

  "Scheduler" should {

    "start Polling show new message" in {
      //todo: check if we save to movement db if successful
      when(movementMessageRepository.get(any, eqTo(List("1"))))
        .thenReturn(Future.successful(Some(Movement("123", "1", None, None, Seq.empty))))
      when(movementMessageRepository.get(any, eqTo(List("3"))))
        .thenReturn(Future.successful(Some(Movement("123", "3", None, None, Seq.empty))))
      when(movementMessageRepository.get(any, eqTo(List("4"))))
        .thenReturn(Future.successful(Some(Movement("123", "4", None, None, Seq.empty))))

      wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=1")))
      wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=3")))
      wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=4")))

      val encoder = Base64.getEncoder
      val expectedMessages = Seq(
        encoder.encodeToString(IE704.toString.getBytes(StandardCharsets.UTF_8)),
        encoder.encodeToString(IE801.toString.getBytes(StandardCharsets.UTF_8)),
        encoder.encodeToString(IE802.toString.getBytes(StandardCharsets.UTF_8))
      )

      withClue("save the message to DB") {
        eventually(timeout(Span(5L, Seconds))) {

          val captor = ArgCaptor[Movement]
          verify(movementMessageRepository, times(3)).save(captor.capture)

          val movements = captor.value
          movements.localReferenceNumber mustBe "123"
          movements.consignorId mustBe "1"
          movements.messages mustBe expectedMessages
        }
      }
    }

//    "Should poll message receipt api" in {
//      eventually(timeout(Span(5L, Seconds))) {
//        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}1")))
//        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}3")))
//        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}4")))
//      }
//    }
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

  private def stubMessageReceiptRequest(exciseNumber: String): StubMapping = {
    wireMock.stubFor(
      put(s"$messageReceiptUrl$exciseNumber")
        .willReturn(ok().withBody(Json.toJson(
          MessageReceiptResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            exciseNumber,
            10
          )).toString()
        ))
    )
  }

  private def createSource: Source[ExciseNumber, NotUsed] = {
    Source(
      Seq(
        ExciseNumber("1", "2"),
        ExciseNumber("3", "3"),
        ExciseNumber("4", "4")
      )
    )
  }
}
