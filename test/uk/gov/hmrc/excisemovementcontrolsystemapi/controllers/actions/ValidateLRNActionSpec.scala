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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessageIE818
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementMessageService

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}


class ValidateLRNActionSpec extends PlaySpec with TestXml with EitherValues with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val emcsUtils: EmcsUtils = mock[EmcsUtils]
  private val movementMessageService = mock[MovementMessageService]

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(emcsUtils.getCurrentDateTime)
      .thenReturn(LocalDateTime.of(2023, 10, 18, 15, 33, 33))
  }

  "ValidateLRNActionSpec" should {
    "return a request when valid LRN/ERN combo in database" in {

      when(movementMessageService.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Right(Seq())))

      val sut = new ValidateLRNActionFactory().apply("lrn", movementMessageService)

      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val request = DataRequestIE818(FakeRequest(), MovementMessageIE818("GBWK002181023"), erns, "123")

      val result = await(sut.refine(request))

      val dataRequest = result.toOption.get
      dataRequest mustBe request
    }

    "an error" when {
      "LRN/ERN combo is not in the db" in {
        when(movementMessageService.getMovementMessagesByLRNAndERNIn(any, any))
          .thenReturn(Future.successful(Left(NotFoundError())))

        val sut = new ValidateLRNActionFactory().apply("lrn", movementMessageService)
        val request = DataRequestIE818(FakeRequest(), MovementMessageIE818("12356"), Set("12356"), "123")
        val result = await(sut.refine(request))

        result match {

          case Left(error: Result) =>
            error.header.status mustBe NOT_FOUND

            val response = contentAsJson(Future.successful(error)).validate[ErrorResponse].asEither

            response.map {
              response =>
                response.message mustBe "Invalid LRN supplied"
                response.debugMessage mustBe "LRN is not valid for this ERN"
            }

          case _ => fail("Should have an error")
        }

      }

      "DB error occurs" in {
        when(movementMessageService.getMovementMessagesByLRNAndERNIn(any, any))
          .thenReturn(Future.successful(Left(MongoError("Error accessing database"))))

        val sut = new ValidateLRNActionFactory().apply("lrn", movementMessageService)
        val request = DataRequestIE818(FakeRequest(), MovementMessageIE818("12356"), Set("12356"), "123")
        val result = await(sut.refine(request))

        result match {
          case Left(error: Result) =>
            error.header.status mustBe INTERNAL_SERVER_ERROR

            val response = contentAsJson(Future.successful(error)).validate[ErrorResponse].asEither

            response.map {
              response =>
                response.message mustBe "Database error"
                response.debugMessage mustBe "Error accessing database"
            }

          case _ => fail("Should have an error")
        }
      }
    }
  }
}
