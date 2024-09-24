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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.{IE704, IE801, IE802, IE813, IE818}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IE704Message, IE801Message, IE818Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Duration, Instant}
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.xml.{NodeSeq, Utility}



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

  private val mockMessageConnector = mock[MessageConnector]
  private val mockDateTimeService = mock[DateTimeService]
  private val mockCorrelationIdService = mock[CorrelationIdService]
  private val mockTraderMovementConnector = mock[TraderMovementConnector]

  private val now = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val newId = "some-id"
  private val utils = new EmcsUtils

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockMessageConnector, mockDateTimeService, mockCorrelationIdService, mockTraderMovementConnector)
  }

  override protected lazy val repository: MovementRepository =
    app.injector.instanceOf[MovementRepository]

  private lazy val ernRetrievalRepository: ErnRetrievalRepository =
    app.injector.instanceOf[ErnRetrievalRepository]

  private lazy val service = app.injector.instanceOf[MessageService]
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

      val hc = HeaderCarrier()
      val ern = "testErn"
      val lrn = "lrnie8158976912"

      val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some(lrn)))
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

      when(mockMessageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))

      when(mockMessageConnector.acknowledgeMessages(any)(any)).thenReturn(
        Future.failed(new RuntimeException()),
        Future.successful(Done)
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
      verify(mockMessageConnector, times(2)).getNewMessages(any)(any)
    }

    "must only allow a single call at a time" in {

      val hc = HeaderCarrier()
      val ern = "testErn"
      val lrn = "lrnie8158976912"

      val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some(lrn)))
      val messages = Seq(IE704Message.createFromXml(ie704))

      val promise = Promise[Done]()

      when(mockDateTimeService.timestamp()).thenReturn(now)
      when(mockCorrelationIdService.generateCorrelationId()).thenReturn(newId)

      when(mockMessageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
      when(mockMessageConnector.acknowledgeMessages(any)(any)).thenReturn(Future(promise.future).flatten)

      val future = service.updateMessages(ern, None)(hc)
      val future2 = service.updateMessages(ern, None)(hc)

      promise.success(Done)
      future.futureValue
      future2.futureValue

      verify(mockMessageConnector, times(1)).getNewMessages(any)(any)
    }

    "must not cause throttled requests to increase the throttle timeout" in {

      val hc = HeaderCarrier()
      val ern = "testErn"
      val lrn = "lrnie8158976912"

      val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some(lrn)))
      val messages = Seq(IE704Message.createFromXml(ie704))

      val timeout = app.configuration.get[Duration]("microservice.services.eis.throttle-cutoff")

      when(mockCorrelationIdService.generateCorrelationId()).thenReturn(newId)

      when(mockMessageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
      when(mockMessageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

      when(mockDateTimeService.timestamp()).thenReturn(now)
      service.updateMessages(ern, ernRetrievalRepository.getLastRetrieved(ern).futureValue)(hc).futureValue
      when(mockDateTimeService.timestamp()).thenReturn(now.plus(timeout.dividedBy(2)))
      service.updateMessages(ern, ernRetrievalRepository.getLastRetrieved(ern).futureValue)(hc).futureValue
      when(mockDateTimeService.timestamp()).thenReturn(now.plus(timeout.plus(Duration.ofSeconds(1))))
      service.updateMessages(ern, ernRetrievalRepository.getLastRetrieved(ern).futureValue)(hc).futureValue

      verify(mockMessageConnector, times(2)).getNewMessages(any)(any)
    }

    "must not try to create a movement which already exists" in {

      val hc = HeaderCarrier()
      val consignorErn = "testErn"
      val consigneeErn = "testErn2"
      val lrn = "lrnie8158976912"
      val arc = "arc"

      val ie801 = XmlMessageGeneratorFactory.generate(consignorErn, MessageParams(IE801, "XI000001", consigneeErn = Some(consigneeErn), localReferenceNumber = Some(lrn), administrativeReferenceCode = Some(arc)))
      val ie818 = XmlMessageGeneratorFactory.generate(consignorErn, MessageParams(IE818, "XI000002", consigneeErn = Some(consigneeErn), localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
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
          Message(utils.encode(messages(1).toXml.toString()), "IE818", "XI000002", consignorErn, Set.empty, now),
        ),
        administrativeReferenceCode = Some(arc)
      )

      when(mockDateTimeService.timestamp()).thenReturn(now)

      when(mockCorrelationIdService.generateCorrelationId()).thenAnswer(UUID.randomUUID().toString)

      when(mockMessageConnector.getNewMessages(any)(any)).thenReturn(
        Future.successful(GetMessagesResponse(Seq(messages(1)), 1))
      )

      when(mockMessageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

      when(mockTraderMovementConnector.getMovementMessages(any, any)(any)).thenReturn(Future.successful(messages))

      movementService.saveNewMovement(initialMovement).futureValue.isRight mustBe true
      service.updateMessages(consignorErn, None)(hc).futureValue

      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorErn)).futureValue

      result.length mustBe 1
      result.head mustEqual expectedMovement
    }

    "must not create a new movement when an existing movement can be found in the database via consignor ERN and LRN" in {

      val hc = HeaderCarrier()
      val consignorErn = "testErn"
      val consigneeErn = "testErn2"
      val lrn = "lrnie8158976912"
      val arc = "arc"

      val ie801 = XmlMessageGeneratorFactory.generate(consignorErn, MessageParams(IE801, "XI000001", consigneeErn = Some(consigneeErn), localReferenceNumber = Some(lrn), administrativeReferenceCode = Some(arc)))
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

      when(mockMessageConnector.getNewMessages(any)(any)).thenReturn(
        Future.successful(GetMessagesResponse(Seq(messages.head), 1))
      )

      when(mockMessageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

      movementService.saveNewMovement(initialMovement).futureValue.isRight mustBe true
      service.updateMessages(consigneeErn, None)(hc).futureValue

      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorErn)).futureValue

      result.length mustBe 1
      result.head mustEqual expectedMovement
    }

    "fixProblemMovement" - {

      val utils = new EmcsUtils()
      val messageFactory = IEMessageFactory()

      def formatXml(ern: String, params: MessageParams): String = {
        utils.encode(messageFactory.createFromXml(
          params.messageType.value,
          XmlMessageGeneratorFactory.generate(ern, params)
        ).toXml.toString)
      }

      "must fix the given movement" in {

        val hc = HeaderCarrier()
        val consignor = "testErn"
        val consignee1 = "testErn2"
        val consignee2 = "testErn3"
        val consignee3 = "testErn4"

        val rootLrn = "2"
        val rootArc = "arc"
        val root801 = formatXml(consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee1), localReferenceNumber = Some(rootLrn), administrativeReferenceCode = Some(rootArc)))

        val secondLrn = "22"
        val secondArc = "arc2"
        val second801 = formatXml(consignor, MessageParams(IE801, "XI000002", consigneeErn = Some(consignee2), localReferenceNumber = Some(secondLrn), administrativeReferenceCode = Some(secondArc)))
        val second818 = formatXml(consignor, MessageParams(IE818, "XI000004", consigneeErn = Some(consignee2), localReferenceNumber = None, administrativeReferenceCode = Some(secondArc)))

        val thirdLrn = "23"
        val thirdArc = "arc3"
        val third801 = formatXml(consignor, MessageParams(IE801, "XI000003", consigneeErn = Some(consignee3), localReferenceNumber = Some(thirdLrn), administrativeReferenceCode = Some(thirdArc)))
        val third802 = formatXml(consignor, MessageParams(IE802, "XI000006", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))
        val third818 = formatXml(consignor, MessageParams(IE818, "XI000005", consigneeErn = Some(consignee3), localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))

        val rootMovement = Movement(
          boxId = None,
          localReferenceNumber = rootLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3), // note: consignee is wrong
          administrativeReferenceCode = Some(rootArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000005", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val thirdMovement = Movement(
          boxId = None,
          localReferenceNumber = thirdLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3),
          administrativeReferenceCode = Some(thirdArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee3, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedRootMovement = rootMovement.copy(
          consigneeId = Some(consignee1),
          messages = Seq(
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS))
          )
        )

        val expectedSecondMovement = Movement(
          boxId = None,
          localReferenceNumber = secondLrn,
          consignorId = consignor,
          consigneeId = Some(consignee2),
          administrativeReferenceCode = Some(secondArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000004", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedThirdMovement = thirdMovement.copy(
          consigneeId = Some(consignee3),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee3, Set.empty, now.minus(1, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
          )
        )

        when(mockDateTimeService.timestamp()).thenReturn(now)

        insert(rootMovement).futureValue
        insert(thirdMovement).futureValue

        service.archiveAndFixProblemMovement(rootMovement._id)(hc).futureValue

        count().futureValue mustEqual 3
        val results = findAll().futureValue

        val actualRootMovement = results.find(_.administrativeReferenceCode == expectedRootMovement.administrativeReferenceCode).value
        val actualSecondMovement = results.find(_.administrativeReferenceCode == expectedSecondMovement.administrativeReferenceCode).value
        val actualThirdMovement = results.find(_.administrativeReferenceCode == thirdMovement.administrativeReferenceCode).value

        actualRootMovement._id mustEqual expectedRootMovement._id
        actualRootMovement.localReferenceNumber mustEqual expectedRootMovement.localReferenceNumber
        actualRootMovement.administrativeReferenceCode mustEqual expectedRootMovement.administrativeReferenceCode
        actualRootMovement.consignorId mustEqual expectedRootMovement.consignorId
        actualRootMovement.consigneeId mustEqual expectedRootMovement.consigneeId
        actualRootMovement.lastUpdated mustEqual expectedRootMovement.lastUpdated
        actualRootMovement.messages mustEqual expectedRootMovement.messages

        actualSecondMovement.localReferenceNumber mustEqual expectedSecondMovement.localReferenceNumber
        actualSecondMovement.administrativeReferenceCode mustEqual expectedSecondMovement.administrativeReferenceCode
        actualSecondMovement.consignorId mustEqual expectedSecondMovement.consignorId
        actualSecondMovement.consigneeId mustEqual expectedSecondMovement.consigneeId
        actualSecondMovement.lastUpdated mustEqual expectedSecondMovement.lastUpdated
        actualSecondMovement.messages.head.encodedMessage mustEqual expectedSecondMovement.messages.head.encodedMessage
        actualSecondMovement.messages mustEqual expectedSecondMovement.messages

        actualThirdMovement._id mustEqual expectedThirdMovement._id
        actualThirdMovement.localReferenceNumber mustEqual expectedThirdMovement.localReferenceNumber
        actualThirdMovement.administrativeReferenceCode mustEqual expectedThirdMovement.administrativeReferenceCode
        actualThirdMovement.consignorId mustEqual expectedThirdMovement.consignorId
        actualThirdMovement.consigneeId mustEqual expectedThirdMovement.consigneeId
        actualThirdMovement.lastUpdated mustEqual expectedThirdMovement.lastUpdated
        actualThirdMovement.messages mustEqual expectedThirdMovement.messages
      }

      "must fix the given movement when none of its messages belong with it" in {

        val hc = HeaderCarrier()
        val consignor = "testErn"
        val consignee1 = "testErn2"
        val consignee2 = "testErn3"
        val consignee3 = "testErn4"

        val rootLrn = "2"
        val rootArc = "arc"
        val root801 = formatXml(consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee1), localReferenceNumber = Some(rootLrn), administrativeReferenceCode = Some(rootArc)))

        val secondLrn = "22"
        val secondArc = "arc2"
        val second801 = formatXml(consignor, MessageParams(IE801, "XI000002", consigneeErn = Some(consignee2), localReferenceNumber = Some(secondLrn), administrativeReferenceCode = Some(secondArc)))
        val second818 = formatXml(consignor, MessageParams(IE818, "XI000004", consigneeErn = Some(consignee2), localReferenceNumber = None, administrativeReferenceCode = Some(secondArc)))

        val thirdLrn = "23"
        val thirdArc = "arc3"
        val third801 = formatXml(consignor, MessageParams(IE801, "XI000003", consigneeErn = Some(consignee3), localReferenceNumber = Some(thirdLrn), administrativeReferenceCode = Some(thirdArc)))
        val third802 = formatXml(consignor, MessageParams(IE802, "XI000006", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))
        val third818 = formatXml(consignor, MessageParams(IE818, "XI000005", consigneeErn = Some(consignee3), localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))

        val rootMovement = Movement(
          boxId = None,
          localReferenceNumber = rootLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3), // note: consignee is wrong
          administrativeReferenceCode = Some(rootArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000005", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val thirdMovement = Movement(
          boxId = None,
          localReferenceNumber = thirdLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3),
          administrativeReferenceCode = Some(thirdArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee3, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedRootMovement = rootMovement.copy(
          administrativeReferenceCode = None,
          consigneeId = None,
          messages = Seq.empty
        )

        val expectedSecondMovement = Movement(
          boxId = None,
          localReferenceNumber = secondLrn,
          consignorId = consignor,
          consigneeId = Some(consignee2),
          administrativeReferenceCode = Some(secondArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000004", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedThirdMovement = thirdMovement.copy(
          consigneeId = Some(consignee3),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee3, Set.empty, now.minus(1, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
          )
        )

        when(mockDateTimeService.timestamp()).thenReturn(now)

        insert(rootMovement).futureValue
        insert(thirdMovement).futureValue

        service.archiveAndFixProblemMovement(rootMovement._id)(hc).futureValue

        count().futureValue mustEqual 3
        val results = findAll().futureValue

        val actualRootMovement = results.find(_.localReferenceNumber == expectedRootMovement.localReferenceNumber).value
        val actualSecondMovement = results.find(_.administrativeReferenceCode == expectedSecondMovement.administrativeReferenceCode).value
        val actualThirdMovement = results.find(_.administrativeReferenceCode == thirdMovement.administrativeReferenceCode).value

        actualRootMovement._id mustEqual expectedRootMovement._id
        actualRootMovement.localReferenceNumber mustEqual expectedRootMovement.localReferenceNumber
        actualRootMovement.administrativeReferenceCode mustEqual expectedRootMovement.administrativeReferenceCode
        actualRootMovement.consignorId mustEqual expectedRootMovement.consignorId
        actualRootMovement.consigneeId mustEqual expectedRootMovement.consigneeId
        actualRootMovement.lastUpdated mustEqual expectedRootMovement.lastUpdated
        actualRootMovement.messages mustEqual expectedRootMovement.messages

        actualSecondMovement.localReferenceNumber mustEqual expectedSecondMovement.localReferenceNumber
        actualSecondMovement.administrativeReferenceCode mustEqual expectedSecondMovement.administrativeReferenceCode
        actualSecondMovement.consignorId mustEqual expectedSecondMovement.consignorId
        actualSecondMovement.consigneeId mustEqual expectedSecondMovement.consigneeId
        actualSecondMovement.lastUpdated mustEqual expectedSecondMovement.lastUpdated
        actualSecondMovement.messages.head.encodedMessage mustEqual expectedSecondMovement.messages.head.encodedMessage
        actualSecondMovement.messages mustEqual expectedSecondMovement.messages

        actualThirdMovement._id mustEqual expectedThirdMovement._id
        actualThirdMovement.localReferenceNumber mustEqual expectedThirdMovement.localReferenceNumber
        actualThirdMovement.administrativeReferenceCode mustEqual expectedThirdMovement.administrativeReferenceCode
        actualThirdMovement.consignorId mustEqual expectedThirdMovement.consignorId
        actualThirdMovement.consigneeId mustEqual expectedThirdMovement.consigneeId
        actualThirdMovement.lastUpdated mustEqual expectedThirdMovement.lastUpdated
        actualThirdMovement.messages mustEqual expectedThirdMovement.messages
      }

      "must correctly restore movements when the messages are in non-chronological order" in {

        val hc = HeaderCarrier()
        val consignor = "testErn"
        val consignee1 = "testErn2"
        val consignee2 = "testErn3"
        val consignee3 = "testErn4"

        val rootLrn = "2"
        val rootArc = "arc"
        val root801 = formatXml(consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee1), localReferenceNumber = Some(rootLrn), administrativeReferenceCode = Some(rootArc)))

        val secondLrn = "22"
        val secondArc = "arc2"
        val second801 = formatXml(consignor, MessageParams(IE801, "XI000002", consigneeErn = Some(consignee2), localReferenceNumber = Some(secondLrn), administrativeReferenceCode = Some(secondArc)))
        val second818 = formatXml(consignor, MessageParams(IE818, "XI000004", consigneeErn = Some(consignee2), localReferenceNumber = None, administrativeReferenceCode = Some(secondArc)))

        val thirdLrn = "23"
        val thirdArc = "arc3"
        val third801 = formatXml(consignor, MessageParams(IE801, "XI000003", consigneeErn = Some(consignee3), localReferenceNumber = Some(thirdLrn), administrativeReferenceCode = Some(thirdArc)))
        val third802 = formatXml(consignor, MessageParams(IE802, "XI000006", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))
        val third818 = formatXml(consignor, MessageParams(IE818, "XI000005", consigneeErn = Some(consignee3), localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))

        val rootMovement = Movement(
          boxId = None,
          localReferenceNumber = rootLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3), // note: consignee is wrong
          administrativeReferenceCode = Some(rootArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000005", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          ).reverse
        )

        val thirdMovement = Movement(
          boxId = None,
          localReferenceNumber = thirdLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3),
          administrativeReferenceCode = Some(thirdArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee3, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedRootMovement = rootMovement.copy(
          consigneeId = Some(consignee1),
          messages = Seq(
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS))
          )
        )

        val expectedSecondMovement = Movement(
          boxId = None,
          localReferenceNumber = secondLrn,
          consignorId = consignor,
          consigneeId = Some(consignee2),
          administrativeReferenceCode = Some(secondArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000004", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedThirdMovement = thirdMovement.copy(
          consigneeId = Some(consignee3),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee3, Set.empty, now.minus(1, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
          )
        )

        when(mockDateTimeService.timestamp()).thenReturn(now)

        insert(rootMovement).futureValue
        insert(thirdMovement).futureValue

        service.archiveAndFixProblemMovement(rootMovement._id)(hc).futureValue

        count().futureValue mustEqual 3
        val results = findAll().futureValue

        val actualRootMovement = results.find(_.administrativeReferenceCode == expectedRootMovement.administrativeReferenceCode).value
        val actualSecondMovement = results.find(_.administrativeReferenceCode == expectedSecondMovement.administrativeReferenceCode).value
        val actualThirdMovement = results.find(_.administrativeReferenceCode == thirdMovement.administrativeReferenceCode).value

        actualRootMovement._id mustEqual expectedRootMovement._id
        actualRootMovement.localReferenceNumber mustEqual expectedRootMovement.localReferenceNumber
        actualRootMovement.administrativeReferenceCode mustEqual expectedRootMovement.administrativeReferenceCode
        actualRootMovement.consignorId mustEqual expectedRootMovement.consignorId
        actualRootMovement.consigneeId mustEqual expectedRootMovement.consigneeId
        actualRootMovement.lastUpdated mustEqual expectedRootMovement.lastUpdated
        actualRootMovement.messages mustEqual expectedRootMovement.messages

        actualSecondMovement.localReferenceNumber mustEqual expectedSecondMovement.localReferenceNumber
        actualSecondMovement.administrativeReferenceCode mustEqual expectedSecondMovement.administrativeReferenceCode
        actualSecondMovement.consignorId mustEqual expectedSecondMovement.consignorId
        actualSecondMovement.consigneeId mustEqual expectedSecondMovement.consigneeId
        actualSecondMovement.lastUpdated mustEqual expectedSecondMovement.lastUpdated
        actualSecondMovement.messages.head.encodedMessage mustEqual expectedSecondMovement.messages.head.encodedMessage
        actualSecondMovement.messages mustEqual expectedSecondMovement.messages

        actualThirdMovement._id mustEqual expectedThirdMovement._id
        actualThirdMovement.localReferenceNumber mustEqual expectedThirdMovement.localReferenceNumber
        actualThirdMovement.administrativeReferenceCode mustEqual expectedThirdMovement.administrativeReferenceCode
        actualThirdMovement.consignorId mustEqual expectedThirdMovement.consignorId
        actualThirdMovement.consigneeId mustEqual expectedThirdMovement.consigneeId
        actualThirdMovement.lastUpdated mustEqual expectedThirdMovement.lastUpdated
        actualThirdMovement.messages mustEqual expectedThirdMovement.messages
      }

      "must not amend a movement which legitimately has more than 2 801s attached to it" in {

        val hc = HeaderCarrier()
        val consignor = "testErn"
        val consignee = "testErn2"
        val consignee2 = "testErn3"

        val rootLrn = "2"
        val rootArc = "arc"
        val first801 = formatXml(consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee), localReferenceNumber = Some(rootLrn), administrativeReferenceCode = Some(rootArc)))
        val ie813 = formatXml(consignor, MessageParams(IE813, "XI000002", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(rootArc)))
        val second801 = formatXml(consignor, MessageParams(IE801, "XI000003", consigneeErn = Some(consignee2), localReferenceNumber = Some(rootLrn), administrativeReferenceCode = Some(rootArc)))

        val movement = Movement(
          boxId = None,
          localReferenceNumber = rootLrn,
          consignorId = consignor,
          consigneeId = Some(consignee2),
          administrativeReferenceCode = Some(rootArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(first801, "IE801", "XI000001", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(first801, "IE801", "XI000001", consignee, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(ie813, "IE813", "XI000002", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(ie813, "IE813", "XI000002", consignee, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000003", consignor, Set.empty, now.minus(1, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000003", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        insert(movement).futureValue

        service.archiveAndFixProblemMovement(movement._id)(hc).futureValue

        count().futureValue mustEqual 1

        val result = findAll().futureValue.head
        result mustEqual movement
      }

      "must fix a movement when the arc is wrong" in {

        val hc = HeaderCarrier()
        val consignor = "testErn"
        val consignee1 = "testErn2"
        val consignee2 = "testErn3"

        val rootLrn = "2"
        val rootArc = "arc"
        val root801 = formatXml(consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee1), localReferenceNumber = Some(rootLrn), administrativeReferenceCode = Some(rootArc)))

        val secondLrn = "22"
        val secondArc = "arc2"
        val second801 = formatXml(consignor, MessageParams(IE801, "XI000002", consigneeErn = Some(consignee2), localReferenceNumber = Some(secondLrn), administrativeReferenceCode = Some(secondArc)))
        val second818 = formatXml(consignor, MessageParams(IE818, "XI000005", consigneeErn = Some(consignee2), localReferenceNumber = None, administrativeReferenceCode = Some(secondArc)))

        val rootMovement = Movement(
          boxId = None,
          localReferenceNumber = rootLrn,
          consignorId = consignor,
          consigneeId = Some(consignee1),
          administrativeReferenceCode = Some(secondArc), // note: arc is wrong
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000005", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedRootMovement = rootMovement.copy(
          boxId = None,
          localReferenceNumber = rootLrn,
          consignorId = consignor,
          consigneeId = Some(consignee1),
          administrativeReferenceCode = Some(rootArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          )
        )

        val expectedSecondMovement = Movement(
          boxId = None,
          localReferenceNumber = secondLrn,
          consignorId = consignor,
          consigneeId = Some(consignee2),
          administrativeReferenceCode = Some(secondArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000005", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        when(mockDateTimeService.timestamp()).thenReturn(now)

        insert(rootMovement).futureValue

        service.archiveAndFixProblemMovement(rootMovement._id)(hc).futureValue

        count().futureValue mustEqual 2
        val results = findAll().futureValue

        val actualRootMovement = results.find(_.administrativeReferenceCode == expectedRootMovement.administrativeReferenceCode).value
        val actualSecondMovement = results.find(_.administrativeReferenceCode == expectedSecondMovement.administrativeReferenceCode).value

        actualRootMovement._id mustEqual expectedRootMovement._id
        actualRootMovement.localReferenceNumber mustEqual expectedRootMovement.localReferenceNumber
        actualRootMovement.administrativeReferenceCode mustEqual expectedRootMovement.administrativeReferenceCode
        actualRootMovement.consignorId mustEqual expectedRootMovement.consignorId
        actualRootMovement.consigneeId mustEqual expectedRootMovement.consigneeId
        actualRootMovement.lastUpdated mustEqual expectedRootMovement.lastUpdated
        actualRootMovement.messages mustEqual expectedRootMovement.messages

        actualSecondMovement.localReferenceNumber mustEqual expectedSecondMovement.localReferenceNumber
        actualSecondMovement.administrativeReferenceCode mustEqual expectedSecondMovement.administrativeReferenceCode
        actualSecondMovement.consignorId mustEqual expectedSecondMovement.consignorId
        actualSecondMovement.consigneeId mustEqual expectedSecondMovement.consigneeId
        actualSecondMovement.lastUpdated mustEqual expectedSecondMovement.lastUpdated
        actualSecondMovement.messages.head.encodedMessage mustEqual expectedSecondMovement.messages.head.encodedMessage
        actualSecondMovement.messages mustEqual expectedSecondMovement.messages
      }

      "must correctly set consignee when there is an 813" in {

        val hc = HeaderCarrier()
        val consignor = "testErn"
        val consignee1 = "testErn2"
        val consignee2 = "testErn3"
        val consignee3 = "testErn4"
        val consignee4 = "testErn5"
        val consignee5 = "testErn6"
        val consignee6 = "testErn7"

        val rootLrn = "2"
        val rootArc = "arc"
        val root801 = formatXml(consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee1), localReferenceNumber = Some(rootLrn), administrativeReferenceCode = Some(rootArc)))
        val root813 = formatXml(consignor, MessageParams(IE813, "XI000007", consigneeErn = Some(consignee4), localReferenceNumber = None, administrativeReferenceCode = Some(rootArc)))

        val secondLrn = "22"
        val secondArc = "arc2"
        val second801 = formatXml(consignor, MessageParams(IE801, "XI000002", consigneeErn = Some(consignee2), localReferenceNumber = Some(secondLrn), administrativeReferenceCode = Some(secondArc)))
        val second818 = formatXml(consignor, MessageParams(IE818, "XI000004", consigneeErn = Some(consignee2), localReferenceNumber = None, administrativeReferenceCode = Some(secondArc)))
        val second813 = formatXml(consignor, MessageParams(IE813, "XI000008", consigneeErn = Some(consignee5), localReferenceNumber = None, administrativeReferenceCode = Some(secondArc)))

        val thirdLrn = "23"
        val thirdArc = "arc3"
        val third801 = formatXml(consignor, MessageParams(IE801, "XI000003", consigneeErn = Some(consignee3), localReferenceNumber = Some(thirdLrn), administrativeReferenceCode = Some(thirdArc)))
        val third802 = formatXml(consignor, MessageParams(IE802, "XI000006", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))
        val third818 = formatXml(consignor, MessageParams(IE818, "XI000005", consigneeErn = Some(consignee3), localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))
        val third813 = formatXml(consignor, MessageParams(IE813, "XI000009", consigneeErn = Some(consignee6), localReferenceNumber = None, administrativeReferenceCode = Some(thirdArc)))

        val rootMovement = Movement(
          boxId = None,
          localReferenceNumber = rootLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3), // note: consignee is wrong
          administrativeReferenceCode = Some(rootArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root813, "IE813", "XI000007", consignee4, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third813, "IE813", "XI000009", consignee6, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(second813, "IE813", "XI000008", consignee5, Set.empty, now.minus(1, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000005", consignee5, Set.empty, now.minus(1, ChronoUnit.DAYS)),
          )
        )

        val thirdMovement = Movement(
          boxId = None,
          localReferenceNumber = thirdLrn,
          consignorId = consignor,
          consigneeId = Some(consignee3),
          administrativeReferenceCode = Some(thirdArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee6, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedRootMovement = rootMovement.copy(
          consigneeId = Some(consignee4),
          messages = Seq(
            Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS)),
            Message(root813, "IE813", "XI000007", consignee4, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          )
        )

        val expectedSecondMovement = Movement(
          boxId = None,
          localReferenceNumber = secondLrn,
          consignorId = consignor,
          consigneeId = Some(consignee5),
          administrativeReferenceCode = Some(secondArc),
          lastUpdated = now.minus(1, ChronoUnit.DAYS),
          messages = Seq(
            Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
            Message(second813, "IE813", "XI000008", consignee5, Set.empty, now.minus(1, ChronoUnit.DAYS)),
            Message(second818, "IE818", "XI000004", consignee5, Set.empty, now.minus(1, ChronoUnit.DAYS))
          )
        )

        val expectedThirdMovement = thirdMovement.copy(
          consigneeId = Some(consignee6),
          messages = Seq(
            Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third818, "IE818", "XI000005", consignee6, Set.empty, now.minus(1, ChronoUnit.DAYS)),
            Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
            Message(third813, "IE813", "XI000009", consignee6, Set.empty, now.minus(2, ChronoUnit.DAYS)),
          )
        )

        when(mockDateTimeService.timestamp()).thenReturn(now)

        insert(rootMovement).futureValue
        insert(thirdMovement).futureValue

        service.archiveAndFixProblemMovement(rootMovement._id)(hc).futureValue

        count().futureValue mustEqual 3
        val results = findAll().futureValue

        val actualRootMovement = results.find(_.administrativeReferenceCode == expectedRootMovement.administrativeReferenceCode).value
        val actualSecondMovement = results.find(_.administrativeReferenceCode == expectedSecondMovement.administrativeReferenceCode).value
        val actualThirdMovement = results.find(_.administrativeReferenceCode == thirdMovement.administrativeReferenceCode).value

        actualRootMovement._id mustEqual expectedRootMovement._id
        actualRootMovement.localReferenceNumber mustEqual expectedRootMovement.localReferenceNumber
        actualRootMovement.administrativeReferenceCode mustEqual expectedRootMovement.administrativeReferenceCode
        actualRootMovement.consignorId mustEqual expectedRootMovement.consignorId
        actualRootMovement.consigneeId mustEqual expectedRootMovement.consigneeId
        actualRootMovement.lastUpdated mustEqual expectedRootMovement.lastUpdated
        actualRootMovement.messages mustEqual expectedRootMovement.messages

        actualSecondMovement.localReferenceNumber mustEqual expectedSecondMovement.localReferenceNumber
        actualSecondMovement.administrativeReferenceCode mustEqual expectedSecondMovement.administrativeReferenceCode
        actualSecondMovement.consignorId mustEqual expectedSecondMovement.consignorId
        actualSecondMovement.consigneeId mustEqual expectedSecondMovement.consigneeId
        actualSecondMovement.lastUpdated mustEqual expectedSecondMovement.lastUpdated
        actualSecondMovement.messages.head.encodedMessage mustEqual expectedSecondMovement.messages.head.encodedMessage
        actualSecondMovement.messages mustEqual expectedSecondMovement.messages

        actualThirdMovement._id mustEqual expectedThirdMovement._id
        actualThirdMovement.localReferenceNumber mustEqual expectedThirdMovement.localReferenceNumber
        actualThirdMovement.administrativeReferenceCode mustEqual expectedThirdMovement.administrativeReferenceCode
        actualThirdMovement.consignorId mustEqual expectedThirdMovement.consignorId
        actualThirdMovement.consigneeId mustEqual expectedThirdMovement.consigneeId
        actualThirdMovement.lastUpdated mustEqual expectedThirdMovement.lastUpdated
        actualThirdMovement.messages mustEqual expectedThirdMovement.messages
      }
    }
  }
}
