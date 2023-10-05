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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.mockito.MockitoSugar.when
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.{GuiceOneServerPerSuite, GuiceOneServerPerTest}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Configuration}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageReceiptResponse, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumber
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ExciseNumberRepository, MovementMessageRepository}

import java.time.LocalDateTime

class SchedulePollingNewMessagesItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with WireMockServerSpec
  with RepositoryTestStub {

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

  "Scheduler" should {

    "start Polling show new message" in {
      //todo: check if we save to movement db if successful

      wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=1")))
      wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=3")))
      wireMock.verify(getRequestedFor(urlEqualTo("/apip-emcs/messages/v1/show-new-messages?exciseregistrationnumber=4")))
    }

    "Should poll message receipt api" in {
      Thread.sleep(1000)
      wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}1")))
      wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}3")))
      wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}4")))
    }
  }

  private def stubShowNewMessageRequest(exciseNumber: String) = {
    wireMock.stubFor(
      get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .willReturn(ok().withBody(Json.toJson(
          ShowNewMessageResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            exciseNumber,
            "message"
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
