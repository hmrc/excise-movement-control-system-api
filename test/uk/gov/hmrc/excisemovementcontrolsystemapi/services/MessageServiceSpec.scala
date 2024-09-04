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

import cats.data.EitherT
import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{never, reset, times, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageConnector, TraderMovementConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageServiceSpec
    extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val movementRepository      = mock[MovementRepository]
  private val ernRetrievalRepository  = mock[ErnRetrievalRepository]
  private val boxIdRepository         = mock[BoxIdRepository]
  private val messageConnector        = mock[MessageConnector]
  private val traderMovementConnector = mock[TraderMovementConnector]
  private val dateTimeService         = mock[DateTimeService]
  private val correlationIdService    = mock[CorrelationIdService]
  private val auditService            = mock[AuditService]
  private val mongoLockRepository     = mock[MongoLockRepository]

  private lazy val messageService = app.injector.instanceOf[MessageService]

  private val utils                      = new EmcsUtils
  private val now                        = Instant.now
  private val migrateCutoffTimestamp     = now.minus(1, ChronoUnit.SECONDS)
  private val lastRetrievedTimestamp     = now.plus(1, ChronoUnit.SECONDS)
  private val updateOrCreateTimestamp    = lastRetrievedTimestamp.plus(1, ChronoUnit.SECONDS)
  private val newId                      = UUID.randomUUID().toString
  private val lock                       = Lock("testErn", "owner", now, now.plus(5, ChronoUnit.MINUTES))
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[ErnRetrievalRepository].toInstance(ernRetrievalRepository),
      bind[BoxIdRepository].toInstance(boxIdRepository),
      bind[MessageConnector].toInstance(messageConnector),
      bind[TraderMovementConnector].toInstance(traderMovementConnector),
      bind[DateTimeService].toInstance(dateTimeService),
      bind[CorrelationIdService].toInstance(correlationIdService),
      bind[AuditService].toInstance(auditService),
      bind[MongoLockRepository].toInstance(mongoLockRepository)
    )
    .configure(
      "microservice.services.eis.throttle-cutoff" -> "5 minutes",
      "migrateLastUpdatedCutoff"                  -> migrateCutoffTimestamp.toString
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      movementRepository,
      ernRetrievalRepository,
      boxIdRepository,
      messageConnector,
      traderMovementConnector,
      dateTimeService,
      correlationIdService,
      auditService,
      mongoLockRepository
    )
    when(dateTimeService.timestamp()).thenReturn(lastRetrievedTimestamp, updateOrCreateTimestamp)
  }

  "updateMessages" when {

    "last retrieved is empty" when {
      "there is a movement but we have never retrieved anything" when {
        "we try to retrieve messages but there are none" should {
          "update last retrieval time for ern" in {
            val ern      = "testErn"
            val movement = Movement(None, "LRN", "Consignor", None)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

            messageService.updateMessages(ern, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository, never).getAllBy(any)
            verify(movementRepository, never).save(any)
            verify(messageConnector, never).acknowledgeMessages(any)(any)
            verify(ernRetrievalRepository).setLastRetrieved(ern, lastRetrievedTimestamp)
          }
        }

        "we try to retrieve messages and there are some" should {

          "add messages to only the movement when the message has an LRN" in {
            val ern                = "testErn"
            val lrnMovement        = Movement(None, "lrnie8158976912", ern, None)
            val notLrnMovement     = Movement(None, "notTheLrn", ern, None)
            val ie704              = XmlMessageGeneratorFactory.generate(
              ern,
              MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912"))
            )
            val messages           = Seq(IE704Message.createFromXml(ie704))
            val expectedMessages   =
              Seq(
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE704",
                  "XI000001",
                  ern,
                  Set("boxId1", "boxId2"),
                  updateOrCreateTimestamp
                )
              )
            val expectedMovement   = lrnMovement.copy(messages = expectedMessages, lastUpdated = updateOrCreateTimestamp)
            val unexpectedMovement = notLrnMovement.copy(messages = expectedMessages)

            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(lrnMovement, notLrnMovement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set("boxId1", "boxId2")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(ern)
            verify(movementRepository, never).save(unexpectedMovement)
            verify(movementRepository).save(expectedMovement)
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }

          "add messages to only the movement when the message has no LRN" in {
            val consignorErn   = "testErn"
            val consigneeErn   = "consigneeErn"
            val arc            = "23XI00000000000000012"
            val ie818          = XmlMessageGeneratorFactory.generate(
              consignorErn,
              MessageParams(
                IE818,
                "GB00002",
                Some(consigneeErn),
                Some(arc),
                None
              )
            )
            val ie801          = XmlMessageGeneratorFactory.generate(
              consignorErn,
              MessageParams(
                IE801,
                "GB00001",
                Some(consigneeErn),
                Some(arc),
                Some("lrn1")
              )
            )
            val messages       = Seq(IE801Message.createFromXml(ie801), IE818Message.createFromXml(ie818))
            val arcMovement    = Movement(
              None,
              "lrn1",
              consignorErn,
              Some(consigneeErn),
              administrativeReferenceCode = None,
              messages = Seq.empty
            )
            val notArcMovement = Movement(None, "lrn2", consignorErn, None)

            val expectedMessages   =
              Seq(
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignorErn,
                  Set.empty,
                  updateOrCreateTimestamp
                ),
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consigneeErn,
                  Set.empty,
                  updateOrCreateTimestamp
                ),
                Message(
                  utils.encode(messages(1).toXml.toString()),
                  "IE818",
                  "GB00002",
                  consignorErn,
                  Set("boxId1"),
                  updateOrCreateTimestamp
                )
              )
            val expectedMovement   = arcMovement.copy(
              messages = expectedMessages,
              lastUpdated = updateOrCreateTimestamp,
              administrativeReferenceCode = Some(arc)
            )
            val unexpectedMovement = notArcMovement.copy(messages = expectedMessages)

            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(arcMovement, notArcMovement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(consignorErn)).thenReturn(Future.successful(Set("boxId1")))
            when(boxIdRepository.getBoxIds(consigneeErn)).thenReturn(Future.successful(Set("boxId2")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(Seq(messages(1)), 1)))
            when(traderMovementConnector.getMovementMessages(any, any)(any))
              .thenReturn(Future.successful(Seq(messages.head)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(consignorErn, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(consignorErn))(any)
            verify(movementRepository).getAllBy(consignorErn)
            verify(movementRepository).getByArc(arc)
            verify(movementRepository, never).save(unexpectedMovement)
            verify(movementRepository).save(expectedMovement)
            verify(messageConnector).acknowledgeMessages(eqTo(consignorErn))(any)
          }
        }
      }

      "there is a movement that already has messages" when {
        "we try to retrieve new messages and there are some" should {
          "add the new messages to the movement" in {
            val ern              = "testErn"
            val ie801            = XmlMessageGeneratorFactory.generate(
              ern,
              MessageParams(
                IE801,
                "GB00001",
                Some("testConsignee"),
                Some("23XI00000000000000012"),
                Some("lrnie8158976912")
              )
            )
            val ie704            = XmlMessageGeneratorFactory.generate(
              ern,
              MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912"))
            )
            val movement         = Movement(
              None,
              "lrnie8158976912",
              ern,
              Some("testConsignee"),
              Some("23XI00000000000000012"),
              messages = Seq(Message(utils.encode(ie801.toString()), "IE801", "GB00001", ern, Set.empty, now))
            )
            val messages         = Seq(IE704Message.createFromXml(ie704))
            val expectedMessages = movement.messages ++ Seq(
              Message(
                utils.encode(messages.head.toXml.toString()),
                "IE704",
                "XI000001",
                ern,
                Set.empty,
                updateOrCreateTimestamp
              )
            )
            val expectedMovement = movement.copy(messages = expectedMessages, lastUpdated = updateOrCreateTimestamp)

            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
        }
      }
      "there are multiple movements for one message (an 829)" should {
        "update all relevant movements with the message" in {
          // 829 doesn't have consignor in it - can't make a movement from this
          // movements created here won't get push notifications
          val movement1Timestamp = now.plus(1, ChronoUnit.SECONDS)
          val movement2Timestamp = movement1Timestamp.plus(1, ChronoUnit.SECONDS)
          val ern                = "testErn"
          val ie829              = XmlMessageGeneratorFactory.generate(
            ern,
            MessageParams(IE829, "XI000001", consigneeErn = Some("testConsignee"))
          )
          val arc1               = "23XI00000000000056339"
          val arc2               = "23XI00000000000056340"
          val movement1          = Movement(None, "???", "???", None, Some(arc1), movement1Timestamp, Seq.empty)
          val movement2          = Movement(None, "???", "???", None, Some(arc2), movement2Timestamp, Seq.empty)
          val message            = IE829Message.createFromXml(ie829)

          val expectedMessage1  =
            Seq(
              Message(utils.encode(message.toXml.toString()), "IE829", "XI000001", ern, Set.empty, movement1Timestamp)
            )
          val expectedMessage2  =
            Seq(
              Message(utils.encode(message.toXml.toString()), "IE829", "XI000001", ern, Set.empty, movement2Timestamp)
            )
          val expectedMovement1 = movement1.copy(messages = expectedMessage1)
          val expectedMovement2 = movement2.copy(messages = expectedMessage2)

          when(dateTimeService.timestamp()).thenReturn(lastRetrievedTimestamp, movement1Timestamp, movement2Timestamp)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement1, movement2)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(Seq(message), 1)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern, None).futureValue

          val movementCaptor = ArgCaptor[Movement]

          verify(dateTimeService, times(3)).timestamp()
          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository, times(2)).save(movementCaptor)
          verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)

          movementCaptor.values.head mustBe expectedMovement1
          movementCaptor.values(1) mustBe expectedMovement2
        }
      }
      "there is no movement" when {
        "we try to retrieve new messages for an ERN and there are some" should {
          "a new movement should be created from an IE704 message" in {
            val ern              = "testErn"
            val ie704            = XmlMessageGeneratorFactory.generate(
              ern,
              MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912"))
            )
            val messages         = Seq(IE704Message.createFromXml(ie704))
            val expectedMovement = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              None,
              None,
              updateOrCreateTimestamp,
              messages = Seq(
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE704",
                  "XI000001",
                  ern,
                  Set("boxId"),
                  updateOrCreateTimestamp
                )
              )
            )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set("boxId")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
          "a new movement should be created from an IE801 message for the consignor" in {
            val consignor        = "testErn"
            val consignee        = "testConsignee"
            val ie801            = XmlMessageGeneratorFactory.generate(
              consignor,
              MessageParams(
                IE801,
                "GB00001",
                Some("testConsignee"),
                Some("23XI00000000000000012"),
                Some("lrnie8158976912")
              )
            )
            val messages         = Seq(IE801Message.createFromXml(ie801))
            val expectedMovement = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              Some("testConsignee"),
              Some("23XI00000000000000012"),
              updateOrCreateTimestamp,
              messages = Seq(
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignor,
                  Set("boxId1"),
                  updateOrCreateTimestamp
                ),
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignee,
                  Set("boxId2"),
                  updateOrCreateTimestamp
                )
              )
            )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
            when(movementRepository.getMovementByLRNAndERNIn(any, any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
            when(boxIdRepository.getBoxIds(consignee)).thenReturn(Future.successful(Set("boxId2")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(consignor, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(consignor))(any)
            verify(movementRepository).getAllBy(eqTo(consignor))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(messageConnector).acknowledgeMessages(eqTo(consignor))(any)
          }
          "a new movement should be created from an IE801 message for the consignee" in {
            val consignor        = "testErn"
            val consignee        = "testConsignee"
            val ie801            = XmlMessageGeneratorFactory.generate(
              consignor,
              MessageParams(
                IE801,
                "GB00001",
                Some(consignee),
                Some("23XI00000000000000012"),
                Some("lrnie8158976912")
              )
            )
            val messages         = Seq(IE801Message.createFromXml(ie801))
            val expectedMovement = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              Some(consignee),
              Some("23XI00000000000000012"),
              updateOrCreateTimestamp,
              messages = Seq(
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignor,
                  Set("boxId1"),
                  updateOrCreateTimestamp
                ),
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignee,
                  Set("boxId2"),
                  updateOrCreateTimestamp
                )
              )
            )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
            when(movementRepository.getMovementByLRNAndERNIn(any, any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
            when(boxIdRepository.getBoxIds(consignee)).thenReturn(Future.successful(Set("boxId2")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(consignee, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(consignee))(any)
            verify(movementRepository).getAllBy(eqTo(consignee))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(messageConnector).acknowledgeMessages(eqTo(consignee))(any)
          }

          "a new movement is created from trader-movement call IE801 message if message is not an IE801 or IE704" in {
            val consignor              = "testErn"
            val consignee              = "testConsignee"
            val ie801Xml               = XmlMessageGeneratorFactory.generate(
              consignor,
              MessageParams(
                IE801,
                "GB00001",
                Some(consignee),
                Some("23XI00000000000000012"),
                Some("lrnie8158976912")
              )
            )
            val ie801Message           = IE801Message.createFromXml(ie801Xml)
            val ie818Xml               = XmlMessageGeneratorFactory.generate(
              consignor,
              MessageParams(IE818, "GB00002", Some(consignee), Some("23XI00000000000000012"))
            )
            val ie818Message           = IE818Message.createFromXml(ie818Xml)
            val messages               = Seq(ie818Message)
            val traderMovementMessages = Seq(ie801Message, ie818Message)
            val expectedMovement       = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              Some(consignee),
              Some("23XI00000000000000012"),
              updateOrCreateTimestamp,
              messages = Seq(
                Message(
                  utils.encode(ie801Message.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignor,
                  Set.empty,
                  updateOrCreateTimestamp
                ),
                Message(
                  utils.encode(ie801Message.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignee,
                  Set.empty,
                  updateOrCreateTimestamp
                ),
                Message(
                  utils.encode(ie818Message.toXml.toString()),
                  "IE818",
                  "GB00002",
                  consignor,
                  Set("boxId1"),
                  updateOrCreateTimestamp
                )
              )
            )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(auditService.auditMessage(any, any)(any)).thenReturn(EitherT.pure(()))
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
            when(movementRepository.getMovementByLRNAndERNIn(any, any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
            when(boxIdRepository.getBoxIds(consignee)).thenReturn(Future.successful(Set("boxId2")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(traderMovementConnector.getMovementMessages(any, any)(any))
              .thenReturn(Future.successful(traderMovementMessages))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(consignor, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(consignor))(any)
            verify(traderMovementConnector).getMovementMessages(eqTo(consignor), eqTo("23XI00000000000000012"))(any)
            verify(movementRepository).getAllBy(eqTo(consignor))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(messageConnector).acknowledgeMessages(eqTo(consignor))(any)
          }
          "a message is audited with failure if its not an IE801 or IE704, and the trader movement messages don't include an IE801" in {
            val ern      = "testErn"
            val ie818    = XmlMessageGeneratorFactory.generate(
              ern,
              MessageParams(IE818, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"))
            )
            val messages = Seq(IE818Message.createFromXml(ie818))

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(auditService.auditMessage(any, any)(any)).thenReturn(EitherT.pure(()))
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(traderMovementConnector.getMovementMessages(any, any)(any))
              .thenReturn(Future.successful(messages))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(traderMovementConnector).getMovementMessages(eqTo(ern), eqTo("23XI00000000000000012"))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(auditService).auditMessage(
              messages.head,
              s"An IE818 message has been retrieved with no movement, unable to create movement"
            )
          }
          "an IE704 message is audited with failure if it does not have an LRN, and the trader movement messages don't include an IE801" in {
            val ern      = "testErn"
            val ie704    = XmlMessageGeneratorFactory.generate(
              ern,
              MessageParams(IE704, "XI000001", administrativeReferenceCode = Some("23XI00000000000000012"))
            )
            val messages = Seq(IE704Message.createFromXml(ie704))

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(auditService.auditMessage(any, any)(any)).thenReturn(EitherT.pure(()))
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(traderMovementConnector.getMovementMessages(any, any)(any))
              .thenReturn(Future.successful(messages))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern, None).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(traderMovementConnector).getMovementMessages(eqTo(ern), eqTo("23XI00000000000000012"))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(auditService).auditMessage(
              messages.head,
              s"An IE704 message has been retrieved with no movement, unable to create movement"
            )
          }

          "a single new movement should be created when there are multiple messages for the same ern" in {
            val ie801Timestamp   = now.plus(1, ChronoUnit.SECONDS)
            val ie802Timestamp   = ie801Timestamp.plus(1, ChronoUnit.SECONDS)
            val consignor        = "testErn"
            val consignee        = "testConsignee"
            val ie801            = XmlMessageGeneratorFactory.generate(
              consignor,
              MessageParams(
                IE801,
                "GB00001",
                Some(consignee),
                Some("23XI00000000000000012"),
                Some("lrnie8158976912")
              )
            )
            val ie802            = XmlMessageGeneratorFactory.generate(
              consignor,
              MessageParams(IE802, "GB0002", administrativeReferenceCode = Some("23XI00000000000000012"))
            )
            val messages         = Seq(IE801Message.createFromXml(ie801), IE802Message.createFromXml(ie802))
            val expectedMovement = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              Some(consignee),
              Some("23XI00000000000000012"),
              ie802Timestamp,
              messages = Seq(
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignor,
                  Set("boxId1"),
                  ie801Timestamp
                ),
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignee,
                  Set("boxId2"),
                  ie801Timestamp
                ),
                Message(
                  utils.encode(messages(1).toXml.toString()),
                  "IE802",
                  "GB0002",
                  consignor,
                  Set("boxId1"),
                  ie802Timestamp
                )
              )
            )

            when(dateTimeService.timestamp()).thenReturn(lastRetrievedTimestamp, ie801Timestamp, ie802Timestamp)
            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
            when(movementRepository.getMovementByLRNAndERNIn(any, any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
            when(boxIdRepository.getBoxIds(consignee)).thenReturn(Future.successful(Set("boxId2")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 0)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(consignor, None).futureValue

            verify(dateTimeService, times(3)).timestamp()
            verify(messageConnector).getNewMessages(eqTo(consignor))(any)
            verify(movementRepository).getAllBy(eqTo(consignor))
            verify(movementRepository).save(eqTo(expectedMovement))
          }
        }
      }

      "there are multiple batches of messages for the same movement" should {
        "must retrieve all messages" in {
          val message1Timestamp = now.plus(1, ChronoUnit.SECONDS)
          val message2Timestamp = message1Timestamp.plus(1, ChronoUnit.SECONDS)
          val message3Timestamp = message2Timestamp.plus(1, ChronoUnit.SECONDS)
          val consignor         = "testErn"
          val consignee         = "testConsignee"
          val lrnMovement       = Movement(None, "lrnie8158976912", consignor, Some(consignee))

          val ie704                = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912"))
          )
          val firstMessage         = IE704Message.createFromXml(ie704)
          val firstExpectedMessage =
            Message(
              utils.encode(firstMessage.toXml.toString()),
              "IE704",
              "XI000001",
              consignor,
              Set("boxId1"),
              message1Timestamp
            )

          val ie7042                = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(IE704, "XI000002", localReferenceNumber = Some("lrnie8158976912"))
          )
          val secondMessage         = IE704Message.createFromXml(ie7042)
          val secondExpectedMessage =
            Message(
              utils.encode(secondMessage.toXml.toString()),
              "IE704",
              "XI000002",
              consignor,
              Set("boxId1"),
              message2Timestamp
            )

          val ie801                 = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(
              IE801,
              "GB00001",
              Some(consignee),
              Some("23XI00000000000000012"),
              Some("lrnie8158976912")
            )
          )
          val thirdMessage          = IE801Message.createFromXml(ie801)
          val thirdExpectedMessage  =
            Message(
              utils.encode(thirdMessage.toXml.toString()),
              "IE801",
              "GB00001",
              consignor,
              Set("boxId1"),
              message3Timestamp
            )
          val fourthExpectedMessage = thirdExpectedMessage.copy(
            recipient = consignee,
            boxesToNotify = Set("boxId2")
          )

          val firstExpectedMovement  =
            lrnMovement.copy(messages = Seq(firstExpectedMessage), lastUpdated = message1Timestamp)
          val secondExpectedMovement = lrnMovement.copy(
            messages = Seq(firstExpectedMessage, secondExpectedMessage),
            lastUpdated = message2Timestamp
          )
          val thirdExpectedMovement  = lrnMovement.copy(
            messages = Seq(firstExpectedMessage, secondExpectedMessage, thirdExpectedMessage, fourthExpectedMessage),
            administrativeReferenceCode = Some("23XI00000000000000012"),
            lastUpdated = message3Timestamp
          )

          when(dateTimeService.timestamp())
            .thenReturn(lastRetrievedTimestamp, message1Timestamp, message2Timestamp, message3Timestamp)
          when(movementRepository.getAllBy(any)).thenReturn(
            Future.successful(Seq(lrnMovement)),
            Future.successful(Seq(firstExpectedMovement)),
            Future.successful(Seq(secondExpectedMovement))
          )

          when(movementRepository.save(any)).thenReturn(Future.successful(Done))

          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
          when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
          when(boxIdRepository.getBoxIds(consignee)).thenReturn(Future.successful(Set("boxId2")))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(
            Future.successful(GetMessagesResponse(Seq(firstMessage), 3)),
            Future.successful(GetMessagesResponse(Seq(secondMessage), 2)),
            Future.successful(GetMessagesResponse(Seq(thirdMessage), 1))
          )
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(consignor, None).futureValue

          val movementCaptor = ArgCaptor[Movement]

          verify(dateTimeService, times(4)).timestamp()
          verify(messageConnector, times(3)).getNewMessages(eqTo(consignor))(any)
          verify(movementRepository, times(3)).getAllBy(eqTo(consignor))
          verify(movementRepository, times(3)).save(movementCaptor)
          verify(messageConnector, times(3)).acknowledgeMessages(eqTo(consignor))(any)

          movementCaptor.values.head mustEqual firstExpectedMovement
          movementCaptor.values(1) mustEqual secondExpectedMovement
          movementCaptor.values(2) mustEqual thirdExpectedMovement
        }
      }
      "there are multiple messages for different movements"          should {
        "must update all movements" in {
          val movement1Timestamp = now.plus(1, ChronoUnit.SECONDS)
          val movement2Timestamp = movement1Timestamp.plus(1, ChronoUnit.SECONDS)
          val movement3Timestamp = movement2Timestamp.plus(1, ChronoUnit.SECONDS)
          val consignor          = "testErn"
          val consignee          = "testConsignee"
          val movement1          = Movement(None, "lrn1", consignor, Some(consignee))
          val ie801_1            = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(IE801, "message1", Some(consignee), Some("arc1"), Some("lrn1"))
          )
          val movement2          = Movement(None, "lrn2", consignor, Some(consignee))
          val ie801_2            = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(IE801, "message2", Some(consignee), Some("arc2"), Some("lrn2"))
          )
          val movement3          = Movement(None, "lrn3", consignor, Some(consignee))
          val ie801_3            = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(IE801, "message3", Some(consignee), Some("arc3"), Some("lrn3"))
          )
          val messages           = Seq(
            IE801Message.createFromXml(ie801_1),
            IE801Message.createFromXml(ie801_2),
            IE801Message.createFromXml(ie801_3)
          )
          val expectedMovement1  = movement1.copy(
            administrativeReferenceCode = Some("arc1"),
            messages = Seq(
              Message(
                utils.encode(messages.head.toXml.toString()),
                "IE801",
                "message1",
                consignor,
                Set.empty,
                movement1Timestamp
              ),
              Message(
                utils.encode(messages.head.toXml.toString()),
                "IE801",
                "message1",
                consignee,
                Set.empty,
                movement1Timestamp
              )
            ),
            lastUpdated = movement1Timestamp
          )
          val expectedMovement2  = movement2.copy(
            administrativeReferenceCode = Some("arc2"),
            messages = Seq(
              Message(
                utils.encode(messages(1).toXml.toString()),
                "IE801",
                "message2",
                consignor,
                Set.empty,
                movement2Timestamp
              ),
              Message(
                utils.encode(messages(1).toXml.toString()),
                "IE801",
                "message2",
                consignee,
                Set.empty,
                movement2Timestamp
              )
            ),
            lastUpdated = movement2Timestamp
          )
          val expectedMovement3  = movement3.copy(
            administrativeReferenceCode = Some("arc3"),
            messages = Seq(
              Message(
                utils.encode(messages(2).toXml.toString()),
                "IE801",
                "message3",
                consignor,
                Set.empty,
                movement3Timestamp
              ),
              Message(
                utils.encode(messages(2).toXml.toString()),
                "IE801",
                "message3",
                consignee,
                Set.empty,
                movement3Timestamp
              )
            ),
            lastUpdated = movement3Timestamp
          )

          when(dateTimeService.timestamp())
            .thenReturn(lastRetrievedTimestamp, movement1Timestamp, movement2Timestamp, movement3Timestamp)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement1, movement2, movement3)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(messages, 3)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(consignor, None).futureValue

          verify(dateTimeService, times(4)).timestamp()
          verify(movementRepository).save(expectedMovement1)
          verify(movementRepository).save(expectedMovement2)
          verify(movementRepository).save(expectedMovement3)
        }
      }
    }
    "last retrieved is before the throttle cut-off" when {
      "calls downstream service to get messages" in {
        val ern      = "testErn"
        val movement = Movement(None, "LRN", "Consignor", None)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(movementRepository.migrateLastUpdated(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

        messageService.updateMessages(ern, Some(now.minus(6, ChronoUnit.MINUTES))).futureValue

        verify(messageConnector).getNewMessages(eqTo(ern))(any)
        verify(ernRetrievalRepository).setLastRetrieved(ern, lastRetrievedTimestamp)
      }
    }
    "last retrieved is at the throttle cut-off" when {
      "does not call downstream service to get messages" in {
        val ern      = "testErn"
        val movement = Movement(None, "LRN", "Consignor", None)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(movementRepository.migrateLastUpdated(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any))
          .thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

        messageService.updateMessages(ern, Some(lastRetrievedTimestamp.minus(5, ChronoUnit.MINUTES))).futureValue

        verify(messageConnector, never).getNewMessages(any)(any)
        verify(ernRetrievalRepository, never).setLastRetrieved(any, any)
      }
    }
    "last retrieved is after the throttle cut-off" when {
      "we try to retrieve messages but there are none" should {
        "does not call downstream service to get messages" in {
          val ern      = "testErn"
          val movement = Movement(None, "LRN", "Consignor", None)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(movementRepository.migrateLastUpdated(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

          messageService.updateMessages(ern, Some(lastRetrievedTimestamp.minus(5, ChronoUnit.MINUTES))).futureValue

          verify(messageConnector, never).getNewMessages(any)(any)
          verify(ernRetrievalRepository, never).setLastRetrieved(any, any)
        }
      }
    }
    "last retrieved is after the migrate lastUpdated cut-off" when {
      "we try to migrate lastUpdated" should {
        "does not migrate lastUpdated" in {
          val ern      = "testErn"
          val movement = Movement(None, "LRN", "Consignor", None)
          when(movementRepository.migrateLastUpdated(any)).thenReturn(Future.successful(Done))
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

          messageService.updateMessages(ern, Some(migrateCutoffTimestamp.plus(5, ChronoUnit.MINUTES))).futureValue

          verify(movementRepository, never).migrateLastUpdated(any)
        }
      }
    }
    "last retrieved is before the migrate lastUpdated cut-off" when {
      "we try to migrate lastUpdated" should {
        "does migrate lastUpdated" in {
          val ern      = "testErn"
          val movement = Movement(None, "LRN", "Consignor", None)
          when(movementRepository.migrateLastUpdated(any)).thenReturn(Future.successful(Done))
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

          messageService.updateMessages(ern, Some(migrateCutoffTimestamp.minus(5, ChronoUnit.MINUTES))).futureValue

          verify(movementRepository).migrateLastUpdated(any)
        }
      }
    }
    "we get no movements for the ern" when {
      "there is a movement for the arc of a received IE801" when {
        "the consignee is different" should {
          "update the consignee on the movement" in { // TODO is this test valid?
            val consignor        = "testErn"
            val oldConsignee     = "Consignee"
            val newConsignee     = "NewConsignee"
            val movement         = Movement(
              newId,
              None,
              "lrnie8158976912",
              consignor,
              Some(oldConsignee),
              Some("23XI00000000000000012"),
              updateOrCreateTimestamp,
              Seq.empty
            )
            val ie801            = XmlMessageGeneratorFactory.generate(
              consignor,
              MessageParams(
                IE801,
                "GB00001",
                Some(newConsignee),
                Some("23XI00000000000000012"),
                Some("lrnie8158976912")
              )
            )
            val messages         = Seq(IE801Message.createFromXml(ie801))
            val expectedMessages =
              Seq(
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  consignor,
                  Set("boxId1"),
                  updateOrCreateTimestamp
                ),
                Message(
                  utils.encode(messages.head.toXml.toString()),
                  "IE801",
                  "GB00001",
                  newConsignee,
                  Set("boxId2"),
                  updateOrCreateTimestamp
                )
              )
            val expectedMovement =
              movement.copy(
                messages = expectedMessages,
                administrativeReferenceCode = Some("23XI00000000000000012"),
                consigneeId = Some(newConsignee)
              )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.getByArc(any)).thenReturn(Future.successful(Some(movement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
            when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
            when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
            when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
            when(boxIdRepository.getBoxIds(newConsignee)).thenReturn(Future.successful(Set("boxId2")))
            when(messageConnector.getNewMessages(any)(any))
              .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(consignor, None).futureValue

            verify(movementRepository).save(expectedMovement)
          }
        }

      }
    }

    "the existing movement has a consignee" when {
      "we get a 801, it should change the consignee" in {
        val consignor        = "testErn"
        val oldConsignee     = "Consignee"
        val newConsignee     = "testConsignee"
        val movement         = Movement(
          newId,
          None,
          "lrnie8158976912",
          consignor,
          Some(oldConsignee),
          Some("23XI00000000000000012"),
          updateOrCreateTimestamp,
          Seq.empty
        )
        val ie801            = XmlMessageGeneratorFactory.generate(
          consignor,
          MessageParams(IE801, "GB00001", Some(newConsignee), Some("23XI00000000000000012"), Some("lrnie8158976912"))
        )
        val messages         = Seq(IE801Message.createFromXml(ie801))
        val expectedMessages =
          Seq(
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE801",
              "GB00001",
              consignor,
              Set("boxId1"),
              updateOrCreateTimestamp
            ),
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE801",
              "GB00001",
              newConsignee,
              Set("boxId2"),
              updateOrCreateTimestamp
            )
          )
        val expectedMovement =
          movement.copy(
            messages = expectedMessages,
            administrativeReferenceCode = Some("23XI00000000000000012"),
            consigneeId = Some(newConsignee)
          )

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
        when(boxIdRepository.getBoxIds(newConsignee)).thenReturn(Future.successful(Set("boxId2")))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(consignor, None).futureValue

        verify(movementRepository).save(expectedMovement)
      }

      "we get an 813, it should change the consignee" in {
        val ern              = "testErn"
        val oldConsignee     = "Consignee"
        val newConsignee     = "testConsignee"
        val movement         = Movement(
          newId,
          None,
          "lrnie8158976912",
          ern,
          Some(oldConsignee),
          Some("23XI00000000000000012"),
          updateOrCreateTimestamp,
          Seq.empty
        )
        val ie813            = XmlMessageGeneratorFactory.generate(
          ern,
          MessageParams(IE813, "GB00001", Some(newConsignee), Some("23XI00000000000000012"), Some("lrnie8158976912"))
        )
        val messages         = Seq(IE813Message.createFromXml(ie813))
        val expectedMessages =
          Seq(
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE813",
              "GB00001",
              ern,
              Set.empty,
              updateOrCreateTimestamp
            )
          )
        val expectedMovement = movement.copy(messages = expectedMessages, consigneeId = Some(newConsignee))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(ern, None).futureValue

        verify(movementRepository).save(expectedMovement)
      }
    }

    "the existing movement doesn't have a consignee" when {
      "we get a 801, it should set the consignee" in {
        val consignor        = "testErn"
        val consignee        = "testConsignee"
        val movement         =
          Movement(newId, None, "lrnie8158976912", consignor, None, None, updateOrCreateTimestamp, Seq.empty)
        val ie801            = XmlMessageGeneratorFactory.generate(
          consignor,
          MessageParams(IE801, "GB00001", Some(consignee), Some("23XI00000000000000012"), Some("lrnie8158976912"))
        )
        val messages         = Seq(IE801Message.createFromXml(ie801))
        val expectedMessages =
          Seq(
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE801",
              "GB00001",
              consignor,
              Set.empty,
              updateOrCreateTimestamp
            ),
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE801",
              "GB00001",
              consignee,
              Set.empty,
              updateOrCreateTimestamp
            )
          )
        val expectedMovement = movement.copy(
          messages = expectedMessages,
          consigneeId = Some(consignee),
          administrativeReferenceCode = Some("23XI00000000000000012")
        )

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(consignor, None).futureValue

        verify(movementRepository).save(expectedMovement)
      }

      "we get an 813, it should set the consignee" in {
        val ern              = "testErn"
        val movement         =
          Movement(
            newId,
            None,
            "lrnie8158976912",
            ern,
            None,
            Some("23XI00000000000000012"),
            updateOrCreateTimestamp,
            Seq.empty
          )
        val ie813            = XmlMessageGeneratorFactory.generate(
          ern,
          MessageParams(IE813, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912"))
        )
        val messages         = Seq(IE813Message.createFromXml(ie813))
        val expectedMessages =
          Seq(
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE813",
              "GB00001",
              ern,
              Set.empty,
              updateOrCreateTimestamp
            )
          )
        val expectedMovement = movement.copy(messages = expectedMessages, consigneeId = Some("testConsignee"))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(ern, None).futureValue

        verify(movementRepository).save(expectedMovement)
      }
    }

    "the existing movement doesn't have an ARC" when {
      "we get a 801, it should set the ARC" in {
        val consignor        = "testErn"
        val consignee        = "testConsignee"
        val movement         =
          Movement(
            newId,
            None,
            "lrnie8158976912",
            consignor,
            Some("testConsignee"),
            None,
            updateOrCreateTimestamp,
            Seq.empty
          )
        val ie801            = XmlMessageGeneratorFactory.generate(
          consignor,
          MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912"))
        )
        val messages         = Seq(IE801Message.createFromXml(ie801))
        val expectedMessages =
          Seq(
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE801",
              "GB00001",
              consignor,
              Set("boxId1"),
              updateOrCreateTimestamp
            ),
            Message(
              utils.encode(messages.head.toXml.toString()),
              "IE801",
              "GB00001",
              consignee,
              Set("boxId2"),
              updateOrCreateTimestamp
            )
          )

        val expectedMovement =
          movement.copy(messages = expectedMessages, administrativeReferenceCode = Some("23XI00000000000000012"))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
        when(boxIdRepository.getBoxIds(consignee)).thenReturn(Future.successful(Set("boxId2")))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(consignor, None).futureValue

        verify(movementRepository).save(expectedMovement)
      }
    }

    "the movement has no messages" when {
      "we get a duplicate messages back from the downstream service" when {
        "only one message is added to the movement" in {
          val consignor        = "testErn"
          val consignee        = "testConsignee"
          val movement         = Movement(
            newId,
            None,
            "lrnie8158976912",
            consignor,
            Some(consignee),
            None,
            updateOrCreateTimestamp,
            Seq.empty
          )
          val ie801            = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(
              IE801,
              "GB00001",
              Some(consignee),
              Some("23XI00000000000000012"),
              Some("lrnie8158976912")
            )
          )
          val messages         = Seq(IE801Message.createFromXml(ie801), IE801Message.createFromXml(ie801))
          val expectedMessages =
            Seq(
              Message(
                utils.encode(messages.head.toXml.toString()),
                "IE801",
                "GB00001",
                consignor,
                Set.empty,
                updateOrCreateTimestamp
              ),
              Message(
                utils.encode(messages.head.toXml.toString()),
                "IE801",
                "GB00001",
                consignee,
                Set.empty,
                updateOrCreateTimestamp
              )
            )
          val expectedMovement =
            movement.copy(messages = expectedMessages, administrativeReferenceCode = Some("23XI00000000000000012"))

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(messages, 2)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(consignor, None).futureValue

          verify(movementRepository).save(expectedMovement)
        }
      }
    }

    "the movement has a message" when {
      "we get the same message back from the downstream service" when {
        "the duplicate message is not added to the movement" in {
          val ern              = "testErn"
          val ie801            = XmlMessageGeneratorFactory.generate(
            ern,
            MessageParams(
              IE801,
              "GB00001",
              Some("testConsignee"),
              Some("23XI00000000000000012"),
              Some("lrnie8158976912")
            )
          )
          val messages         = Seq(IE801Message.createFromXml(ie801))
          val existingMessages =
            Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
          val movement         = Movement(
            newId,
            None,
            "lrnie8158976912",
            ern,
            Some("testConsignee"),
            Some("23XI00000000000000012"),
            now,
            existingMessages
          )

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern, None).futureValue

          verify(movementRepository).save(movement)
        }

        "the duplicate message is added to the movement if recipient is different" in {
          val consignor = "testErn"
          val consignee = "testConsignee"
          val ie801     = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(
              IE801,
              "GB00001",
              Some(consignee),
              Some("23XI00000000000000012"),
              Some("lrnie8158976912")
            )
          )

          val ie801Consignee = XmlMessageGeneratorFactory.generate(
            consignor,
            MessageParams(
              IE801,
              "GB00001",
              Some(consignee),
              Some("23XI00000000000000012"),
              Some("lrnie8158976912")
            )
          )

          val messages = Seq(IE801Message.createFromXml(ie801), IE801Message.createFromXml(ie801Consignee))

          val existingMessages =
            Seq(
              Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", consignor, Set("boxId1"), now)
            )

          val expectedMessages =
            Seq(
              Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", consignor, Set("boxId1"), now),
              Message(
                utils.encode(messages(1).toXml.toString()),
                "IE801",
                "GB00001",
                consignee,
                Set("boxId2"),
                updateOrCreateTimestamp
              )
            )

          val movement = Movement(
            newId,
            None,
            "lrnie8158976912",
            consignor,
            Some(consignee),
            Some("23XI00000000000000012"),
            updateOrCreateTimestamp,
            existingMessages
          )

          val expectedMovement = movement.copy(messages = expectedMessages)

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
          when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
          when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
          when(boxIdRepository.getBoxIds(consignor)).thenReturn(Future.successful(Set("boxId1")))
          when(boxIdRepository.getBoxIds(consignee)).thenReturn(Future.successful(Set("boxId2")))
          when(messageConnector.getNewMessages(any)(any))
            .thenReturn(Future.successful(GetMessagesResponse(Seq(messages(1)), 1)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(consignee, None).futureValue

          verify(movementRepository).save(expectedMovement)
        }
      }
    }

    "the movement has an lrn, and we try to add an 801 message with an lrn that is a substring of that lrn" should {
      "not add that 801 message, because it doesn't belong on that movement - it should create a new movement instead" in {

        val ern      = "testErn"
        val movement = Movement(
          newId,
          None,
          "2",
          ern,
          Some("testConsignee"),
          None,
          now,
          Seq.empty
        )

        val new801WithSubstringLrn = Seq(
          IE801Message.createFromXml(
            XmlMessageGeneratorFactory.generate(
              ern,
              MessageParams(
                IE801,
                "GB00001",
                Some("testConsignee"),
                Some("23XI00000000000000099"),
                Some("lrn234")
              )
            )
          )
        )

        val messagesWithSubstringLrn = Seq(
          Message(utils.encode(new801WithSubstringLrn.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now),
          Message(
            utils.encode(new801WithSubstringLrn.head.toXml.toString()),
            "IE801",
            "GB00001",
            "testConsignee",
            Set.empty,
            now
          )
        )

        val expectedMovement = Movement(
          newId,
          None,
          "lrn234",
          ern,
          Some("testConsignee"),
          Some("23XI00000000000000099"),
          now,
          messagesWithSubstringLrn
        )

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)

        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))
        when(dateTimeService.timestamp()).thenReturn(now)

        // These are the mocks that specify the important bits for this test:
        when(messageConnector.getNewMessages(any)(any))
          .thenReturn(Future.successful(GetMessagesResponse(new801WithSubstringLrn, 1)))
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
        when(movementRepository.getMovementByLRNAndERNIn(any, any)).thenReturn(Future.successful(Seq.empty))

        messageService.updateMessages(ern, None).futureValue

        verify(movementRepository).save(expectedMovement)

      }
    }
  }

  "update all messages" when {
    "all erns are successful" should {
      "process all erns" in {
        val ern1NewMessages = Seq(
          IE704Message.createFromXml(
            XmlMessageGeneratorFactory
              .generate("ern1", MessageParams(IE704, "messageId1", localReferenceNumber = Some("lrn1")))
          )
        )
        val ern1Movement    = Movement(
          None,
          "lrn1",
          "ern1",
          None,
          messages = Seq(
            Message(utils.encode(ern1NewMessages.head.toXml.toString()), "IE704", "messageId1", "ern1", Set.empty, now)
          )
        )
        val ern2NewMessages = Seq(
          IE704Message.createFromXml(
            XmlMessageGeneratorFactory
              .generate("ern2", MessageParams(IE704, "messageId2", localReferenceNumber = Some("lrn2")))
          )
        )
        val ern2Movement    = Movement(
          None,
          "lrn2",
          "ern2",
          None,
          messages = Seq(
            Message(utils.encode(ern2NewMessages.head.toXml.toString()), "IE704", "messageId2", "ern2", Set.empty, now)
          )
        )

        when(movementRepository.getAllBy(eqTo("ern1"))).thenReturn(Future.successful(Seq(ern1Movement)))
        when(movementRepository.getAllBy(eqTo("ern2"))).thenReturn(Future.successful(Seq(ern2Movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(eqTo("ern1"))(any))
          .thenReturn(Future.successful(GetMessagesResponse(ern1NewMessages, 1)))
        when(messageConnector.getNewMessages(eqTo("ern2"))(any))
          .thenReturn(Future.successful(GetMessagesResponse(ern2NewMessages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateAllMessages(Set("ern1", "ern2")).futureValue

        verify(movementRepository).save(ern1Movement)
        verify(movementRepository).save(ern2Movement)
      }
    }
    "an ern fails"            should {
      "continue with further erns and not fail the future" in {
        val ern1NewMessages = Seq(
          IE704Message.createFromXml(
            XmlMessageGeneratorFactory
              .generate("ern1", MessageParams(IE704, "messageId1", localReferenceNumber = Some("lrn1")))
          )
        )
        val ern1Movement    = Movement(
          None,
          "lrn1",
          "ern1",
          None,
          messages = Seq(
            Message(utils.encode(ern1NewMessages.head.toXml.toString()), "IE704", "messageId1", "ern1", Set.empty, now)
          )
        )
        val ern2NewMessages = Seq(
          IE704Message.createFromXml(
            XmlMessageGeneratorFactory
              .generate("ern2", MessageParams(IE704, "messageId2", localReferenceNumber = Some("lrn2")))
          )
        )
        val ern2Movement    = Movement(
          None,
          "lrn2",
          "ern2",
          None,
          messages = Seq(
            Message(utils.encode(ern2NewMessages.head.toXml.toString()), "IE704", "messageId2", "ern2", Set.empty, now)
          )
        )

        when(movementRepository.getAllBy(eqTo("ern1"))).thenReturn(Future.successful(Seq(ern1Movement)))
        when(movementRepository.getAllBy(eqTo("ern2"))).thenReturn(Future.successful(Seq(ern2Movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(eqTo("ern1"))(any))
          .thenReturn(Future.failed(new RuntimeException("Error!")))
        when(messageConnector.getNewMessages(eqTo("ern2"))(any))
          .thenReturn(Future.successful(GetMessagesResponse(ern2NewMessages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateAllMessages(Set("ern1", "ern2")).futureValue

        verify(movementRepository, never).save(ern1Movement)
        verify(movementRepository).save(ern2Movement)
      }
    }

    "there are many messages" should {
      "update them if they all correspond to one movement" in {
        val message1Timestamp = now.plus(1, ChronoUnit.SECONDS)
        val message2Timestamp = message1Timestamp.plus(1, ChronoUnit.SECONDS)
        val message3Timestamp = message2Timestamp.plus(1, ChronoUnit.SECONDS)
        val message4Timestamp = message3Timestamp.plus(1, ChronoUnit.SECONDS)
        val message1          = IE801Message.createFromXml(
          XmlMessageGeneratorFactory
            .generate("ern1", MessageParams(IE801, "XI0000021a", Some("AT00000602078"), Some("arc"), Some("lrn1")))
        )
        val message2          = IE819Message.createFromXml(
          XmlMessageGeneratorFactory.generate("ern1", MessageParams(IE819, "X00008a", Some("token"), Some("arc")))
        )
        val message3          = IE807Message.createFromXml(
          XmlMessageGeneratorFactory
            .generate("ern1", MessageParams(IE807, "XI0000021b", Some("AT00000602078"), Some("arc"), Some("lrn1")))
        )
        val message4          = IE840Message.createFromXml(
          XmlMessageGeneratorFactory.generate("ern1", MessageParams(IE840, "X00008b", Some("token"), Some("arc")))
        )

        val ern1NewMessages = Seq(message1, message2, message3, message4)
        val ern1Movement    = Movement(
          None,
          "lrn1",
          "ern1",
          None,
          messages = Seq.empty
        )

        when(dateTimeService.timestamp()).thenReturn(
          lastRetrievedTimestamp,
          message1Timestamp,
          message2Timestamp,
          message3Timestamp,
          message4Timestamp
        )
        when(movementRepository.getAllBy(eqTo("ern1"))).thenReturn(Future.successful(Seq(ern1Movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(eqTo("ern1"))(any))
          .thenReturn(Future.successful(GetMessagesResponse(ern1NewMessages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages("ern1", None).futureValue

        val expectedMovement = ern1Movement.copy(
          consigneeId = Some("AT00000602078"),
          administrativeReferenceCode = Some("arc"),
          messages = Seq(
            Message(
              utils.encode(message1.toXml.toString()),
              "IE801",
              "XI0000021a",
              "ern1",
              Set.empty,
              message1Timestamp
            ),
            Message(
              utils.encode(message1.toXml.toString()),
              "IE801",
              "XI0000021a",
              "AT00000602078",
              Set.empty,
              message1Timestamp
            ),
            Message(
              utils.encode(message2.toXml.toString()),
              "IE819",
              "X00008a",
              "ern1",
              Set.empty,
              message2Timestamp
            ),
            Message(
              utils.encode(message3.toXml.toString()),
              "IE807",
              "XI0000021b",
              "ern1",
              Set.empty,
              message3Timestamp
            ),
            Message(
              utils.encode(message4.toXml.toString()),
              "IE840",
              "X00008b",
              "ern1",
              Set.empty,
              message4Timestamp
            )
          ),
          lastUpdated = message4Timestamp
        )

        val captor = ArgCaptor[Movement]
        verify(dateTimeService, times(5)).timestamp()
        verify(movementRepository, times(1)).save(captor.capture)

        captor.value mustEqual expectedMovement
      }
      "update them if they correspond to more than one movement, and create a movement if needed" in {
        val message1Timestamp = now.plus(1, ChronoUnit.SECONDS)
        val message2Timestamp = message1Timestamp.plus(1, ChronoUnit.SECONDS)
        val message3Timestamp = message2Timestamp.plus(1, ChronoUnit.SECONDS)
        val message4Timestamp = message3Timestamp.plus(1, ChronoUnit.SECONDS)
        val message5Timestamp = message4Timestamp.plus(1, ChronoUnit.SECONDS)
        val message1          = IE801Message.createFromXml(
          XmlMessageGeneratorFactory
            .generate("ern1", MessageParams(IE801, "XI0000021a", Some("AT00000602078"), Some("arc"), Some("lrn1")))
        )
        val message2          = IE819Message.createFromXml(
          XmlMessageGeneratorFactory.generate("ern1", MessageParams(IE819, "X00008a", Some("token"), Some("arc")))
        )
        val message3          = IE807Message.createFromXml(
          XmlMessageGeneratorFactory
            .generate("ern1", MessageParams(IE807, "XI0000021b", Some("AT00000602078"), Some("arc"), Some("lrn1")))
        )
        val message4          = IE840Message.createFromXml(
          XmlMessageGeneratorFactory.generate("ern1", MessageParams(IE840, "X00008b", Some("token"), Some("arc")))
        )
        val message5          = IE801Message.createFromXml(
          XmlMessageGeneratorFactory
            .generate("ern1", MessageParams(IE801, "XI0000099", Some("AT00000602078"), Some("arc2"), Some("lrn2")))
        )

        val ern1NewMessages = Seq(message1, message2, message3, message4, message5)
        val ern1Movement    = Movement(
          None,
          "lrn1",
          "ern1",
          None,
          messages = Seq.empty
        )

        when(dateTimeService.timestamp()).thenReturn(
          lastRetrievedTimestamp,
          message1Timestamp,
          message2Timestamp,
          message3Timestamp,
          message4Timestamp,
          message5Timestamp
        )
        when(movementRepository.getAllBy(eqTo("ern1"))).thenReturn(Future.successful(Seq(ern1Movement)))
        when(movementRepository.getByArc(any)).thenReturn(Future.successful(None))
        when(movementRepository.getMovementByLRNAndERNIn(any, any)).thenReturn(Future.successful(Seq.empty))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(mongoLockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(lock)))
        when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.unit)
        when(ernRetrievalRepository.setLastRetrieved(any, any)).thenReturn(Future.successful(None))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(eqTo("ern1"))(any))
          .thenReturn(Future.successful(GetMessagesResponse(ern1NewMessages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))
        when(correlationIdService.generateCorrelationId()).thenReturn(newId)

        messageService.updateMessages("ern1", None).futureValue

        val expectedMovement1 = ern1Movement.copy(
          consigneeId = Some("AT00000602078"),
          administrativeReferenceCode = Some("arc"),
          messages = Seq(
            Message(
              utils.encode(message1.toXml.toString()),
              "IE801",
              "XI0000021a",
              "ern1",
              Set.empty,
              message1Timestamp
            ),
            Message(
              utils.encode(message1.toXml.toString()),
              "IE801",
              "XI0000021a",
              "AT00000602078",
              Set.empty,
              message1Timestamp
            ),
            Message(
              utils.encode(message2.toXml.toString()),
              "IE819",
              "X00008a",
              "ern1",
              Set.empty,
              message2Timestamp
            ),
            Message(
              utils.encode(message3.toXml.toString()),
              "IE807",
              "XI0000021b",
              "ern1",
              Set.empty,
              message3Timestamp
            ),
            Message(
              utils.encode(message4.toXml.toString()),
              "IE840",
              "X00008b",
              "ern1",
              Set.empty,
              message4Timestamp
            )
          ),
          lastUpdated = message4Timestamp
        )
        val expectedMovement2 = Movement(
          newId,
          None,
          "lrn2",
          "ern1",
          Some("AT00000602078"),
          Some("arc2"),
          message5Timestamp,
          Seq(
            Message(
              utils.encode(message5.toXml.toString()),
              "IE801",
              "XI0000099",
              "ern1",
              Set.empty,
              message5Timestamp
            ),
            Message(
              utils.encode(message5.toXml.toString()),
              "IE801",
              "XI0000099",
              "AT00000602078",
              Set.empty,
              message5Timestamp
            )
          )
        )

        val movementCaptor = ArgCaptor[Movement]
        verify(dateTimeService, times(6)).timestamp()
        verify(movementRepository, times(2)).save(movementCaptor.capture)

        movementCaptor.values.head mustBe expectedMovement2
        movementCaptor.values(1) mustBe expectedMovement1
      }
    }
  }
}
