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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util


import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable, UnprocessableEntity}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionResponse, RimValidationErrorResponse, ValidatorResults}
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant
import scala.reflect.runtime.universe.typeOf

class EISHttpReaderSpec extends PlaySpec with EitherValues {

  private val eisHttpParser = EISHttpReader("123", "GB123", "date time")
  private val localDateTime = Instant.parse("2023-09-19T15:57:23.654Z")
  private val exampleError = Json.toJson(
    EISErrorResponse(
      localDateTime,
      "BAD_REQUEST",
      "Error",
      "Error details",
      "123"
    ))

  "read" should {
    "return EISResponse" in {
      val eisResponse = EISSubmissionResponse("ok", "Success", "123")

      val result = eisHttpParser.read(
        "ANY",
        "/foo",
        HttpResponse(200, Json.toJson(eisResponse).toString())
      )

      result mustBe Right(eisResponse)
    }

    forAll(Seq(
      (BAD_REQUEST, BadRequest(exampleError)),
      (NOT_FOUND, NotFound(exampleError)),
      (INTERNAL_SERVER_ERROR, InternalServerError(exampleError)),
      (SERVICE_UNAVAILABLE, ServiceUnavailable(exampleError)),
      (UNPROCESSABLE_ENTITY, UnprocessableEntity(exampleError)))) { case (statusCode, expectedResult) =>
      s"return $statusCode" when {
        s"$statusCode has returned from HttpResponse" in {
          val result = eisHttpParser.read(
            "ANY",
            "/foo",
            HttpResponse(statusCode, exampleError.toString())
          )
          result.left.value mustBe expectedResult
        }
      }
    }

    "cleanup references to the control document in from EIS validation" when {
      "return a BAD_REQUEST" in {
        val result = eisHttpParser.read(
          "ANY",
          "/foo",
          HttpResponse(BAD_REQUEST, Json.toJson(createRimValidationResponse).toString())
        )

        result.left.value mustBe BadRequest(Json.toJson(expectedRimValidationResponse))
      }

      "return a UNPROCESSABLE_ENTITY" in {
        val result = eisHttpParser.read(
          "ANY",
          "/foo",
          HttpResponse(UNPROCESSABLE_ENTITY, Json.toJson(createRimValidationResponse).toString())
        )

        result.left.value mustBe UnprocessableEntity(Json.toJson(expectedRimValidationResponse))
      }
    }

    "throw if cannot parse json" in {

      the[RuntimeException] thrownBy {
        eisHttpParser.read(
          "ANY",
          "/foo",
          HttpResponse(200, """{"test":"test"}""")
        )
      } must have message s"Response body could not be read as type ${typeOf[EISSubmissionResponse]}"
    }
  }

  private def createRimValidationResponse = {
    RimValidationErrorResponse(
      emcsCorrelationId = "correlationId",
      message = Seq("Validation error(s) occurred"),
      validatorResults = Seq(
        createRimError(8080L, "/con:Control[1]/con:Parameter[1]/urn:IE815[1]/urn:DateOfDispatch[1]"),
        createRimError(8090L, "/con:Control[1]/con:Parameter[1]/urn:IE818[1]/urn:DateOfDispatch[1]")
      )
    )
  }

  private def expectedRimValidationResponse = {
    RimValidationErrorResponse(
      emcsCorrelationId = "correlationId",
      message = Seq("Validation error(s) occurred"),
      validatorResults = Seq(
        createRimError(8080L, "/urn:IE815[1]/urn:DateOfDispatch[1]"),
        createRimError(8090L, "/urn:IE818[1]/urn:DateOfDispatch[1]"),
      )
    )
  }

  private def createRimError(errorCode: BigInt, location: String): ValidatorResults = {
    ValidatorResults(
      errorCategory = "business",
      errorType = errorCode,
      errorReason = "The Date of Dispatch you entered is incorrect",
      errorLocation = location,
      originalAttributeValue = localDateTime.toString
    )
  }
}
