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
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MovementMessageCreateFailedResult, MovementMessageCreatedResult}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementMessageService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class MovementMessageServiceSpec extends PlaySpec {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockMovementMessageRepository = mock[MovementMessageRepository]

  private val movementMessageService = new MovementMessageService(mockMovementMessageRepository)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneedId = "ABC123"

  "saveMovementMessage" should {
    "return CreateMovementMessageResult" in {
      val successMovementMessage = MovementMessageCreatedResult(MovementMessage(lrn, consignorId, consigneedId))
      when(mockMovementMessageRepository.saveMovementMessage(any)).thenReturn(Future.successful(successMovementMessage))

      val result = await(movementMessageService.saveMovementMessage(lrn, consignorId, consigneedId))

      result mustBe successMovementMessage
    }
  }


}
