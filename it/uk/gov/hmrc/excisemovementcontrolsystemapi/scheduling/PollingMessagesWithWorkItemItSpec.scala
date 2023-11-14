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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}
import org.bson.types.ObjectId
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Configuration}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{NewMessagesXml, SchedulingTestData}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.WireMockServerSpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageReceiptResponse, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumberWorkItem, Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ExciseNumberQueueWorkItemRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDateTime}
import java.util.Base64
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DAYS, Duration}

class PollingMessagesWithWorkItemItSpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[WorkItem[ExciseNumberWorkItem]]
  with CleanMongoCollectionSupport
  with WireMockServerSpec
  with NewMessagesXml
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with BeforeAndAfterEach {

  private val showNewMessageUrl = "/apip-emcs/messages/v1/show-new-messages"
  private val messageReceiptUrl = "/apip-emcs/messages/v1/message-receipt?exciseregistrationnumber="
  private val expectedMessage = Seq(
    createMessage(SchedulingTestData.ie801, MessageTypes.IE801.value),
    createMessage(SchedulingTestData.ie818, MessageTypes.IE818.value),
    createMessage(SchedulingTestData.ie802, MessageTypes.IE802.value)
  )

  private lazy val timeService = mock[DateTimeService]
  private val availableBefore = Instant.now
  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val appConfig = mock[AppConfig]
  protected override lazy val repository = new ExciseNumberQueueWorkItemRepository(
    appConfig,
    mongoComponent,
    timeService
  )

  lazy val repo = new MovementRepository(
    mongoComponent,
    appConfig,
    timeService
  )
  protected def appBuilder = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServer ++ Map("mongodb.uri" -> mongoUri))
      .loadConfig(env => Configuration.load(env, Map("config.resource" -> "application.test.conf")))
      .overrides(
        bind[DateTimeService].to(timeService)
      )

  }

  lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    wireMock.resetAll()

    setUpWireMockStubs()
    when(timeService.instant).thenReturn(Instant.parse("2018-11-30T18:35:24.00Z"))
    when(appConfig.getMovementTTLInDays).thenReturn(Duration.create(30, DAYS))
    when(timeService.instant).thenReturn(availableBefore)
  }


  override def afterEach(): Unit = {
    super.afterEach()
    app.stop()
  }

  "Scheduler" should {
    "start Polling show new message copy" in {

      prepareDatabase()
      insert(createWorkItem("1")).futureValue
      insert(createWorkItem("3")).futureValue
      insert(createWorkItem("4")).futureValue

      repo.updateMovement(Movement("token", "1", None, None, Instant.now, Seq.empty)).futureValue
      repo.updateMovement(Movement("token", "3", None, None, Instant.now, Seq.empty)).futureValue
      repo.updateMovement(Movement("token", "4", None, None, Instant.now, Seq.empty)).futureValue

      app


      Thread.sleep(6000)
      eventually { wireMock.verify( getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=1")))}
      eventually { wireMock.verify(getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=3")))}
      eventually { wireMock.verify(getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=4")))}
      eventually {wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}1")))}
      eventually {wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}3")))}
      eventually {wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}4")))}

      val movements = repo.collection.find.toFuture().futureValue

      movements.size mustBe 3
      assertResults(movements.find(_.consignorId.equals("1")).get, Movement("token", "1", None, Some("tokentokentokentokent"), Instant.now, expectedMessage))
      assertResults(movements.find(_.consignorId.equals("3")).get, Movement("token", "3", None, Some("tokentokentokentokent"), Instant.now, expectedMessage.take(1)))
      assertResults(movements.find(_.consignorId.equals("4")).get, Movement("token", "4", None, Some("tokentokentokentokent"), Instant.now, Seq(createMessage(SchedulingTestData.ie704, MessageTypes.IE704.value))))
    }
  }

  private def assertResults(actual: Movement, expected: Movement) = {
    actual.localReferenceNumber mustBe expected.localReferenceNumber
    actual.consignorId mustBe expected.consignorId
    actual.administrativeReferenceCode mustBe expected.administrativeReferenceCode
    actual.consigneeId mustBe expected.consigneeId
    decodeAndCleanUpMessage(actual.messages) mustBe decodeAndCleanUpMessage(expected.messages)
  }

  private def decodeAndCleanUpMessage(messages: Seq[Message]): Seq[String] = {
    messages
      .map(o => Base64.getDecoder.decode(o.encodedMessage).map(_.toChar).mkString)
      .map(cleanUpString(_))
  }

  private def setUpWireMockStubs(): Unit = {
    stubMultipleShowNewMessageRequest("1")
    stubShowNewMessageRequestForConsignorId3
    stubShowNewMessageRequestForConsignorId4
    stubMessageReceiptRequest("1")
    stubMessageReceiptRequest("3")
    stubMessageReceiptRequest("4")

  }

  private def stubShowNewMessageRequestForConsignorId3 = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=3")
        .inScenario(s"requesting-new-message-for-ern-3")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            "3",
            Base64.getEncoder.encodeToString(newMessageWithIE801.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
        .willSetStateTo(s"new-message-response-for-ern-3")
    )

    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=3")
        .inScenario("requesting-new-message-for-ern-3")
        .whenScenarioStateIs(s"new-message-response-for-ern-3")
        .willSetStateTo("end-of-stubbing")
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            "3",
            Base64.getEncoder.encodeToString(emptyNewMessageDataXml.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
    )
  }

  private def stubShowNewMessageRequestForConsignorId4 = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=4")
        .inScenario(s"requesting-new-message-for-ern-4")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            "4",
            Base64.getEncoder.encodeToString(newMessageXmlWithIE704.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
        .willSetStateTo(s"new-message-response-for-ern-4")
    )

    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=4")
        .inScenario("requesting-new-message-for-ern-4")
        .whenScenarioStateIs(s"new-message-response-for-ern-4")
        .willSetStateTo("end-of-stubbing")
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            "4",
            Base64.getEncoder.encodeToString(emptyNewMessageDataXml.toString().getBytes(StandardCharsets.UTF_8)),
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
          EISConsumptionResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            exciseNumber,
            Base64.getEncoder.encodeToString(newMessageWithIE801.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
        .willSetStateTo("new-message-response")
    )

    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .inScenario("requesting-new-message")
        .whenScenarioStateIs("new-message-response")
        .willSetStateTo("show-empty-message")
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            exciseNumber,
            Base64.getEncoder.encodeToString(newMessageWith818And802.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
    )

    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .inScenario("requesting-new-message")
        .whenScenarioStateIs("show-empty-message")
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            exciseNumber,
            Base64.getEncoder.encodeToString(emptyNewMessageDataXml.toString().getBytes(StandardCharsets.UTF_8)),
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

  private def cleanUpString(str: String): String = {
    str.replaceAll("[\\t\\n\\r\\s]+", "")
  }

  private def createWorkItem(ern: String): WorkItem[ExciseNumberWorkItem] = {
    WorkItem(
      id           = new ObjectId(),
      receivedAt   = availableBefore.minusSeconds(60),
      updatedAt    = availableBefore.minusSeconds(60),
      availableAt  = availableBefore.minusSeconds(60),
      status       = ProcessingStatus.ToDo,
      failureCount = 0,
      item         = ExciseNumberWorkItem(ern)
    )
  }

  private def createMessage(xml: String, messageType: String): Message = {
    Message(
      Base64.getEncoder.encodeToString(xml.getBytes(StandardCharsets.UTF_8)),
      messageType,
      timeService
    )
  }
}

