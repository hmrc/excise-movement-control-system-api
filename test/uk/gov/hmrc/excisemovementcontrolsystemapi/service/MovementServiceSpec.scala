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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{GeneralMongoError, NotFoundError}
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
    "return  List of Messages" in {
      val messages = Seq(Message("123456", "IE801"), Message("ABCDE", "IE815"))
      val movementMessage = Movement(lrn, consignorId, Some(consigneeId), None, Instant.now(), Some(messages))
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movementMessage)))


      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Right(messages)
    }

    "return empty Message when Movement is found with no Messages" in {
      val movementMessage = Movement(lrn, consignorId, Some(consigneeId), None, Instant.now())
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movementMessage)))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Right(Seq.empty)
    }

    "throw an error" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result.left.value mustBe GeneralMongoError("error")
    }
  }

  "getMovementMessagesByLRNAndERNIn with no movement message for LRN and ERN combination" should {
    "return a NotFoundError" in {
      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(None))

      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))

      result.left.value mustBe NotFoundError()
    }
  }

  //TODO these tests will be relevant when we change back to the other way
//  "getMovementMessagesByLRNAndERNIn with multiple movement messages for LRN and ERN combination" should {
//    "return a MongoError" in {
//      val movementMessage = Movement(lrn, consignorId, Some(consigneeId))
//      when(mockMovementMessageRepository.getMovementByLRNAndERNIn(any, any))
//        .thenReturn(Future.successful(Seq(movementMessage, movementMessage)))
//
//      val result = await(movementMessageService.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)))
//
//      result.left.value mustBe GeneralMongoError("Multiple movements found for lrn and ern combination")
//    }
//  }


}
