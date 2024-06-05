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

import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable, UnprocessableEntity}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionResponse, RimValidationErrorResponse, RimValidatorResults}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EisErrorResponsePresentation, MessageTypes, ValidationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant
import scala.reflect.runtime.universe.typeOf

class EISHttpReaderSpec extends PlaySpec with EitherValues {

  private val localDateTime   = Instant.parse("2023-09-19T15:57:23.654123456Z")
  private val dateTimeService = mock[DateTimeService]
  when(dateTimeService.timestamp()).thenReturn(localDateTime)

  private val eisHttpParser        = EISHttpReader("123", "GB123", "date time", dateTimeService, MessageTypes.IE815.value)
  private val exampleEISError      = Json.toJson(
    EISErrorResponse(
      localDateTime,
      "BAD_REQUEST",
      "Error",
      "Error details",
      "123"
    )
  )
  private val exampleResponseError = Json.toJson(
    EisErrorResponsePresentation(localDateTime, "Error", "Error details", "123")
  )

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

    forAll(
      Seq(
        (BAD_REQUEST, BadRequest(exampleResponseError)),
        (NOT_FOUND, NotFound(exampleResponseError)),
        (INTERNAL_SERVER_ERROR, InternalServerError(exampleResponseError)),
        (SERVICE_UNAVAILABLE, ServiceUnavailable(exampleResponseError)),
        (UNPROCESSABLE_ENTITY, UnprocessableEntity(exampleResponseError))
      )
    ) { case (statusCode, expectedResult) =>
      s"return $statusCode" when {
        s"$statusCode has returned from HttpResponse" in {
          val result = eisHttpParser.read(
            "ANY",
            "/foo",
            HttpResponse(statusCode, exampleEISError.toString())
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

    "return a 500 Internal Server Error if EIS has supplied an unexpected error format" in {

      val eisError = Json.parse("{\"status\": 500, \"message\": \"Unexpected error\"}")

      val result = eisHttpParser.read(
        "ANY",
        "/foo",
        HttpResponse(BAD_GATEWAY, eisError.toString())
      )

      val error = Json.parse(s"""
          |{
          |   "dateTime":"2023-09-19T15:57:23.654Z",
          |   "message":"Unexpected error",
          |   "debugMessage":"Error occurred while reading downstream response",
          |   "correlationId":"123"
          |}
          |""".stripMargin)

      result.left.value mustBe InternalServerError(error)

    }
  }

  private def createRimValidationResponse =
    RimValidationErrorResponse(
      emcsCorrelationId = "correlationId",
      message = Seq("Validation error(s) occurred"),
      validatorResults = Seq(
        createRimError(8080L, "/con:Control[1]/con:Parameter[1]/urn:IE815[1]/urn:DateOfDispatch[1]"),
        createRimError(
          8090L,
          "/con:Control[1]/con:Parameter[1]/urn:IE818[1]/urn:AcceptedOrRejectedReportOfReceiptExport[1]/urn:Attributes[1][1]",
          None
        )
      )
    )

  private def expectedRimValidationResponse =
    EisErrorResponsePresentation(
      localDateTime,
      "Validation error",
      "Validation error(s) occurred",
      "correlationId",
      Some(
        Seq(
          createLocalValidationError(8080L, "/urn:IE815[1]/urn:DateOfDispatch[1]"),
          createLocalValidationError(
            8090L,
            "/urn:IE818[1]/urn:AcceptedOrRejectedReportOfReceiptExport[1]/urn:Attributes[1][1]",
            None
          )
        )
      )
    )

  private def createRimError(
    errorCode: BigInt,
    location: String,
    origValue: Option[String] = Some(localDateTime.toString)
  ): RimValidatorResults =
    RimValidatorResults(
      errorCategory = Some("business"),
      errorType = Some(errorCode),
      errorReason = Some("The Date of Dispatch you entered is incorrect"),
      errorLocation = Some(location),
      originalAttributeValue = origValue
    )

  private def createLocalValidationError(
    errorCode: BigInt,
    location: String,
    origValue: Option[String] = Some(localDateTime.toString)
  ): ValidationResponse =
    ValidationResponse(
      errorCategory = Some("business"),
      errorType = Some(errorCode),
      errorReason = Some("The Date of Dispatch you entered is incorrect"),
      errorLocation = Some(location),
      originalAttributeValue = origValue
    )
}
