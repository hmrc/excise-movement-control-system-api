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
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.nio.charset.StandardCharsets
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime}
import java.util.Base64
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DAYS, Duration, MINUTES}

class PollingNewMessagesWithWorkItemJobItSpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[WorkItem[ExciseNumberWorkItem]]
  with CleanMongoCollectionSupport
  with WireMockServerSpec
  with NewMessagesXml
  with MockitoSugar
  with ScalaFutures
  with Eventually
  with IntegrationPatience
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val showNewMessageUrl = "/apip-emcs/messages/v1/show-new-messages"
  private val messageReceiptUrl = "/apip-emcs/messages/v1/message-receipt?exciseregistrationnumber="

  private lazy val timeService = mock[TimestampSupport]
  // The DB truncates it to milliseconds so to make exact comparisons in the asserts we need to ditch the nanos
  private val availableBefore = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  when(timeService.timestamp()).thenReturn(availableBefore)

  private val expectedMessage = Seq(
    createMessage(SchedulingTestData.ie801, MessageTypes.IE801.value),
    createMessage(SchedulingTestData.ie818, MessageTypes.IE818.value),
    createMessage(SchedulingTestData.ie802, MessageTypes.IE802.value)
  )


  // This is used by repository and movementRepository to set the databases before
  // the application start. Once the application has started, the app will load a real
  // instance of AppConfig using the application.test.conf
  private lazy val mongoAppConfig = mock[AppConfig]
  when(mongoAppConfig.movementTTL).thenReturn(Duration.create(30, DAYS))
  when(mongoAppConfig.workItemTTL).thenReturn(Duration.create(30, DAYS))
  when(mongoAppConfig.workItemInProgressTimeOut).thenReturn(Duration.create(5, MINUTES))

  protected override lazy val repository = new ExciseNumberQueueWorkItemRepository(
    mongoAppConfig,
    mongoComponent,
    timeService
  )

  protected def appBuilder: GuiceApplicationBuilder = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServer ++ Map("mongodb.uri" -> mongoUri))
      .loadConfig(env => Configuration.load(env, Map("config.resource" -> "application.test.conf")))
      .overrides(
        bind[TimestampSupport].to(timeService)
      )

  }

  lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    wireMock.resetAll()

    prepareDatabase()

  }

  override def afterEach(): Unit = {
    super.afterEach()

    //TODO Doing this breaks the timestamp check in "if fails three times work item marked as ToDo with a slow interval"
    // Do we still need to do this??
    //app.stop()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    dropDatabase()
  }

  "Scheduler" should {

    "start Polling show new message" in {

      val movementRepository = new MovementRepository(
        mongoComponent,
        mongoAppConfig,
        timeService
      )

      setUpWireMockStubs()

      insert(createWorkItem("1")).futureValue
      insert(createWorkItem("3")).futureValue
      insert(createWorkItem("4")).futureValue

      movementRepository.saveMovement(Movement("token", "1", None, None, Instant.now, Seq.empty)).futureValue
      movementRepository.saveMovement(Movement("token", "3", None, None, Instant.now, Seq.empty)).futureValue
      movementRepository.saveMovement(Movement("token", "4", None, None, Instant.now, Seq.empty)).futureValue

      // start application
      app

      // todo: not a very good way to wait for the thread to do is job. Tried eventually but it does not
      // work. Try to find a better way.
      Thread.sleep(6000)

      eventually {
        wireMock.verify(getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=1")))
      }
      eventually {
        wireMock.verify(getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=3")))
      }
      eventually {
        wireMock.verify(getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=4")))
      }
      eventually {
        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}1")))
      }
      eventually {
        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}3")))
      }
      eventually {
        wireMock.verify(putRequestedFor(urlEqualTo(s"${messageReceiptUrl}4")))
      }


      val movements = movementRepository.collection.find().toFuture().futureValue

      movements.size mustBe 3
      assertResults(movements.find(_.consignorId.equals("1")).get, Movement("token", "1", None, Some("tokentokentokentokent"), Instant.now, expectedMessage))
      assertResults(movements.find(_.consignorId.equals("3")).get, Movement("token", "3", None, Some("tokentokentokentokent"), Instant.now, expectedMessage.take(1)))
      assertResults(movements.find(_.consignorId.equals("4")).get, Movement("token", "4", None, Some("tokentokentokentokent"), Instant.now, Seq(createMessage(SchedulingTestData.ie704, MessageTypes.IE704.value))))
    }

    "do not call EIS if nothing is in the work queue " in {

      val movementRepository = new MovementRepository(
        mongoComponent,
        mongoAppConfig,
        timeService
      )

      setUpWireMockStubs()

      movementRepository.saveMovement(Movement("token", "1", None, None, Instant.now, Seq.empty)).futureValue
      movementRepository.saveMovement(Movement("token", "3", None, None, Instant.now, Seq.empty)).futureValue
      movementRepository.saveMovement(Movement("token", "4", None, None, Instant.now, Seq.empty)).futureValue

      // start application
      app

      eventually {
        wireMock.verify(0, getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=1")))
      }
      eventually {
        wireMock.verify(0, getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=3")))
      }
      eventually {
        wireMock.verify(0, getRequestedFor(urlEqualTo(s"$showNewMessageUrl?exciseregistrationnumber=4")))
      }
      eventually {
        wireMock.verify(0, putRequestedFor(urlEqualTo(s"${messageReceiptUrl}1")))
      }
      eventually {
        wireMock.verify(0, putRequestedFor(urlEqualTo(s"${messageReceiptUrl}3")))
      }
      eventually {
        wireMock.verify(0, putRequestedFor(urlEqualTo(s"${messageReceiptUrl}4")))
      }


      val movements = movementRepository.collection.find().toFuture().futureValue

      movements.size mustBe 3
      assertResults(movements.find(_.consignorId.equals("1")).get, Movement("token", "1", None, None, Instant.now, Seq.empty))
      assertResults(movements.find(_.consignorId.equals("3")).get, Movement("token", "3", None, None, Instant.now, Seq.empty))
      assertResults(movements.find(_.consignorId.equals("4")).get, Movement("token", "4", None, None, Instant.now, Seq.empty))
    }

    "if fails three times work item marked as ToDo with a slow interval" in {

      val movementRepository = new MovementRepository(
        mongoComponent,
        mongoAppConfig,
        timeService
      )

      stubForThrowingError("1")

      val createdWorkItem = createWorkItem("1")
      insert(createdWorkItem).futureValue

      movementRepository.saveMovement(Movement("token", "1", None, None, Instant.now, Seq.empty)).futureValue

      // start application
      app

      // todo: not a very good way to wait for the thread to do is job. Tried eventually but it does not
      // work. Try to find a better way.
      Thread.sleep(6000)

      val movements = movementRepository.collection.find().toFuture().futureValue

      movements.size mustBe 1
      assertResults(movements.find(_.consignorId.equals("1")).get, Movement("token", "1", None, None, Instant.now, Seq.empty))

      val workItems = findAll().futureValue

      val workItem = workItems.find(_.item.exciseNumber.equals("1")).get
      workItem.status mustBe ProcessingStatus.ToDo
      workItem.availableAt mustBe createdWorkItem.availableAt.plusSeconds(2 * 60)

    }

    "if fails mark work item as failed so can be retried" in {

      val movementRepository = new MovementRepository(
        mongoComponent,
        mongoAppConfig,
        timeService
      )

      stubForThrowingError("1")

      insert(createWorkItem("1")).futureValue

      movementRepository.saveMovement(Movement("token", "1", None, None, Instant.now, Seq.empty)).futureValue

      // start application
      app

      // todo: not a very good way to wait for the thread to do is job. Tried eventually but it does not
      // work. Try to find a better way.
      Thread.sleep(100)

      val movements = movementRepository.collection.find().toFuture().futureValue

      movements.size mustBe 1
      assertResults(movements.find(_.consignorId.equals("1")).get, Movement("token", "1", None, None, Instant.now, Seq.empty))

      val workItems = findAll().futureValue

      workItems.find(_.item.exciseNumber.equals("1")).get.status mustBe ProcessingStatus.ToDo
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
      .map(cleanUpString)
  }

  private def setUpWireMockStubs(): Unit = {
    stubShowNewMessageRequestForConsignorId1()
    stubShowNewMessageRequestForConsignorId3()
    stubShowNewMessageRequestForConsignorId4()
    stubMessageReceiptRequest("1")
    stubMessageReceiptRequest("3")
    stubMessageReceiptRequest("4")

  }

  private def stubShowNewMessageRequestForConsignorId3(): Unit = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=3")
        .inScenario(s"requesting-new-message-for-ern-3")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            "3",
            Base64.getEncoder.encodeToString(newMessageWithIE801().toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
        .willSetStateTo(s"show-empty-message")
    )

    stubForEmptyMessageData("3")
  }

  private def stubShowNewMessageRequestForConsignorId4(): Unit = {
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
        .willSetStateTo(s"show-empty-message")
    )

    stubForEmptyMessageData("4")
  }

  private def stubShowNewMessageRequestForConsignorId1(): Unit = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=1")
        .inScenario("requesting-new-message-for-ern-1")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2023, 1, 2, 3, 4, 5),
            "1",
            //Set the new message count so it will poll again and get the item below
            Base64.getEncoder.encodeToString(newMessageWithIE801(11).toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
        .willSetStateTo("show-second-response")
    )

    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=1")
        .inScenario("requesting-new-message-for-ern-1")
        .whenScenarioStateIs("show-second-response")
        .willReturn(ok().withBody(Json.toJson(
          EISConsumptionResponse(
            LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            "1",
            Base64.getEncoder.encodeToString(newMessageWith818And802.toString().getBytes(StandardCharsets.UTF_8)),
          )).toString()
        ))
        .willSetStateTo("show-empty-message")
    )

    stubForEmptyMessageData("1")
  }

  private def stubForThrowingError(exciseNumber: String) = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .willReturn(serverError().withBody("Internal server error"))
    )
  }

  private def stubForEmptyMessageData(exciseNumber: String): Unit = {
    wireMock.stubFor(
      WireMock.get(s"$showNewMessageUrl?exciseregistrationnumber=$exciseNumber")
        .inScenario(s"requesting-new-message-for-ern-$exciseNumber")
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

    val sixtySecsAgo = availableBefore.minusSeconds(60)

    TestUtils.createWorkItem(
      ern = ern,
      receivedAt = sixtySecsAgo,
      updatedAt = sixtySecsAgo,
      availableAt = sixtySecsAgo,
      fastPollRetries = 3
    )

  }

  private def createMessage(xml: String, messageType: String): Message = {
    Message(
      Base64.getEncoder.encodeToString(xml.getBytes(StandardCharsets.UTF_8)),
      messageType,
      timeService.timestamp()
    )
  }
}

