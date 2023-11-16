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
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDateTime}
import scala.concurrent.ExecutionContext

class MovementServiceSpec extends PlaySpec with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockMovementRepository = mock[MovementRepository]
  private val emcsUtils = mock[EmcsUtils]
  private val testDateTime: LocalDateTime = LocalDateTime.of(2023, 11, 15, 17, 2, 34)
  when(emcsUtils.getCurrentDateTime).thenReturn(testDateTime)

  private val movementService = new MovementService(mockMovementRepository, emcsUtils)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"

  private val exampleMovement: Movement = Movement(lrn, consignorId, Some(consigneeId))

  "saveMovement" should {
    "return a Movement" in {
      val successMovement = exampleMovement
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      val result = await(movementService.saveNewMovementMessage(successMovement))

      result mustBe Right(successMovement)
    }

    "throw an error when database throws a runtime exception" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementService.saveNewMovementMessage(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Database error", "Error occurred while saving movement message")

      result.left.value mustBe InternalServerError(Json.toJson(expectedError))
    }

    "throw an error when LRN is already in database with an ARC" in {
      val exampleMovementWithArc = exampleMovement.copy(administrativeReferenceCode =  Some("arc"))

      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(exampleMovementWithArc)))

      val result = await(movementService.saveNewMovementMessage(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Duplicate LRN error", "The local reference number 123 has already been used for another movement")

      result.left.value mustBe BadRequest(Json.toJson(expectedError))
    }

    "throw an error when LRN is already in database with no ARC for different consignee" in {
      val exampleMovementWithDifferentConsignee = exampleMovement.copy(consigneeId = Some("1234"))

      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(exampleMovementWithDifferentConsignee)))

      val result = await(movementService.saveNewMovementMessage(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Duplicate LRN error", "The local reference number 123 has already been used for another movement")

      result.left.value mustBe BadRequest(Json.toJson(expectedError))
    }

    "return a Movement when LRN is already in database with no ARC for same consignee" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(exampleMovement)))

      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      val result = await(movementService.saveNewMovementMessage(exampleMovement))

      result mustBe Right(exampleMovement)
    }

  }

  "getMovementMessagesByLRNAndERNIn with valid LRN and ERN combination" should {
    "return  a movement" in {
      val message1 = Message("123456", "IE801")
      val message2 = Message("ABCDE", "IE815")
      val movement = Movement(lrn, consignorId, Some(consigneeId), None, Instant.now(), Seq(message1, message2))
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movement)))

      val result = await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Some(movement)
    }

    "throw an error if multiple movement found" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(
          Movement("lrn1", "consignorId1", None),
          Movement("lrn2", "consignorId2", None)
        )))

      intercept[RuntimeException] {
        await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))
      }.getMessage mustBe s"[MovementService] - Multiple movement found for local reference number: $lrn"


    }
  }

  "getMovementMessagesByLRNAndERNIn with no movement message for LRN and ERN combination" should {
    "return no movement" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe None
    }
  }

  "getMatchingERN" should {

    "return None if no movement found" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementService.getMatchingERN(lrn, List(consignorId)))

      result mustBe None

    }

    "return an ERN for the movement found" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(exampleMovement)))

      val result = await(movementService.getMatchingERN(lrn, List(consignorId)))

      result mustBe Some(consignorId)
    }

    "return an ERN for the movement for a consigneeId match" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(exampleMovement)))

      val result = await(movementService.getMatchingERN(lrn, List(consigneeId)))

      result mustBe Some(consigneeId)
    }

    "throw an exception if more then one movement found" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(
          exampleMovement,
          exampleMovement
        )))

      intercept[RuntimeException] {
        await(movementService.getMatchingERN(lrn, List(consignorId)))
      }.getMessage mustBe s"[MovementService] - Multiple movements found for local reference number: $lrn"
    }
  }

  "getMovementByErn" should {

    val lrnToFilterBy = "lrn2"
    val ernToFilterBy = "ABC2"
    val arcToFilterBy = "arc2"

    "return all that movement for that ERN" in {
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1)))

      val result = await(movementService.getMovementByErn(Seq(consignorId)))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter ERN" in {
      val consignorId2 = "ABC2"
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement("lrn1", consignorId2, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilter.and(Seq("ern" -> Some(consignorId)))
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter LRN" in {
      val lrnToFilterBy = "lrn2"
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(lrnToFilterBy, consignorId, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilter.and(Seq("lrn" -> Some(lrnToFilterBy)))
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }


    "return only the movements that correspond to the filter ARC" in {
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement("lrn1", consignorId, None, Some("arc2"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2)))

      val filter = MovementFilter.and(Seq("arc" -> Some(arcToFilterBy)))
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }

    "return only the movements that correspond to the filter LRN and ern" in {
      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(lrnToFilterBy, consignorId, None, Some("arc1"))
      val expectedMovement3 = Movement("lrn1", ernToFilterBy, None, Some("arc1"))
      val expectedMovement4 = Movement(lrnToFilterBy, ernToFilterBy, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2, expectedMovement3, expectedMovement4)))

      val filter = MovementFilter.and(Seq("ern" -> Some(ernToFilterBy), "lrn" -> Some(lrnToFilterBy)))
      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement4)
    }

    "return only the movements that correspond to the filter LRN, ern and arc" in {

      val expectedMovement1 = Movement("lrn1", consignorId, None, Some("arc1"))
      val expectedMovement2 = Movement(lrnToFilterBy, consignorId, None, Some("arc1"))
      val expectedMovement3 = Movement("lrn1", ernToFilterBy, None, Some("arc1"))
      val expectedMovement4 = Movement(lrnToFilterBy, ernToFilterBy, None, Some(arcToFilterBy))
      val expectedMovement5 = Movement("lrn1", consignorId, None, Some(arcToFilterBy))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1, expectedMovement2, expectedMovement3, expectedMovement4, expectedMovement5)))

      val filter = MovementFilter.and(Seq(
        "ern" -> Some(ernToFilterBy),
        "lrn" -> Some(lrnToFilterBy),
        "arc" -> Some(arcToFilterBy)
      ))
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
}
