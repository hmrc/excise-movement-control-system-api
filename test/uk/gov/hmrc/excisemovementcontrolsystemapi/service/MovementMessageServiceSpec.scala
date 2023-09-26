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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MongoError
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementMessageService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class MovementMessageServiceSpec extends PlaySpec with EitherValues{

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockMovementMessageRepository = mock[MovementMessageRepository]

  private val movementMessageService = new MovementMessageService(mockMovementMessageRepository)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneedId = "ABC123"

  "saveMovementMessage" should {
    "return a MovementMessage" in {
      val successMovementMessage = MovementMessage(lrn, consignorId, Some(consigneedId))
      when(mockMovementMessageRepository.saveMovementMessage(any))
        .thenReturn(Future.successful(true))

      val result = await(movementMessageService.saveMovementMessage(successMovementMessage))

      result mustBe Right(successMovementMessage)
    }

    "throw an error" in {
      when(mockMovementMessageRepository.saveMovementMessage(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementMessageService.saveMovementMessage(MovementMessage(lrn, consignorId, Some(consigneedId))))

      result.left.value mustBe MongoError("error")
    }
  }
}
