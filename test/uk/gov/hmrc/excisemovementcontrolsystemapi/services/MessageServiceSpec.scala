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
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{times, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageServiceSpec extends PlaySpec
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach {

  private val movementRepository = mock[MovementRepository]
  private val ernRetrievalRepository = mock[ErnRetrievalRepository]
  private val boxIdRepository = mock[BoxIdRepository]
  private val messageConnector = mock[MessageConnector]
  private val dateTimeService = mock[DateTimeService]
  private val correlationIdService = mock[CorrelationIdService]
  private val auditService = mock[AuditService]

  private lazy val messageService = app.injector.instanceOf[MessageService]

  private val utils = new EmcsUtils
  private val now = Instant.now
  private val newId = UUID.randomUUID().toString
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[ErnRetrievalRepository].toInstance(ernRetrievalRepository),
      bind[BoxIdRepository].toInstance(boxIdRepository),
      bind[MessageConnector].toInstance(messageConnector),
      bind[DateTimeService].toInstance(dateTimeService),
      bind[CorrelationIdService].toInstance(correlationIdService),
      bind[AuditService].toInstance(auditService),
    )
    .configure(
      "microservice.services.eis.throttle-cutoff" -> "5 minutes"
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      movementRepository,
      ernRetrievalRepository,
      boxIdRepository,
      messageConnector,
      dateTimeService,
      correlationIdService,
      auditService
    )
  }

  "updateMessages" when {

    "last retrieved is empty" when {
      "there is a movement but we have never retrieved anything" when {
        "we try to retrieve messages but there are none" should {
          "update last retrieval time for ern" in {
            val ern = "testErn"
            val movement = Movement(None, "LRN", "Consignor", None)
            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository, never()).getAllBy(any)
            verify(movementRepository, never()).save(any)
            verify(ernRetrievalRepository).save(eqTo(ern))
            verify(messageConnector, never()).acknowledgeMessages(any)(any)
          }
        }
        "we try to retrieve messages and there are some" should {
          "add messages to only the movement with the right LRN" in {
            val ern = "testErn"
            val lrnMovement = Movement(None, "lrnie8158976912", ern, None)
            val notLrnMovement = Movement(None, "notTheLrn", ern, None)
            val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912")))
            val messages = Seq(IE704Message.createFromXml(ie704))
            val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", ern, Set.empty, now))
            val expectedMovement = lrnMovement.copy(messages = expectedMessages)
            val unexpectedMovement = notLrnMovement.copy(messages = expectedMessages)

            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(lrnMovement, notLrnMovement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(ern)
            verify(movementRepository, never).save(unexpectedMovement)
            verify(movementRepository).save(expectedMovement)
            verify(ernRetrievalRepository).save(ern)
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
          "add messages to only the movement with the right ARC" in {
            val ern = "testErn"
            val arcMovement = Movement(None, "notTheLrn", ern, Some("testConsignee"), administrativeReferenceCode = Some("23XI00000000000000012"))
            val notArcMovement = Movement(None, "notTheLrn", ern, None)
            val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
            val messages = Seq(IE801Message.createFromXml(ie801))
            val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
            val expectedMovement = arcMovement.copy(messages = expectedMessages)
            val unexpectedMovement = notArcMovement.copy(messages = expectedMessages)

            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(arcMovement, notArcMovement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(ern)
            verify(movementRepository, never).save(unexpectedMovement)
            verify(movementRepository).save(expectedMovement)
            verify(ernRetrievalRepository).save(ern)
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
          "add the boxIds to notify to saved messages" in {
            val ern = "testErn"
            val boxIds = Set("box1", "box2")
            val arcMovement = Movement(None, "notTheLrn", ern, Some("testConsignee"), administrativeReferenceCode = Some("23XI00000000000000012"))
            val notArcMovement = Movement(None, "notTheLrn", ern, None)
            val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
            val messages = Seq(IE801Message.createFromXml(ie801))
            val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, boxIds, now))
            val expectedMovement = arcMovement.copy(messages = expectedMessages)
            val unexpectedMovement = notArcMovement.copy(messages = expectedMessages)

            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(arcMovement, notArcMovement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(boxIds))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(ern)
            verify(movementRepository, never).save(unexpectedMovement)
            verify(movementRepository).save(expectedMovement)
            verify(ernRetrievalRepository).save(ern)
            verify(boxIdRepository).getBoxIds(ern)
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
        }
      }
      "there is a movement that already has messages" when {
        "we try to retrieve new messages and there are some" should {
          "add the new messages to the movement" in {
            val ern = "testErn"
            val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
            val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912")))
            val movement = Movement(None, "lrnie8158976912", ern, Some("testConsignee"), Some("23XI00000000000000012"), messages = Seq(Message(utils.encode(ie801.toString()), "IE801", "GB00001", ern, Set.empty, now)))
            val messages = Seq(IE704Message.createFromXml(ie704))
            val expectedMessages = movement.messages ++ Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", ern, Set.empty, now))
            val expectedMovement = movement.copy(messages = expectedMessages)

            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(ernRetrievalRepository).save(eqTo(ern))
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
        }
      }
      "there are multiple movements for one message (an 829)" should {
        "update all relevant movements with the message" in {
          // 829 doesn't have consignor in it - can't make a movement from this
          // movements created here won't get push notifications

          val ern = "testErn"
          val ie829 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE829, "XI000001", consigneeErn = Some("testConsignee")))
          val arc1 = "23XI00000000000056339"
          val arc2 = "23XI00000000000056340"
          val movement1 = Movement(None, "???", "???", None, Some(arc1), now, Seq.empty)
          val movement2 = Movement(None, "???", "???", None, Some(arc2), now, Seq.empty)
          val message = IE829Message.createFromXml(ie829)

          val expectedMessage = Seq(Message(utils.encode(message.toXml.toString()), "IE829", "XI000001", ern, Set.empty, now))
          val expectedMovement1 = movement1.copy(messages = expectedMessage)
          val expectedMovement2 = movement2.copy(messages = expectedMessage)

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement1, movement2)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(Seq(message), 1)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern).futureValue

          val movementCaptor = ArgCaptor[Movement]

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository, times(2)).save(movementCaptor)
          verify(ernRetrievalRepository).save(eqTo(ern))
          verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)

          movementCaptor.values.head mustBe expectedMovement1
          movementCaptor.values(1) mustBe expectedMovement2
        }
      }
      "there is no movement" when {
        "we try to retrieve new messages for an ERN and there are some" should {
          "a new movement should be created from an IE704 message" in {
            val ern = "testErn"
            val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912")))
            val messages = Seq(IE704Message.createFromXml(ie704))
            val expectedMovement = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              None,
              None,
              now,
              messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", ern, Set.empty, now))
            )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(ernRetrievalRepository).save(eqTo(ern))
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
          "a new movement should be created from an IE801 message" in {
            val ern = "testErn"
            val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
            val messages = Seq(IE801Message.createFromXml(ie801))
            val expectedMovement = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              Some("testConsignee"),
              Some("23XI00000000000000012"),
              now,
              messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
            )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(ernRetrievalRepository).save(eqTo(ern))
            verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
          }
          "a message is audited with failure if its not an IE801 or IE704" in {
            val ern = "testErn"
            val ie818 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE818, "GB00001", Some("testConsignee"), Some("23XI00000000000000012")))
            val messages = Seq(IE818Message.createFromXml(ie818))

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(auditService.auditMessage(any, any)(any)).thenReturn(EitherT.pure(()))
            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(auditService).auditMessage(messages.head, s"An IE818 message has been retrieved with no movement, unable to create movement")
          }

          "a single new movement should be created when there are multiple messages for the same ern" in {
            val ern = "testErn"
            val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
            val ie802 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE802, "GB0002", administrativeReferenceCode = Some("23XI00000000000000012")))
            val messages = Seq(IE801Message.createFromXml(ie801), IE802Message.createFromXml(ie802))
            val expectedMovement = Movement(
              newId,
              None,
              "lrnie8158976912",
              "testErn",
              Some("testConsignee"),
              Some("23XI00000000000000012"),
              now,
              messages = Seq(
                Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now),
                Message(utils.encode(messages(1).toXml.toString()), "IE802", "GB0002", ern, Set.empty, now))
            )

            when(correlationIdService.generateCorrelationId()).thenReturn(newId)
            when(dateTimeService.timestamp()).thenReturn(now)
            when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
            when(movementRepository.save(any)).thenReturn(Future.successful(Done))
            when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
            when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
            when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
            when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 0)))
            when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

            messageService.updateMessages(ern).futureValue

            verify(messageConnector).getNewMessages(eqTo(ern))(any)
            verify(movementRepository).getAllBy(eqTo(ern))
            verify(movementRepository).save(eqTo(expectedMovement))
            verify(ernRetrievalRepository).save(eqTo(ern))
          }
        }
      }

      "there are multiple batches of messages" when {

        "must retrieve all messages" in {

          val ern = "testErn"
          val lrnMovement = Movement(None, "lrnie8158976912", ern, Some("testConsignee"))

          val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912")))
          val firstMessage = IE704Message.createFromXml(ie704)
          val firstExpectedMessage = Message(utils.encode(firstMessage.toXml.toString()), "IE704", "XI000001", ern, Set.empty, now)

          val ie7042 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000002", localReferenceNumber = Some("lrnie8158976912")))
          val secondMessage = IE704Message.createFromXml(ie7042)
          val secondExpectedMessage = Message(utils.encode(secondMessage.toXml.toString()), "IE704", "XI000002", ern, Set.empty, now)

          val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
          val thirdMessage = IE801Message.createFromXml(ie801)
          val thirdExpectedMessage = Message(utils.encode(thirdMessage.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now)

          val firstExpectedMovement = lrnMovement.copy(messages = Seq(firstExpectedMessage))
          val secondExpectedMovement = lrnMovement.copy(messages = Seq(firstExpectedMessage, secondExpectedMessage))
          val thirdExpectedMovement = lrnMovement.copy(messages = Seq(firstExpectedMessage, secondExpectedMessage, thirdExpectedMessage), administrativeReferenceCode = Some("23XI00000000000000012"))

          when(dateTimeService.timestamp()).thenReturn(now)

          when(movementRepository.getAllBy(any)).thenReturn(
            Future.successful(Seq(lrnMovement)),
            Future.successful(Seq(firstExpectedMovement)),
            Future.successful(Seq(secondExpectedMovement))
          )

          when(movementRepository.save(any)).thenReturn(Future.successful(Done))

          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(
            Future.successful(GetMessagesResponse(Seq(firstMessage), 3)),
            Future.successful(GetMessagesResponse(Seq(secondMessage), 2)),
            Future.successful(GetMessagesResponse(Seq(thirdMessage), 1))
          )
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern).futureValue

          val movementCaptor = ArgCaptor[Movement]

          verify(messageConnector, times(3)).getNewMessages(eqTo(ern))(any)
          verify(movementRepository, times(3)).getAllBy(eqTo(ern))
          verify(movementRepository, times(3)).save(movementCaptor)
          verify(ernRetrievalRepository).save(eqTo(ern))
          verify(messageConnector, times(3)).acknowledgeMessages(eqTo(ern))(any)

          movementCaptor.values.head mustEqual firstExpectedMovement
          movementCaptor.values(1) mustEqual secondExpectedMovement
          movementCaptor.values(2) mustEqual thirdExpectedMovement
        }
      }
    }
    "last retrieved is before the throttle cut-off" when {
      "calls downstream service to get messages" in {
        val ern = "testErn"
        val movement = Movement(None, "LRN", "Consignor", None)
        when(dateTimeService.timestamp()).thenReturn(now)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(Some(now.minus(6, ChronoUnit.MINUTES))))
        when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

        messageService.updateMessages(ern).futureValue

        verify(messageConnector).getNewMessages(eqTo(ern))(any)
      }
    }
    "last retrieved is at the throttle cut-off" when {
      "we try to retrieve messages but there are none" should {
        "does not call downstream service to get messages" in {
          val ern = "testErn"
          val movement = Movement(None, "LRN", "Consignor", None)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(Some(now.minus(5, ChronoUnit.MINUTES))))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector, never).getNewMessages(any)(any)
        }
      }
    }
    "last retrieved is after the throttle cut-off" when {
      "we try to retrieve messages but there are none" should {
        "does not call downstream service to get messages" in {
          val ern = "testErn"
          val movement = Movement(None, "LRN", "Consignor", None)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(Some(now.minus(4, ChronoUnit.MINUTES))))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(Seq.empty, 0)))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector, never).getNewMessages(any)(any)
        }
      }
    }

    "the existing movement has a consignee" when {
      "we get a 801, it should not change the consignee" in {
        val ern = "testErn"
        val movement = Movement(newId, None, "lrnie8158976912", ern, Some("Consignee"),None, now, Seq.empty)
        val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
        val messages = Seq(IE801Message.createFromXml(ie801))
        val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
        val expectedMovement = movement.copy(messages = expectedMessages, administrativeReferenceCode = Some("23XI00000000000000012"))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(dateTimeService.timestamp()).thenReturn(now)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(ern).futureValue

        verify(movementRepository).save(expectedMovement)
      }
      "we get an 813, it should change the consignee" in {
        val ern = "testErn"
        val movement = Movement(newId, None, "lrnie8158976912", ern, Some("Consignee"), Some("23XI00000000000000012"), now, Seq.empty)
        val ie813 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE813, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
        val messages = Seq(IE813Message.createFromXml(ie813))
        val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE813", "GB00001", ern, Set.empty, now))
        val expectedMovement = movement.copy(messages = expectedMessages, consigneeId = Some("testConsignee"))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(dateTimeService.timestamp()).thenReturn(now)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(ern).futureValue

        verify(movementRepository).save(expectedMovement)
      }
    }
    "the existing movement doesn't have a consignee" when {
      "we get a 801, it should set the consignee" in {
        val ern = "testErn"
        val movement = Movement(newId, None, "lrnie8158976912", ern, None, None, now, Seq.empty)
        val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
        val messages = Seq(IE801Message.createFromXml(ie801))
        val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
        val expectedMovement = movement.copy(messages = expectedMessages, consigneeId = Some("testConsignee"), administrativeReferenceCode = Some("23XI00000000000000012"))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(dateTimeService.timestamp()).thenReturn(now)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(ern).futureValue

        verify(movementRepository).save(expectedMovement)
      }
      "we get an 813, it should set the consignee" in {
        val ern = "testErn"
        val movement = Movement(newId, None, "lrnie8158976912", ern, None, Some("23XI00000000000000012"), now, Seq.empty)
        val ie813 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE813, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
        val messages = Seq(IE813Message.createFromXml(ie813))
        val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE813", "GB00001", ern, Set.empty, now))
        val expectedMovement = movement.copy(messages = expectedMessages, consigneeId = Some("testConsignee"))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(dateTimeService.timestamp()).thenReturn(now)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(ern).futureValue

        verify(movementRepository).save(expectedMovement)
      }
    }
    "the existing movement doesn't have an ARC" when {
      "we get a 801, it should set the ARC" in {
        val ern = "testErn"
        val movement = Movement(newId, None, "lrnie8158976912", ern, Some("testConsignee"), None, now, Seq.empty)
        val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
        val messages = Seq(IE801Message.createFromXml(ie801))
        val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
        val expectedMovement = movement.copy(messages = expectedMessages, administrativeReferenceCode = Some("23XI00000000000000012"))

        when(correlationIdService.generateCorrelationId()).thenReturn(newId)
        when(dateTimeService.timestamp()).thenReturn(now)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
        when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
        when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

        messageService.updateMessages(ern).futureValue

        verify(movementRepository).save(expectedMovement)
      }
    }
    "the movement has no messages" when {
      "we get a duplicate messages back from the downstream service" when {
        "only one message is added to the movement" in {
          val ern = "testErn"
          val movement = Movement(newId, None, "lrnie8158976912", ern, Some("testConsignee"), None, now, Seq.empty)
          val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
          val messages = Seq(IE801Message.createFromXml(ie801), IE801Message.createFromXml(ie801))
          val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
          val expectedMovement = movement.copy(messages = expectedMessages, administrativeReferenceCode = Some("23XI00000000000000012"))

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern).futureValue

          verify(movementRepository).save(expectedMovement)
        }
      }
    }
    "the movement has a message" when {
      "we get the same message back from the downstream service" when {
        "the duplicate message is not added to the movement" in {
          val ern = "testErn"
          val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
          val messages = Seq(IE801Message.createFromXml(ie801))
          val existingMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", ern, Set.empty, now))
          val movement = Movement(newId, None, "lrnie8158976912", ern, Some("testConsignee"), Some("23XI00000000000000012"), now, existingMessages)

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(boxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set.empty))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern).futureValue

          verify(movementRepository).save(movement)
        }
      }
    }
    // TODO what about 829 where one movement is found and another isn't?
  }
}


