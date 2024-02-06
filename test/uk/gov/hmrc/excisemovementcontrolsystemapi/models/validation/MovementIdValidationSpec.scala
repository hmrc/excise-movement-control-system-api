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

import org.mockito.MockitoSugar.when
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, NotFound}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class MovementIdValidationSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {
  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val consignorId = "123"
  private val movementService = mock[MovementService]
  private val timestamp = Instant.now()

  private val movementIdValidator = MovementIdValidation(movementService)

  "validateMovementId" should {

    "return the movement for the id if it is valid and found" in {
      val uuid = "17425b8d-92e3-41e1-a7be-f21ab9241911"
      val expectedMovement1 = Movement(uuid, "boxId", "lrn1", consignorId, None, Some("arc1"), Instant.now, Seq.empty)

      when(movementService.getMovementById(uuid))
        .thenReturn(Future.successful(Some(expectedMovement1)))

      val result = await(movementIdValidator.validateMovementId(uuid))

      result mustBe Right(expectedMovement1)
    }

    "return NotFound error if no match" in {

      val uuid = "17425b8d-92e3-41e1-a7be-f21ab9241911"

      when(movementService.getMovementById(uuid))
        .thenReturn(Future.successful(None))

      val result = await(movementIdValidator.validateMovementId(uuid)).left.value

      result mustBe a[MovementIdNotFound]
      result.errorMessage mustBe "Movement 17425b8d-92e3-41e1-a7be-f21ab9241911 could not be found"

    }

    "return FormatInvalid error if format is invalid" in {

      val result = await(movementIdValidator.validateMovementId("uuid")).left.value

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

    "return a 404 Not Found" when {

      "movement id format invalid error" in {

        val result = movementIdValidator.convertErrorToResponse(MovementIdNotFound("badId"), timestamp)

        result mustBe NotFound(Json.toJson(ErrorResponse(
          timestamp,
          "Movement not found",
          "Movement badId could not be found"
        )))
      }

    }

  }

}
