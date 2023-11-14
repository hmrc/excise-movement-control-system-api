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
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, GeneralMongoError, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import scala.concurrent.ExecutionContext

class MovementServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockMovementMessageRepository = mock[MovementRepository]
  private val dateTimeService = mock[DateTimeService]

  private val movementMessageService = new MovementService(mockMovementMessageRepository, new EmcsUtils, dateTimeService)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"
  private val now = Instant.parse("2018-11-30T18:35:24.00Z")
  private val newMessage = mock[IEMessage]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockMovementMessageRepository, newMessage)
  }


  "saveMovementMessage" should {
    "return a MovementMessage" in {
      val successMovementMessage = Movement(lrn, consignorId, Some(consigneeId))
      when(mockMovementMessageRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      val result = await(movementMessageService.saveMovementMessage(successMovementMessage))

      result mustBe Right(successMovementMessage)
    }

    "throw an error" in {
      when(mockMovementMessageRepository.saveMovement(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementMessageService.saveMovementMessage(Movement(lrn, consignorId, Some(consigneeId))))

      result.left.value mustBe GeneralMongoError("error")
    }
  }

  "getMovementMessagesByLRNAndERNIn with valid LRN and ERN combination" should {
    "return  a movement" in {
      val message1 = Message("123456", "IE801", dateTimeService)
      val message2 = Message("ABCDE", "IE815", dateTimeService)
      val movement = Movement(lrn, consignorId, Some(consigneeId), None, Instant.now(), Seq(message1, message2))
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movement)))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Some(movement)
    }

    "throw an error if multiple movement found" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(
          Movement("lrn1", "consignorId1", None),
          Movement("lrn2", "consignorId2", None)
        )))

      intercept[RuntimeException] {
        await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))
      }.getMessage mustBe s"[MovementService] - Multiple movement found for local reference number: $lrn"


    }
  }

  "getMovementMessagesByLRNAndERNIn with no movement message for LRN and ERN combination" should {
    "return no movement" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe None
    }
  }

  "getMatchingERN" should {

    "return None if no movement found" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementMessageService.getMatchingERN(lrn, List(consignorId)))

      result mustBe None

    }

    "return an ERN for the movement found" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(Movement(lrn, consignorId, Some(consigneeId)))))

      val result = await(movementMessageService.getMatchingERN(lrn, List(consignorId)))

      result mustBe Some(consignorId)
    }

    "return an ERN for the movement for a consigneeId match" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(Movement(lrn, consignorId, Some(consigneeId)))))

      val result = await(movementMessageService.getMatchingERN(lrn, List(consigneeId)))

      result mustBe Some(consigneeId)
    }

    "throw an exception if more then one movement found" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(
          Movement(lrn, consignorId, Some(consigneeId)),
          Movement(lrn, consignorId, Some(consigneeId))
        )))

      intercept[RuntimeException] {
        await(movementMessageService.getMatchingERN(lrn, List(consignorId)))
      }.getMessage mustBe s"[MovementService] - Multiple movements found for local reference number: $lrn"
    }
  }

  "getMovementByErn" should {

    val lrnToFilterBy = "lrn2"
    val ernToFilterBy = "ABC2"
    val arcToFilterBy = "arc2"

    "return all that movement for that ERN" in {
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))

      when(mockMovementMessageRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1)))

      val result = await(movementMessageService.getMovementByErn(Seq(consignorId)))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter ERN" in {
      val consignorId2 = "ABC2"
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement("lrn1", consignorId2, None, Some("arc1"))

      when(mockMovementMessageRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilter.and(Seq("ern" -> Some(consignorId)))
      val result = await(movementMessageService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter LRN" in {
      val lrnToFilterBy = "lrn2"
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(lrnToFilterBy, consignorId, None, Some("arc1"))

      when(mockMovementMessageRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilter.and(Seq("lrn" -> Some(lrnToFilterBy)))
      val result = await(movementMessageService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }


    "return only the movements that correspond to the filter ARC" in {
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement("lrn1", consignorId, None, Some("arc2"))

      when(mockMovementMessageRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilter.and(Seq("arc" -> Some(arcToFilterBy)))
      val result = await(movementMessageService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }

    "return only the movements that correspond to the filter LRN and ern" in {
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(lrnToFilterBy, consignorId, None, Some("arc1"))
      val expectedMovement3 = Movement("lrn1", ernToFilterBy, None, Some("arc1"))
      val expectedMovement4 = Movement(lrnToFilterBy, ernToFilterBy, None, Some("arc1"))

      when(mockMovementMessageRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2, expectedMovement3, expectedMovement4)))

      val filter = MovementFilter.and(Seq("ern" -> Some(ernToFilterBy), "lrn" -> Some(lrnToFilterBy)))
      val result = await(movementMessageService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement4)
    }

    "return only the movements that correspond to the filter LRN, ern and arc" in {

      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(lrnToFilterBy, consignorId, None, Some("arc1"))
      val expectedMovement3 = Movement("lrn1", ernToFilterBy, None, Some("arc1"))
      val expectedMovement4 = Movement(lrnToFilterBy, ernToFilterBy, None, Some(arcToFilterBy))
      val expectedMovement5 = Movement("lrn1", consignorId, None, Some(arcToFilterBy))

      when(mockMovementMessageRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2, expectedMovement3, expectedMovement4, expectedMovement5)))

      val filter = MovementFilter.and(Seq(
        "ern" -> Some(ernToFilterBy),
        "lrn" -> Some(lrnToFilterBy),
        "arc" -> Some(arcToFilterBy)
      ))
      val result = await(movementMessageService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement4)
    }

    "return an empty list" in {
      when(mockMovementMessageRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementMessageService.getMovementByErn(Seq(consignorId)))

      result mustBe Seq.empty
    }
  }

  "updateMovement" should {

    val cachedMessage1 = Message("<IE801>test</IE801>", MessageTypes.IE801.value, dateTimeService)
    val cachedMessage2 = Message("<IE802>test</IE802>", MessageTypes.IE802.value, dateTimeService)

    val cachedMovements = Seq(
      Movement("123", consignorId, None, Some("456"), now, Seq.empty),
      Movement("345", consignorId, None, Some("89"), now, Seq.empty),
      Movement("345", "12", None, Some("890"), now, Seq.empty)
    )

    "save movement" when {
      "message contains Administration Reference Code (ARC)" in {
        setUpForUpdateMovement(newMessage, Some("456"), None, "<IE818>test</IE818>", cachedMovements)

        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).updateMovement(
          eqTo(Movement("123", consignorId, None, Some("456"), now, Seq(expectedMessage))))
      }

      "message contains Both Administration Reference Code (ARC) and Local Ref Number (LRN)" in {
        setUpForUpdateMovement(newMessage, Some("456"), Some("123"), "<IE818>test</IE818>", cachedMovements)

        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).updateMovement(
          eqTo(Movement("123", consignorId, None, Some("456"), now, Seq(expectedMessage))))
      }

      "message contains Local Ref Number (LRN)" in {
        setUpForUpdateMovement(newMessage, None, Some("345"), "<IE818>test</IE818>", cachedMovements)

        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).updateMovement(
          eqTo(Movement("345", consignorId, None, Some("89"), now, Seq(expectedMessage))))
      }

      "test message" in {
        setUpForUpdateMovement(newMessage, Some("456"), None, "<IE818>test</IE818>", cachedMovements)

        when(newMessage.toXml).thenReturn(scala.xml.XML.loadString("<IE818>test</IE818>"))
        when(newMessage.messageType).thenReturn(MessageTypes.IE818.value)
        val movement = Seq(
          Movement("123", consignorId, None, Some("456"), now, Seq(cachedMessage1, cachedMessage2)),
        )
        when(mockMovementMessageRepository.getAllBy(any))
          .thenReturn(Future.successful(movement))
        await(movementMessageService.updateMovement(newMessage, consignorId))

        val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
        val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, dateTimeService)

        verify(mockMovementMessageRepository).updateMovement(
          eqTo(Movement("123", consignorId, None, Some("456"), now, Seq(cachedMessage1, cachedMessage2, expectedMessage))))

      }
    }

    "throw an error is message has both ARC and LRN missing" in {
      setUpForUpdateMovement(newMessage, None, None, "<foo>test</foo>", cachedMovements)

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

      setUpForUpdateMovement(newMessage, None, Some("123"), "<foo>test</foo>", cachedMovements)
      when(mockMovementMessageRepository.getAllBy(any))
        .thenReturn(Future.successful(Seq(Movement(lrn, consignorId, None, None, now, Seq(cachedMessage, cachedMessage1, cachedMessage2)))))

      await(movementMessageService.updateMovement(newMessage, consignorId))

      val expectedMovement = Movement(lrn, consignorId, None, None, now, Seq(cachedMessage, cachedMessage1, cachedMessage2))
      verify(mockMovementMessageRepository).updateMovement(eqTo(expectedMovement))
    }

    "save to DB when message has different content but the same message type" in {
      val cachedMessage: Message = createMessage("<foo>different content</foo>", MessageTypes.IE801.value)
      setUpForUpdateMovement(newMessage, None, Some("123"), "<foo>test</foo>", cachedMovements)
      when(mockMovementMessageRepository.updateMovement(any)).thenReturn(Future.successful(true))
      when(mockMovementMessageRepository.getAllBy(any))
        .thenReturn(Future.successful(Seq(Movement(lrn, consignorId, None, None, now, Seq(cachedMessage, cachedMessage1)))))

      await(movementMessageService.updateMovement(newMessage, consignorId))

      val expectedNewMessage = createMessage("<foo>test</foo>", MessageTypes.IE818.value)
      verify(mockMovementMessageRepository).updateMovement(
        eqTo(Movement(lrn, consignorId, None, None, now, Seq(cachedMessage, cachedMessage1, expectedNewMessage)))
      )
    }

    "return false if did not save the message" in {
      setUpForUpdateMovement(newMessage, None, Some("123"), "<foo>test</foo>", cachedMovements)
      when(mockMovementMessageRepository.updateMovement(any)).thenReturn(Future.successful(false))

      val result = await(movementMessageService.updateMovement(newMessage, consignorId))

      result mustBe false
    }
  }

  private def setUpForUpdateMovement
  (
    message: IEMessage,
    arc: Option[String],
    lrn: Option[String],
    messageXml: String,
    cachedMovements: Seq[Movement]
  ): Unit = {
    when(message.administrativeReferenceCode).thenReturn(arc)
    when(message.lrnEquals(eqTo(lrn.getOrElse("")))).thenReturn(lrn.isDefined)
    when(message.toXml).thenReturn(scala.xml.XML.loadString(messageXml))
    when(message.messageType).thenReturn(MessageTypes.IE818.value)
    when(mockMovementMessageRepository.getAllBy(any))
      .thenReturn(Future.successful(cachedMovements))
    when(mockMovementMessageRepository.updateMovement(any)).thenReturn(Future.successful(true))
  }

  private def createMessage(xml: String, messageType: String) = {
    val encodeMessage = Base64.getEncoder.encodeToString(xml.getBytes(StandardCharsets.UTF_8))
    Message(encodeMessage, messageType, dateTimeService)
  }
}
