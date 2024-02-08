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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation

import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService

import java.time.Instant
import scala.concurrent.ExecutionContext

class MovementIdValidationSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {
  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val movementService = mock[MovementService]
  private val timestamp = Instant.now()

  private val movementIdValidator = MovementIdValidation(movementService)

  "validateMovementId" should {

    "return the movement id if it is valid" in {
      val uuid = "17425b8d-92e3-41e1-a7be-f21ab9241911"

      val result = movementIdValidator.validateMovementId(uuid)

      result mustBe Right(uuid)
    }

    "return FormatInvalid error if format is invalid" in {

      val result = movementIdValidator.validateMovementId("uuid").left.value

      result mustBe a[MovementIdFormatInvalid]
      result.errorMessage mustBe "The movement ID should be a valid UUID"

    }
  }

  "convertErrorToResponse" should {

    "return a 400 Bad Request" when {

      "movement id format invalid error" in {

        val result = movementIdValidator.convertErrorToResponse(MovementIdFormatInvalid(), timestamp)

        result mustBe BadRequest(Json.toJson(ErrorResponse(
          timestamp,
          "Movement Id format error",
          "The movement ID should be a valid UUID"
        )))
      }

    }

  }

}
