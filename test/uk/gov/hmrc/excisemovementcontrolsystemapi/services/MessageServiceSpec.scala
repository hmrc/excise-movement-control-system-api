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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IE704Message, IE801Message, IE802Message, IE829Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class MessageServiceSpec extends PlaySpec
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach {

  private val movementRepository = mock[MovementRepository]
  private val ernRetrievalRepository = mock[ErnRetrievalRepository]
  private val messageConnector = mock[MessageConnector]
  private val dateTimeService = mock[DateTimeService]
  private val correlationIdService = mock[CorrelationIdService]

  private lazy val messageService = app.injector.instanceOf[MessageService]

  private val utils = new EmcsUtils
  private val now = Instant.now
  private val newId = UUID.randomUUID().toString
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[ErnRetrievalRepository].toInstance(ernRetrievalRepository),
      bind[MessageConnector].toInstance(messageConnector),
      bind[DateTimeService].toInstance(dateTimeService),
      bind[CorrelationIdService].toInstance(correlationIdService)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      movementRepository,
      ernRetrievalRepository,
      messageConnector,
      dateTimeService
    )
  }

  "updateMessages" when {
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
          val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now))
          val expectedMovement = lrnMovement.copy(messages = expectedMessages)
          val unexpectedMovement = notLrnMovement.copy(messages = expectedMessages)

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(lrnMovement, notLrnMovement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 0)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository, never).save(eqTo(unexpectedMovement))
          verify(movementRepository).save(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
          verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
        }
        "add messages to only the movement with the right ARC" in {
          val ern = "testErn"
          val arcMovement = Movement(None, "notTheLrn", ern, None, administrativeReferenceCode = Some("23XI00000000000000012"))
          val notArcMovement = Movement(None, "notTheLrn", ern, None)
          val ie801 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE801, "GB00001", Some("testConsignee"), Some("23XI00000000000000012"), Some("lrnie8158976912")))
          val messages = Seq(IE801Message.createFromXml(ie801))
          val expectedMessages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", now))
          val expectedMovement = arcMovement.copy(messages = expectedMessages)
          val unexpectedMovement = notArcMovement.copy(messages = expectedMessages)

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(arcMovement, notArcMovement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 0)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository, never).save(eqTo(unexpectedMovement))
          verify(movementRepository).save(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
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
          val movement = Movement(None, "lrnie8158976912", ern, Some("testConsignee"), Some("23XI00000000000000012"), messages = Seq(Message(utils.encode(ie801.toString()), "IE801", "GB00001", now)))
          val messages = Seq(IE704Message.createFromXml(ie704))
          val expectedMessages = movement.messages ++ Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now))
          val expectedMovement = movement.copy(messages = expectedMessages)

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 0)))
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

        val expectedMessage = Seq(Message(utils.encode(message.toXml.toString()), "IE829", "XI000001", now))
        val expectedMovement1 = movement1.copy(messages = expectedMessage)
        val expectedMovement2 = movement2.copy(messages = expectedMessage)

        when(dateTimeService.timestamp()).thenReturn(now)
        when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement1, movement2)))
        when(movementRepository.save(any)).thenReturn(Future.successful(Done))
        when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
        when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
        when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(Seq(message), 0)))
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
    "there is a no movement" when {
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
            messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now))
          )

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 0)))
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
            messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", now))
          )

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 0)))
          when(messageConnector.acknowledgeMessages(any)(any)).thenReturn(Future.successful(Done))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository).save(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
          verify(messageConnector).acknowledgeMessages(eqTo(ern))(any)
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
              Message(utils.encode(messages.head.toXml.toString()), "IE801", "GB00001", now),
              Message(utils.encode(messages(1).toXml.toString()), "IE802", "GB0002", now))
          )

          when(correlationIdService.generateCorrelationId()).thenReturn(newId)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
          when(movementRepository.save(any)).thenReturn(Future.successful(Done))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
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

    // TODO what about 829 where one movement is found and another isn't?
    // TODO updating messages must be idempotent
  }
}


