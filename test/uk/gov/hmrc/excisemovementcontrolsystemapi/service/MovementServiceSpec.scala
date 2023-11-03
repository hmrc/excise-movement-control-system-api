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
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.GeneralMongoError
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext

class MovementServiceSpec extends PlaySpec with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockMovementMessageRepository = mock[MovementRepository]

  private val movementMessageService = new MovementService(mockMovementMessageRepository)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"

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
      val message1 = Message("123456", "IE801")
      val message2 = Message("ABCDE", "IE815")
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
      }.getMessage mustBe s"Multiple movement found for local reference number: $lrn"


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
      }.getMessage mustBe s"Multiple movement found for local reference number: $lrn"
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
}
