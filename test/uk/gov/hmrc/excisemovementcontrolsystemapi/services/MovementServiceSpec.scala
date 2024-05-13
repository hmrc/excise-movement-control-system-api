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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilterBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class MovementServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockMovementRepository = mock[MovementRepository]
  private val emcsUtils = new EmcsUtils
  private val testDateTime: Instant = Instant.parse("2023-11-15T17:02:34.123456Z")
  private val dateTimeService = mock[DateTimeService]
  when(dateTimeService.timestamp()).thenReturn(testDateTime)

  private val movementService = new MovementService(mockMovementRepository, emcsUtils, dateTimeService)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"
  private val newMessage = mock[IEMessage]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockMovementRepository, newMessage)
  }

  private val exampleMovement: Movement = Movement(Some("boxId"), lrn, consignorId, Some(consigneeId))

  "saveNewMovement" should {
    "return a Movement" in {
      val successMovement = exampleMovement
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      val result = await(movementService.saveNewMovement(successMovement))

      result mustBe Right(successMovement)
    }

    "throw an error when cannot save movement" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementService.saveNewMovement(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Database error", "error")
      result.left.value mustBe InternalServerError(Json.toJson(expectedError))
    }

    "throw an error when LRN is already in database for consignor with an ARC" in {
      val exampleMovementWithArc = exampleMovement.copy(administrativeReferenceCode = Some("arc"))

      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(exampleMovementWithArc)))

      val result = await(movementService.saveNewMovement(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Duplicate LRN error", "The local reference number 123 has already been used for another movement")
      result.left.value mustBe BadRequest(Json.toJson(expectedError))
    }

    "return an error if database fail on retrieving movement" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val result = await(movementService.saveNewMovement(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Database error", "Database error")
      result.left.value mustBe InternalServerError(Json.toJson(expectedError))
    }

    "throw an error when LRN is already in database with no ARC for same consignor but different consignee" in {
      val exampleMovementWithDifferentConsignee = exampleMovement.copy(consigneeId = Some("1234"))

      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(exampleMovementWithDifferentConsignee)))

      val result = await(movementService.saveNewMovement(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Duplicate LRN error", "The local reference number 123 has already been used for another movement")
      result.left.value mustBe BadRequest(Json.toJson(expectedError))
    }

    "return the database movement when LRN is already in database with no ARC for same consignor and same consignee" in {
      val movementInDB = exampleMovement.copy(lastUpdated = Instant.now)

      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movementInDB)))
      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      val result = await(movementService.saveNewMovement(exampleMovement))

      result mustBe Right(movementInDB)
    }

    "return the database movement when LRN is already in database for different consignor but same consignee" in {
      val movementInDB = exampleMovement.copy(lastUpdated = Instant.now, consignorId = "newConsignor")

      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movementInDB)))
      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      val result = await(movementService.saveNewMovement(exampleMovement))

      result mustBe Right(movementInDB)
    }

    "return the database movement when LRN is already in database as a consignee and it is submitted as a consignor" in {
      val movementInDB = exampleMovement.copy(lastUpdated = Instant.now, consignorId = consigneeId, consigneeId = Some(consignorId))

      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movementInDB)))
      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      val result = await(movementService.saveNewMovement(exampleMovement))

      result mustBe Right(movementInDB)
    }

  }

  "getMovementByLRNAndERNIn with valid LRN and ERN combination" should {
    "return  a movement" in {
      val message1 = Message("123456", "IE801", "messageId1", "ern", testDateTime)
      val message2 = Message("ABCDE", "IE815", "messageId2", "ern", testDateTime)
      val movement = Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), None, Instant.now(), Seq(message1, message2))
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movement)))

      val result = await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Some(movement)
    }

    "throw an error if multiple movement found" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(
          Movement(Some("boxId"), "lrn1", "consignorId1", None),
          Movement(Some("boxId"), "lrn2", "consignorId2", None)
        )))

      intercept[RuntimeException] {
        await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))
      }.getMessage mustBe s"[MovementService] - Multiple movement found for local reference number: $lrn"
    }
  }

  "getMovementByLRNAndERNIn with no movement message for LRN and ERN combination" should {
    "return no movement" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe None
    }
  }

  "getMovementByErn" should {

    val lrnToFilterBy = "lrn2"
    val ernToFilterBy = "ABC2"
    val arcToFilterBy = "arc2"

    "return all that movement for that ERN" in {
      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1)))

      val result = await(movementService.getMovementByErn(Seq(consignorId)))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter ERN" in {
      val consignorId2 = "ABC2"
      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(Some("boxId"), "lrn1", consignorId2, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilterBuilder().withErn(Some(consignorId)).build()
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter LRN" in {
      val lrnToFilterBy = "lrn2"
      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(Some("boxId"), lrnToFilterBy, consignorId, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilterBuilder().withLrn(Some(lrnToFilterBy)).build()
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }


    "return only the movements that correspond to the filter ARC" in {
      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc2"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilterBuilder().withArc(Some(arcToFilterBy)).build()
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }

    "return only the movements that correspond to the filter LRN and ern" in {
      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(Some("boxId"), lrnToFilterBy, consignorId, None, Some("arc1"))
      val expectedMovement3 = Movement(Some("boxId"), "lrn1", ernToFilterBy, None, Some("arc1"))
      val expectedMovement4 = Movement(Some("boxId"), lrnToFilterBy, ernToFilterBy, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2, expectedMovement3, expectedMovement4)))

      val filter = MovementFilterBuilder().withErn(Some(ernToFilterBy)).withLrn(Some(lrnToFilterBy)).build()
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement4)
    }

    "return only the movements that correspond to the filter LRN, ern and arc" in {

      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(Some("boxId"), lrnToFilterBy, consignorId, None, Some("arc1"))
      val expectedMovement3 = Movement(Some("boxId"), "lrn1", ernToFilterBy, None, Some("arc1"))
      val expectedMovement4 = Movement(Some("boxId"), lrnToFilterBy, ernToFilterBy, None, Some(arcToFilterBy))
      val expectedMovement5 = Movement(Some("boxId"), "lrn1", consignorId, None, Some(arcToFilterBy))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2, expectedMovement3, expectedMovement4, expectedMovement5)))

      val filter = MovementFilterBuilder()
        .withErn(Some(ernToFilterBy))
        .withLrn(Some(lrnToFilterBy))
        .withArc(Some(arcToFilterBy))
        .build()
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement4)
    }

    "return an empty list" in {
      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementService.getMovementByErn(Seq(consignorId)))

      result mustBe Seq.empty
    }
  }

  "getMovementById" should {

    "return the movement for the id if it is found" in {
      val expectedMovement1 = Movement("uuid1", Some("boxId"), "lrn1", consignorId, None, Some("arc1"), Instant.now, Seq.empty)

      when(mockMovementRepository.getMovementById("uuid1"))
        .thenReturn(Future.successful(Some(expectedMovement1)))

      val result = await(movementService.getMovementById("uuid1"))

      result mustBe Some(expectedMovement1)
    }

    "return None if no match" in {

      when(mockMovementRepository.getMovementById("uuid2"))
        .thenReturn(Future.successful(None))

      val result = await(movementService.getMovementById("uuid2"))

      result mustBe None
    }
  }

  "updateMovement" should {

    val messageIdForNewMessage = "messageId3"
    val cachedMessage1 = Message("<IE801>test</IE801>", MessageTypes.IE801.value, "messageId1", "ern", testDateTime)
    val cachedMessage2 = Message("<IE802>test</IE802>", MessageTypes.IE802.value, "messageId2", "ern", testDateTime)

    val movementARC456 = Movement(Some("boxId"), "123", consignorId, None, Some("456"), testDateTime, Seq.empty)
    val movementARC89 = Movement(Some("boxId"), "345", consignorId, None, Some("89"), testDateTime, Seq.empty)
    val movementARC890 = Movement(Some("boxId"), "345", "12", None, Some("890"), testDateTime, Seq.empty)
    val movementNoArcLrn456 = Movement(Some("boxId"), "456", consignorId, None, None, testDateTime, Seq.empty)
    val movementNoArcLrn789 = Movement(Some("boxId"), "789", consignorId, None, None, testDateTime, Seq.empty)

    val cachedMovements = Seq(movementARC456, movementARC89, movementARC890, movementNoArcLrn456, movementNoArcLrn789)
    val encodeMessage = Base64.getEncoder.encodeToString("<IE818>test</IE818>".getBytes(StandardCharsets.UTF_8))
    val expectedMessage = Message(encodeMessage, MessageTypes.IE818.value, messageIdForNewMessage, consignorId, testDateTime)

    "save movement" when {
      "message contains Administration Reference Code (ARC)" in {
        val updatedMovement = movementARC456.copy(messages = Seq(expectedMessage))
        setUpForUpdateMovement(
          newMessage,
          Seq(Some("456")),
          None,
          "<IE818>test</IE818>",
          cachedMovements,
          messageIdForNewMessage,
          Some(updatedMovement)
        )

        val result = await(movementService.updateMovement(newMessage, consignorId))

        result mustBe Seq(updatedMovement)
        verify(mockMovementRepository).updateMovement(eqTo(updatedMovement))
      }

      "message contains Both Administration Reference Code (ARC) and Local Ref Number (LRN)" in {
        val updateMovement = movementARC456.copy(messages = Seq(expectedMessage))
        setUpForUpdateMovement(
          newMessage,
          Seq(Some("456")),
          Some("123"), "<IE818>test</IE818>",
          cachedMovements,
          messageIdForNewMessage,
          Some(updateMovement)
        )

        await(movementService.updateMovement(newMessage, consignorId))

        verify(mockMovementRepository).updateMovement(eqTo(updateMovement))
      }

      "message contains Local Ref Number (LRN)" in {
        val updatesMovement = movementARC89.copy(messages = Seq(expectedMessage))
        setUpForUpdateMovement(
          newMessage,
          Seq(None),
          Some("345"),
          "<IE818>test</IE818>",
          cachedMovements,
          messageIdForNewMessage,
          Some(updatesMovement)
        )

        await(movementService.updateMovement(newMessage, consignorId))

        verify(mockMovementRepository).updateMovement(eqTo(updatesMovement))
      }
    }

    "retain old messages when adding a new one to a movement" in {
      val cachedMovement = movementARC456.copy(messages = Seq(cachedMessage1, cachedMessage2))
      val updatedMovement = cachedMovement.copy(messages = Seq(cachedMessage1, cachedMessage2, expectedMessage))
      setUpForUpdateMovement(
        newMessage,
        Seq(Some("456")),
        None,
        "<IE818>test</IE818>",
        cachedMovements,
        messageIdForNewMessage,
        Some(updatedMovement)
      )

      when(newMessage.toXml).thenReturn(scala.xml.XML.loadString("<IE818>test</IE818>"))
      when(newMessage.messageType).thenReturn(MessageTypes.IE818.value)
      when(mockMovementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(cachedMovement)))
      await(movementService.updateMovement(newMessage, consignorId))

      verify(mockMovementRepository).updateMovement(eqTo(updatedMovement))
    }

    "not overwrite ARC that are not empty" in {
      val updatedMovement = movementARC456.copy(messages = Seq(expectedMessage))
      setUpForUpdateMovement(
        newMessage,
        Seq(Some("arc")),
        Some("123"),
        "<IE818>test</IE818>",
        cachedMovements,
        messageIdForNewMessage,
        Some(updatedMovement)
      )

      await(movementService.updateMovement(newMessage, consignorId))

      verify(mockMovementRepository).updateMovement(eqTo(updatedMovement))
    }

    "message contains multiple Administration Reference Codes (ARCs)" in {
      val updateMovement1 = movementARC456.copy(messages = Seq(expectedMessage))
      val updateMovement2 = movementARC890.copy(messages = Seq(expectedMessage))
      val updateMovement3 = movementARC89.copy(messages = Seq(expectedMessage))
      setUpForUpdateMovement(newMessage, Seq(Some("456"), Some("890"), Some("89")), None, "<IE818>test</IE818>", cachedMovements, messageIdForNewMessage)
      when(mockMovementRepository.updateMovement(any))
        .thenReturn(
          Future.successful(Some(updateMovement1)),
          Future.successful(None),
          Future.successful(Some(updateMovement3))
        )
      val result = await(movementService.updateMovement(newMessage, consignorId))

      result mustBe Seq(updateMovement1, updateMovement3)
      verify(mockMovementRepository).updateMovement(eqTo(updateMovement1))
      verify(mockMovementRepository).updateMovement(eqTo(updateMovement2))
      verify(mockMovementRepository).updateMovement(eqTo(updateMovement3))
    }

    "movement updated matching on LRN when message ARC is empty" in {
      val updateMovement1 = movementNoArcLrn456.copy(messages = Seq(expectedMessage))
      val updateMovement2 = movementNoArcLrn789.copy(messages = Seq(expectedMessage))
      setUpForUpdateMovement(newMessage, Seq(None), Some("789"), "<IE818>test</IE818>", cachedMovements, messageIdForNewMessage)
      when(mockMovementRepository.updateMovement(eqTo(updateMovement1)))
        .thenReturn(Future.successful(Some(updateMovement1)))
      when(mockMovementRepository.updateMovement(eqTo(updateMovement2)))
        .thenReturn(Future.successful(Some(updateMovement2)))
      val result = await(movementService.updateMovement(newMessage, consignorId))

      result mustBe Seq(updateMovement2)
      verify(mockMovementRepository, times(0)).updateMovement(eqTo(updateMovement1))
      verify(mockMovementRepository).updateMovement(eqTo(updateMovement2))
    }


    "throw an error" when {
      "message has both ARC and LRN missing" in {
        setUpForUpdateMovement(newMessage, Seq(None), None, "<foo>test</foo>", cachedMovements, messageIdForNewMessage)

        when(newMessage.toString).thenReturn("message type: Mocked Message")

        intercept[RuntimeException] {
          await(movementService.updateMovement(newMessage, consignorId))
        }.getMessage mustBe "[MovementService] - Cannot find movement for ERN: ABC, message type: Mocked Message"
      }

      "movement is not present" in {
        when(mockMovementRepository.getAllBy(any)).thenReturn(Future.successful(Seq.empty))

        intercept[RuntimeException] {
          await(movementService.updateMovement(newMessage, consignorId))
        }
      }
    }

    "do not save duplicate messages to DB" in {
      val cachedMessage = createMessage("<foo>test</foo>", MessageTypes.IE801.value, messageIdForNewMessage)

      val movementWithMessagesAlready = Movement(Some("boxId"), lrn, consignorId, None, None, testDateTime, Seq(cachedMessage, cachedMessage1, cachedMessage2))

      setUpForUpdateMovement(newMessage, Seq(None), Some("123"), "<foo>test</foo>", cachedMovements, messageIdForNewMessage)
      when(mockMovementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movementWithMessagesAlready)))

      val result = await(movementService.updateMovement(newMessage, consignorId))

      result mustBe Seq.empty
      verify(mockMovementRepository, times(0)).updateMovement(any)
    }

    "save to DB when message has different content but the same message type" in {
      val cachedMessage: Message = createMessage("<foo>different content</foo>", MessageTypes.IE801.value, messageIdForNewMessage, consignorId)
      val movementWithMessagesAlready = Movement(Some("boxId"), lrn, consignorId, None, None, testDateTime, Seq(cachedMessage, cachedMessage1))

      setUpForUpdateMovement(newMessage, Seq(None), Some("123"), "<foo>test</foo>", cachedMovements, messageIdForNewMessage)
      when(mockMovementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movementWithMessagesAlready)))

      await(movementService.updateMovement(newMessage, consignorId))

      val expectedNewMessage = createMessage("<foo>test</foo>", MessageTypes.IE818.value, messageIdForNewMessage, consignorId)
      val expectedMovement = movementWithMessagesAlready.copy(messages = Seq(cachedMessage, cachedMessage1, expectedNewMessage))
      verify(mockMovementRepository).updateMovement(eqTo(expectedMovement))
    }

    "return an empty sequence if did not save the message" in {
      setUpForUpdateMovement(newMessage, Seq(None), Some("123"), "<foo>test</foo>", cachedMovements, messageIdForNewMessage)
      when(mockMovementRepository.updateMovement(any))
        .thenReturn(Future.successful(None))

      val result = await(movementService.updateMovement(newMessage, consignorId))

      result mustBe Seq.empty
    }
  }

  private def setUpForUpdateMovement
  (
    message: IEMessage,
    arc: Seq[Option[String]],
    lrn: Option[String],
    messageXml: String,
    cachedMovements: Seq[Movement],
    messageIDForNewMessage: String,
    updatedMovement: Option[Movement] = None
  ): Unit = {
    when(message.administrativeReferenceCode).thenReturn(arc)
    when(message.lrnEquals(eqTo(lrn.getOrElse("")))).thenReturn(lrn.isDefined)
    when(message.toXml).thenReturn(scala.xml.XML.loadString(messageXml))
    when(message.messageType).thenReturn(MessageTypes.IE818.value)
    when(message.messageIdentifier).thenReturn(messageIDForNewMessage)
    when(mockMovementRepository.getAllBy(any))
      .thenReturn(Future.successful(cachedMovements))
    when(mockMovementRepository.updateMovement(any))
      .thenReturn(Future.successful(updatedMovement))
  }

  private def createMessage(xml: String, messageType: String, messageIdForNewMessage: String, recipient: String = "ern") = {
    val encodeMessage = Base64.getEncoder.encodeToString(xml.getBytes(StandardCharsets.UTF_8))
    Message(encodeMessage, messageType, messageIdForNewMessage, recipient, testDateTime)
  }
}
