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
import org.mockito.MockitoSugar.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE704Message
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
  with TestXml
  with BeforeAndAfterEach {

  private val movementRepository = mock[MovementRepository]
  private val ernRetrievalRepository = mock[ErnRetrievalRepository]
  private val messageConnector = mock[MessageConnector]
  private val dateTimeService = mock[DateTimeService]
  private val messageService = app.injector.instanceOf[MessageService]
  private val utils = new EmcsUtils
  private val now = Instant.now
  private val newId = UUID.randomUUID().toString
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  case object TestMovementCreator extends MovementCreator {
    override def create(boxId: Option[String], localReferenceNumber: String, consignorId: String, consigneeId: Option[String], administrativeReferenceCode: Option[String], lastUpdated: Instant, messages: Seq[Message]): Movement = {
      Movement(newId, boxId, localReferenceNumber, consignorId, consigneeId, administrativeReferenceCode, now, messages)
    }
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[ErnRetrievalRepository].toInstance(ernRetrievalRepository),
      bind[MessageConnector].toInstance(messageConnector),
      bind[DateTimeService].toInstance(dateTimeService),
      bind[MovementCreator].toInstance(TestMovementCreator),
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
          when(movementRepository.updateMovement(any)).thenReturn(Future.successful(Some(movement)))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(Seq.empty))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository, never()).getAllBy(any)
          verify(movementRepository, never()).updateMovement(any)
          verify(ernRetrievalRepository).save(eqTo(ern))
        }
      }
      "we try to retrieve messages and there are some" should {
        "add messages to only the movement with the right LRN" in {
          val ern = "testErn"
          val lrnMovement = Movement(None, "lrnie8158976912", "Consignor", None)
          val notLrnMovement = Movement(None, "notTheLrn", "consignor", None)
          val messages = Seq(IE704Message.createFromXml(IE704NoArc))
          val expectedMovement = lrnMovement.copy(messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now)))
          val unexpectedMovement = notLrnMovement.copy(messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now)))

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(lrnMovement, notLrnMovement)))
          when(movementRepository.updateMovement(any)).thenReturn(Future.successful(Some(expectedMovement)))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(messages))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository, never).updateMovement(eqTo(unexpectedMovement))
          verify(movementRepository).updateMovement(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
        }
        "add messages to only the movement with the right ARC" in {
          val ern = "testErn"
          val arcMovement = Movement(None, "notTheLrn", "Consignor", None, administrativeReferenceCode = Some("23XI00000000000000012"))
          val notArcMovement = Movement(None, "notTheLrn", "consignor", None)
          val messages = Seq(IE704Message.createFromXml(IE704))
          val expectedMovement = arcMovement.copy(messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now)))
          val unexpectedMovement = notArcMovement.copy(messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now)))

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(arcMovement, notArcMovement)))
          when(movementRepository.updateMovement(any)).thenReturn(Future.successful(Some(expectedMovement)))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(messages))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository, never).updateMovement(eqTo(unexpectedMovement))
          verify(movementRepository).updateMovement(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
        }
      }
    }
    "there is a movement that already has messages" when {
      "we try to retrieve new messages and there are some" should {
        "add the new messages to the movement" in {
          val ern = "testErn"
          val movement = Movement(None, "lrnie8158976912", "Consignor", None, messages = Seq(Message(utils.encode(IE801.toString()), "IE801", "token", now)))
          val messages = Seq(IE704Message.createFromXml(IE704NoArc))
          val expectedMovement = movement.copy(messages = movement.messages ++ Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now)))

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.updateMovement(any)).thenReturn(Future.successful(Some(expectedMovement)))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(messages))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository).updateMovement(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
        }
      }
    }
    "there is a no movement" when {
      "we try to retrieve new messages for an ERN and there are some" should {
        "a new movement should be created from an IE704 message" in {
          val ern = "testErn"
          val messages = Seq(IE704Message.createFromXml(IE704NoArc))
          val expectedMovement = TestMovementCreator.create(
            None,
            "lrnie8158976912",
            "testErn",
            None,
            None,
            messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now))
          )

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))
          when(movementRepository.saveMovement(any)).thenReturn(Future.successful(true))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(messageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(messages))

          messageService.updateMessages(ern).futureValue

          verify(messageConnector).getNewMessages(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository).saveMovement(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
        }
      }
    }
  }
}
