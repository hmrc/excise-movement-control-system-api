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

package uk.gov.hmrc.excisemovementcontrolsystemapi.service

import dispatch.Future
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageTypes, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementMessageService, ShowNewMessageParser}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext

class MovementMessageServiceSpec extends PlaySpec
  with BeforeAndAfterEach
  with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val messageParser = mock[ShowNewMessageParser]
  private val mockMovementMessageRepository = mock[MovementMessageRepository]

  private val movementMessageService = new MovementMessageService(messageParser, mockMovementMessageRepository)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMovementMessageRepository)
  }

  "saveMovementMessage" should {
    "return a MovementMessage" in {
      val successMovementMessage = Movement(lrn, consignorId, Some(consigneeId))
      when(mockMovementMessageRepository.save(any))
        .thenReturn(Future.successful(true))

      val result = await(movementMessageService.saveMovementMessage(successMovementMessage))

      result mustBe Right(successMovementMessage)
    }

    "throw an error" in {
      when(mockMovementMessageRepository.save(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementMessageService.saveMovementMessage(Movement(lrn, consignorId, Some(consigneeId))))

      result.left.value mustBe MongoError("error")
    }
  }

  "getMovementMessagesByLRNAndERNIn with valid LRN and ERN combination" should {
    "return  List of Messages" in {
      val messages = Seq(Message("123456", "IE801"), Message("ABCDE", "IE815"))
      val movementMessage = Movement(lrn, consignorId, Some(consigneeId), None, messages, Instant.now())
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(movementMessage)))


      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Right(messages)
    }

    "return empty Message when Movement is found with no Messages" in {
      val movementMessage = Movement(lrn, consignorId, Some(consigneeId), None, Seq.empty, Instant.now())
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(movementMessage)))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Right(Seq.empty)
    }

    "throw an error" in {
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result.left.value mustBe MongoError("error")
    }
  }

  "getMovementMessagesByLRNAndERNIn with no movement message for LRN and ERN combination" should {
    "return a NotFoundError" in {
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(None))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result.left.value mustBe NotFoundError()
    }
  }

  "updateMovement" should {

    val instant = Instant.now
    val message1 = Message("message1", MessageTypes.IE704.value)
    val message2 = Message("message2", MessageTypes.IE801.value)
    val message3 = Message("message3", MessageTypes.IE802.value)
    val message4 = Message("message4", MessageTypes.IE815.value)

    "decode message and get all messages - NEW" in {
      when(messageParser.parseEncodedMessage(any)).thenReturn(Seq(message1))
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(true))
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(Movement(lrn, consignorId, None, None, Seq.empty, instant))))

      await(movementMessageService.updateMovement(lrn, consignorId, "encode message"))

      verify(messageParser).parseEncodedMessage(eqTo("encode message"))
    }

    "get the movement from the DB" in {
      when(messageParser.parseEncodedMessage(any)).thenReturn(Seq(message1))
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(true))
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(Movement(lrn, consignorId, None, None, Seq.empty))))

      await(movementMessageService.updateMovement(lrn, consignorId, "any encoded message"))

      verify(mockMovementMessageRepository).get(lrn, List(consignorId))
    }

    "save the movement with all message" in {
      when(messageParser.parseEncodedMessage(any)).thenReturn(Seq(message1))
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(true))
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(Movement(lrn, consignorId, None, None, Seq.empty, instant))))

      val result = await(movementMessageService.updateMovement(lrn, consignorId, "any encode message"))

      val movement = Movement(lrn, consignorId, None, None, Seq(message1), instant)

      result mustBe true
      verify(mockMovementMessageRepository).save(eqTo(movement))
    }

    "do not save to DB when message is a duplicate" in {
      when(messageParser.parseEncodedMessage(any)).thenReturn(Seq(message1, message4))
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(true))
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(Movement(lrn, consignorId, None, None, Seq(message1, message2, message3), instant))))

      await(movementMessageService.updateMovement(lrn, consignorId, "Seq(message1, message4)"))

      val expectedMovement = Movement(lrn, consignorId, None, None, Seq(message1, message2, message3, message4), instant)
      verify(mockMovementMessageRepository).save(eqTo(expectedMovement))
    }

    "do not save to DB when message has different content but the same message type" in {
      when(messageParser.parseEncodedMessage(any)).thenReturn(Seq(message3, message4))
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(true))
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(Movement(lrn, consignorId, None, None, Seq(message1, message2), instant))))

      await(movementMessageService.updateMovement(lrn, consignorId, "Seq(message3, message4)"))

      val expectedMovement = Movement(lrn, consignorId, None, None, Seq(message1, message2, message3, message4), instant)
      verify(mockMovementMessageRepository).save(eqTo(expectedMovement))
    }

    "return false if did not save the message" in {
      when(messageParser.parseEncodedMessage(any)).thenReturn(Seq(message3, message4))
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(false))
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(Movement(lrn, consignorId, None, None, Seq.empty, instant))))

      val result = await(movementMessageService.updateMovement(lrn, consignorId, "Seq(message3, message4)"))

      result mustBe false
    }

    "return false if cannot retrieve message" in {
      when(mockMovementMessageRepository.get(any, any)).thenReturn(Future.successful(None))

      val result = await(movementMessageService.updateMovement(lrn, consignorId, "Seq(message3, message4)"))

      result mustBe false
    }
  }
}
