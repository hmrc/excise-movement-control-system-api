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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EisUtils, MessageTypes, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{DateTimeService, MovementMessageService}
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import scala.concurrent.ExecutionContext

class MovementMessageServiceSpec extends PlaySpec
  with BeforeAndAfterEach
  with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val eisUtils = mock[EisUtils]
  private val dateTimeService = mock[DateTimeService]
  private val mockMovementMessageRepository = mock[MovementMessageRepository]

  private val movementMessageService = new MovementMessageService(
    mockMovementMessageRepository,
    eisUtils,
    dateTimeService
  )

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"
  private val now = Instant.parse("2018-11-30T18:35:24.00Z")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMovementMessageRepository, dateTimeService, eisUtils)
    when(dateTimeService.now).thenReturn(now)
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
      val messages = Seq(Message("123456", "IE801", dateTimeService), Message("ABCDE", "IE815", dateTimeService))
      val movementMessage = Movement(lrn, consignorId, Some(consigneeId), None, messages)
      when(mockMovementMessageRepository.get(any, any))
        .thenReturn(Future.successful(Some(movementMessage)))


      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Right(messages)
    }

    "return empty Message when Movement is found with no Messages" in {
      val movementMessage = Movement(lrn, consignorId, Some(consigneeId), None, Seq.empty)
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

    val newMessage = mock[IEMessage]
    val cachedMessage1 = Message("<IE801>test</IE801>", MessageTypes.IE801.value, dateTimeService)
    val cachedMessage2 = Message("<IE802>test</IE802>", MessageTypes.IE802.value, dateTimeService)

    val cachedMovements = Seq(
      Movement("123", consignorId, None, Some("456"), Seq.empty, now),
      Movement("345", consignorId, None, Some("89"), Seq.empty, now),
      Movement("345", "12", None, Some("890"), Seq.empty, now)
    )

    "save movement" when {
      "message contains Administration Reference Code (ARC)" in {
        setUpForUpdateMovemen1(newMessage, Some("456"), None, "<IE818>test</IE818>", cachedMovements)

        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).save(
          eqTo(Movement("123", consignorId, None, Some("456"), Seq(expectedMessage), now)))
    }

      "message contains Both Administration Reference Code (ARC) and Local Ref Number (LRN)" in {
        setUpForUpdateMovemen1(newMessage, Some("456"), Some("123"), "<IE818>test</IE818>", cachedMovements)

        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).save(
          eqTo(Movement("123", consignorId, None, Some("456"), Seq(expectedMessage), now)))
      }

      "message contains Local Ref Number (LRN)" in {
        setUpForUpdateMovemen1(newMessage, None, Some("345"), "<IE818>test</IE818>", cachedMovements)

        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).save(
          eqTo(Movement("345", consignorId, None, Some("89"), Seq(expectedMessage), now)))
      }

      "test message" in {
        setUpForUpdateMovemen1(newMessage, Some("456"), None, "<IE818>test</IE818>", cachedMovements)

        when(newMessage.toXml).thenReturn(scala.xml.XML.loadString("<IE818>test</IE818>"))
        when(newMessage.getType).thenReturn(MessageTypes.IE818.value)
        val movement = Seq(
          Movement("123", consignorId, None, Some("456"), Seq(cachedMessage1, cachedMessage2), now),
        )
        when(mockMovementMessageRepository.getAllBy(any))
          .thenReturn(Future.successful(movement))
        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).save(
          eqTo(Movement("123", consignorId, None, Some("456"), Seq(cachedMessage1, cachedMessage2, expectedMessage), now)))

      }
    }

    "throw an error is message has both ARC and LRN missing" in {
      setUpForUpdateMovemen1(newMessage, None, None, "<foo>test</foo>", cachedMovements)

      intercept[RuntimeException] {
        await(movementMessageService.updateMovement(newMessage, consignorId))
      }.getMessage mustBe "Cannot retrieve a movement. Local reference number or administration reference code are not present"
    }

    "return false if cannot retrieve message1" in {
      when(mockMovementMessageRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))

      intercept[RuntimeException] {
        await(movementMessageService.updateMovement(newMessage, consignorId))
      }.getMessage mustBe "Cannot retrieve a movement. Local reference number or administration reference code are not present"
    }


    "do not save duplicate messages to DB" in {
      val cachedMessage = createMessage("<foo>test</foo>",MessageTypes.IE801.value)

      setUpForUpdateMovemen1(newMessage, None, Some("123"), "<foo>test</foo>", cachedMovements)
      when(mockMovementMessageRepository.getAllBy(any))
        .thenReturn(Future.successful(Seq(Movement(lrn, consignorId, None, None, Seq(cachedMessage, cachedMessage1, cachedMessage2), now))))

      await(movementMessageService.updateMovement(newMessage, consignorId))

      val expectedMovement = Movement(lrn, consignorId, None, None, Seq(cachedMessage, cachedMessage1, cachedMessage2), now)
      verify(mockMovementMessageRepository).save(eqTo(expectedMovement))
    }

    "save to DB when message has different content but the same message type" in {
      val cachedMessage: Message = createMessage("<foo>different content</foo>", MessageTypes.IE801.value)
      setUpForUpdateMovemen1(newMessage, None, Some("123"), "<foo>test</foo>", cachedMovements)
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(true))
      when(mockMovementMessageRepository.getAllBy(any))
        .thenReturn(Future.successful(Seq(Movement(lrn, consignorId, None, None, Seq(cachedMessage, cachedMessage1), now))))

      await(movementMessageService.updateMovement(newMessage, consignorId))

      val expectedNewMessage = createMessage("<foo>test</foo>", MessageTypes.IE818.value)
      verify(mockMovementMessageRepository).save(
        eqTo(Movement(lrn, consignorId, None, None, Seq(cachedMessage, cachedMessage1, expectedNewMessage), now))
      )
    }

    "return false if did not save the message" in {
      setUpForUpdateMovemen1(newMessage, None, Some("123"), "<foo>test</foo>", cachedMovements)
      when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(false))

      val result = await(movementMessageService.updateMovement(newMessage, consignorId))

      result mustBe false
    }
  }

  "getUniqueConsignorId" should {
    "return a list of excise numbers" in {
      val movement1 = Movement("lrn1", "consignorId1", Some("consigneeId1"), None, Seq.empty, now)
      val movement2 = Movement("lrn2", "consignorId2", Some("consigneeId2"), None, Seq.empty, now)
      val movement3 = Movement("lrn3", "consignorId3", Some("consigneeId3"), None, Seq.empty, now)
      val movement4 = Movement("lrn4", "consignorId1", Some("consigneeId2"), None, Seq.empty, now)
      val movement5 = Movement("lrn5", "consignorId2", Some("consigneeId2"), None, Seq.empty, now)
      when(mockMovementMessageRepository.getMovements)
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3, movement4, movement5)))

      val result = await(movementMessageService.getUniqueConsignorId)

      result mustBe Seq(movement1, movement2, movement3)

    }
  }

  private def createMessage(xml: String, messageType: String) = {
    val encodeMessage = Base64.getEncoder.encodeToString(xml.getBytes(StandardCharsets.UTF_8))
    Message(encodeMessage, messageType, dateTimeService)
  }

  private def setUpForUpdateMovemen1
  (
    message: IEMessage,
    arc: Option[String],
    lrn: Option[String],
    messageXml: String,
    cachedMovements: Seq[Movement]
  ): Unit = {
    when(eisUtils.createEncoder).thenReturn(Base64.getEncoder)
    when(message.administrativeRefCode).thenReturn(arc)
    when(message.localReferenceNumber).thenReturn(lrn)
    when(message.toXml).thenReturn(scala.xml.XML.loadString(messageXml))
    when(message.getType).thenReturn(MessageTypes.IE818.value)
    when(mockMovementMessageRepository.getAllBy(any))
      .thenReturn(Future.successful(cachedMovements))
    when(mockMovementMessageRepository.save(any)).thenReturn(Future.successful(true))
  }
}
