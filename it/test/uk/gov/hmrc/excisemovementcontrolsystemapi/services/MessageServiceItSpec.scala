/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.{Mockito, MockitoSugar}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageConnector, TraderMovementConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageReceiptSuccessResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.{IE704, IE801, IE802, IE813, IE818}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IE704Message, IE801Message, IE818Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

class MessageServiceItSpec
    extends AnyFreeSpec
    with Matchers
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[Movement]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar
    with OptionValues {

  // For some reason not all indexes are applied when running these tests
  // but we have tests for checking these indexes in the individual repository specs
  override val checkIndexedQueries: Boolean = false

  private val mockMessageConnector        = mock[MessageConnector]
  private val mockDateTimeService         = mock[DateTimeService]
  private val mockCorrelationIdService    = mock[CorrelationIdService]
  private val mockTraderMovementConnector = mock[TraderMovementConnector]

  private val now         = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val newId       = "some-id"
  private val utils       = new EmcsUtils
  private implicit val hc = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockMessageConnector, mockDateTimeService, mockCorrelationIdService, mockTraderMovementConnector)
  }

  override protected lazy val repository: MovementRepository =
    app.injector.instanceOf[MovementRepository]

  private lazy val ernRetrievalRepository: ErnRetrievalRepository =
    app.injector.instanceOf[ErnRetrievalRepository]

  private lazy val service         = app.injector.instanceOf[MessageService]
  private lazy val movementService = app.injector.instanceOf[MovementService]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent),
        bind[MessageConnector].toInstance(mockMessageConnector),
        bind[DateTimeService].toInstance(mockDateTimeService),
        bind[CorrelationIdService].toInstance(mockCorrelationIdService),
        bind[TraderMovementConnector].toInstance(mockTraderMovementConnector)
      )
      .build()

  "MessageService" - {

    "must only add messages once, even if we process the message multiple times" in {

      val hc  = HeaderCarrier()
      val ern = "testErn"
      val lrn = "lrnie8158976912"
      val acknowledgeResponse = MessageReceiptSuccessResponse(now, ern, 1)

      val ie704    =
        XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some(lrn)))
      val messages = Seq(IE704Message.createFromXml(ie704))

      val expectedMovement = Movement(
        newId,
        None,
        lrn,
        ern,
        None,
        None,
        now,
        messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", ern, Set.empty, now))
      )

      when(mockDateTimeService.timestamp()).thenReturn(
        now
      )
      when(mockCorrelationIdService.generateCorrelationId()).thenReturn(newId)

      when(mockMessageConnector.getNewMessages(any, any, any)(any))
        .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))

      when(mockMessageConnector.acknowledgeMessages(any, any, any)(any)).thenReturn(
        Future.failed(new RuntimeException()),
        Future.successful(acknowledgeResponse)
      )

      service.updateMessages(ern, None)(hc).failed.futureValue
      val result1 = repository.getMovementByLRNAndERNIn(lrn, List(ern)).futureValue

      result1 must contain only expectedMovement

      service.updateMessages(ern, Some(now.minus(10, ChronoUnit.MINUTES)))(hc).futureValue
      val result2 = repository.getMovementByLRNAndERNIn(lrn, List(ern)).futureValue

      result1 mustEqual result2

      // For this test, it's important that these are two calls that retrieve messages
      // We don't want the second call being throttled, so this check is added to make sure we're
      // testing the right behaviour
      verify(mockMessageConnector, times(2)).getNewMessages(any, any, any)(any)
    }

    "must only allow a single call at a time" in {

      val hc  = HeaderCarrier()
      val ern = "testErn"
      val lrn = "lrnie8158976912"
      val acknowledgeResponse = MessageReceiptSuccessResponse(now, ern, 1)

      val ie704    =
        XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some(lrn)))
      val messages = Seq(IE704Message.createFromXml(ie704))

      val promise = Promise[Done]()

      when(mockDateTimeService.timestamp()).thenReturn(now)
      when(mockCorrelationIdService.generateCorrelationId()).thenReturn(newId)

      when(mockMessageConnector.getNewMessages(any, any, any)(any))
        .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
      when(mockMessageConnector.acknowledgeMessages(any, any, any)(any))
        .thenReturn(Future.successful(acknowledgeResponse))

      val future  = service.updateMessages(ern, None)(hc)
      val future2 = service.updateMessages(ern, None)(hc)

      promise.success(Done)
      future.futureValue
      future2.futureValue

      verify(mockMessageConnector, times(1)).getNewMessages(any, any, any)(any)
    }

    "must not cause throttled requests to increase the throttle timeout" in {

      val hc  = HeaderCarrier()
      val ern = "testErn"
      val lrn = "lrnie8158976912"
      val acknowledgeResponse = MessageReceiptSuccessResponse(now, ern, 1)

      val ie704    =
        XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some(lrn)))
      val messages = Seq(IE704Message.createFromXml(ie704))

      val timeout = app.configuration.get[Duration]("microservice.services.eis.throttle-cutoff")

      when(mockCorrelationIdService.generateCorrelationId()).thenReturn(newId)

      when(mockMessageConnector.getNewMessages(any, any, any)(any))
        .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
      when(mockMessageConnector.acknowledgeMessages(any, any, any)(any))
        .thenReturn(Future.successful(acknowledgeResponse))

      when(mockDateTimeService.timestamp()).thenReturn(now)
      service.updateMessages(ern, ernRetrievalRepository.getLastRetrieved(ern).futureValue)(hc).futureValue
      when(mockDateTimeService.timestamp()).thenReturn(now.plus(timeout.dividedBy(2)))
      service.updateMessages(ern, ernRetrievalRepository.getLastRetrieved(ern).futureValue)(hc).futureValue
      when(mockDateTimeService.timestamp()).thenReturn(now.plus(timeout.plus(Duration.ofSeconds(1))))
      service.updateMessages(ern, ernRetrievalRepository.getLastRetrieved(ern).futureValue)(hc).futureValue

      verify(mockMessageConnector, times(2)).getNewMessages(any, any, any)(any)
    }

    "must not try to create a movement which already exists" in {

      val consignorErn = "testErn"
      val consigneeErn = "testErn2"
      val lrn          = "lrnie8158976912"
      val arc          = "arc"
      val acknowledgeResponse = MessageReceiptSuccessResponse(now, consignorErn, 1)

      val ie801    = XmlMessageGeneratorFactory.generate(
        consignorErn,
        MessageParams(
          IE801,
          "XI000001",
          consigneeErn = Some(consigneeErn),
          localReferenceNumber = Some(lrn),
          administrativeReferenceCode = Some(arc)
        )
      )
      val ie818    = XmlMessageGeneratorFactory.generate(
        consignorErn,
        MessageParams(
          IE818,
          "XI000002",
          consigneeErn = Some(consigneeErn),
          localReferenceNumber = None,
          administrativeReferenceCode = Some(arc)
        )
      )
      val messages = Seq(IE801Message.createFromXml(ie801), IE818Message.createFromXml(ie818))

      val initialMovement = Movement(
        None,
        lrn,
        consignorErn,
        Some(consigneeErn),
        None,
        lastUpdated = now
      )

      val expectedMovement = initialMovement.copy(
        messages = Seq(
          Message(utils.encode(messages.head.toXml.toString()), "IE801", "XI000001", consignorErn, Set.empty, now),
          Message(utils.encode(messages.head.toXml.toString()), "IE801", "XI000001", consigneeErn, Set.empty, now),
          Message(utils.encode(messages(1).toXml.toString()), "IE818", "XI000002", consignorErn, Set.empty, now)
        ),
        administrativeReferenceCode = Some(arc)
      )

      when(mockDateTimeService.timestamp()).thenReturn(now)

      when(mockCorrelationIdService.generateCorrelationId()).thenAnswer(UUID.randomUUID().toString)

      when(mockMessageConnector.getNewMessages(any, any, any)(any)).thenReturn(
        Future.successful(GetMessagesResponse(Seq(messages(1)), 1))
      )

      when(mockMessageConnector.acknowledgeMessages(any, any, any)(any))
        .thenReturn(Future.successful(acknowledgeResponse))

      when(mockTraderMovementConnector.getMovementMessages(any, any)(any)).thenReturn(Future.successful(messages))

      movementService.saveNewMovement(initialMovement).futureValue.isRight mustBe true
      service.updateMessages(consignorErn, None)(hc).futureValue

      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorErn)).futureValue

      result.length mustBe 1
      result.head mustEqual expectedMovement
    }

    "must not create a new movement when an existing movement can be found in the database via consignor ERN and LRN" in {

      val hc           = HeaderCarrier()
      val consignorErn = "testErn"
      val consigneeErn = "testErn2"
      val lrn          = "lrnie8158976912"
      val arc          = "arc"
      val acknowledgeResponse = MessageReceiptSuccessResponse(now, consignorErn, 1)

      val ie801    = XmlMessageGeneratorFactory.generate(
        consignorErn,
        MessageParams(
          IE801,
          "XI000001",
          consigneeErn = Some(consigneeErn),
          localReferenceNumber = Some(lrn),
          administrativeReferenceCode = Some(arc)
        )
      )
      val messages = Seq(IE801Message.createFromXml(ie801))

      val initialMovement = Movement(
        None,
        lrn,
        consignorErn,
        None,
        None,
        lastUpdated = now
      )

      val expectedMovement = initialMovement.copy(
        messages = Seq(
          Message(utils.encode(messages.head.toXml.toString()), "IE801", "XI000001", consignorErn, Set.empty, now),
          Message(utils.encode(messages.head.toXml.toString()), "IE801", "XI000001", consigneeErn, Set.empty, now)
        ),
        administrativeReferenceCode = Some(arc),
        consigneeId = Some(consigneeErn)
      )

      when(mockDateTimeService.timestamp()).thenReturn(now)

      when(mockCorrelationIdService.generateCorrelationId()).thenAnswer(UUID.randomUUID().toString)

      when(mockMessageConnector.getNewMessages(any, any, any)(any)).thenReturn(
        Future.successful(GetMessagesResponse(Seq(messages.head), 1))
      )

      when(mockMessageConnector.acknowledgeMessages(any, any, any)(any))
        .thenReturn(Future.successful(acknowledgeResponse))

      movementService.saveNewMovement(initialMovement)(hc).futureValue.isRight mustBe true
      service.updateMessages(consigneeErn, None)(hc).futureValue

      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorErn)).futureValue

      result.length mustBe 1
      result.head mustEqual expectedMovement
    }
  }
}
