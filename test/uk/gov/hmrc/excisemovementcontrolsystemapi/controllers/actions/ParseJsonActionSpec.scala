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

import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderETDSRequest, getPreValidateTraderRequest}

import java.time.Instant
import scala.concurrent.ExecutionContext

class ParseJsonActionSpec extends PlaySpec with ScalaFutures with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val dateTimeService = mock[DateTimeService]
  private val parseJsonAction = new ParseJsonActionImpl(
    dateTimeService,
    stubControllerComponents()
  )

  private val exampleJson                     =
    "{\n    \"exciseTraderValidationRequest\": {\n        \"exciseTraderRequest\": {\n            \"exciseRegistrationNumber\": \"GBWK0000010\",\n            \"entityGroup\": \"UK Record\",\n            \"validateProductAuthorisationRequest\": [\n                {\n                    \"product\": {\n                        \"exciseProductCode\": \"W200\"\n                    }\n                },\n                {\n                    \"product\": {\n                        \"exciseProductCode\": \"S100\"\n                    }\n                }\n            ]\n        }\n    }\n}"
  private val examplePreValidateTraderRequest = PreValidateTraderRequest(
    ExciseTraderValidationRequest(
      ExciseTraderRequest(
        "GBWK0000010",
        "UK Record",
        Seq(
          ValidateProductAuthorisationRequest(ExciseProductCode("W200")),
          ValidateProductAuthorisationRequest(ExciseProductCode("S100"))
        )
      )
    )
  )

  private val examplePreValidateTraderETDSRequest = getPreValidateTraderETDSRequest
  private val exampleETDSJson                     = Json.toJson(examplePreValidateTraderETDSRequest)

  private val timestamp = Instant.parse("2023-05-11T01:01:01.987654Z")
  when(dateTimeService.timestamp()).thenReturn(timestamp)

  "refine" should {
    "return a ParsedPreValidateTraderRequest" in {
      val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(Json.parse(exampleJson)), Set("ern"), "123")

      val result = parseJsonAction.refine(enrolmentRequest).futureValue

      result mustBe Right(ParsedPreValidateTraderRequest(enrolmentRequest, examplePreValidateTraderRequest))
    }

    "return an error" when {
      "no json was sent" in {
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(""), Set("ern"), "123")

        val result = parseJsonAction.refine(enrolmentRequest).futureValue

        val expectedError = ErrorResponse(timestamp, "Json error", "Not valid Json or Json is empty")
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }

      "JSON parsing exception" in {

        val exampleJson      = "{\"exciseTraderValidationRequest\": 123 }"
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(Json.parse(exampleJson)), Set("ern"), "123")

        val result = parseJsonAction.refine(enrolmentRequest).futureValue

        val expectedError = ErrorResponse(
          timestamp,
          "Not valid PreValidateTrader message",
          "Error parsing Json: List((/exciseTraderValidationRequest,List(JsonValidationError(List(error.expected.jsobject),List()))))"
        )
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }

    }
  }

  "refineETDS" should {
    "return a ParsedPreValidateTraderETDSRequest" in {
      val enrolmentRequest =
        EnrolmentRequest(FakeRequest().withBody(Json.toJson(getPreValidateTraderRequest)), Set("ern"), "123")

      val result = parseJsonAction.refineETDS(enrolmentRequest).futureValue

      result mustBe Right(ParsedPreValidateTraderETDSRequest(enrolmentRequest, examplePreValidateTraderETDSRequest))
    }

    "return an error" when {
      "no json was sent" in {
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(""), Set("ern"), "123")

        val result = parseJsonAction.refineETDS(enrolmentRequest).futureValue

        val expectedError = ErrorResponse(timestamp, "Json error", "Not valid Json or Json is empty")
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }

      "JSON parsing exception" in {

        val exampleJson      = "{\"someInvalidJson\": 123 }"
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(Json.parse(exampleJson)), Set("ern"), "123")

        val result = parseJsonAction.refineETDS(enrolmentRequest).futureValue

        result.left.value.header.status mustBe 400
      }

    }
  }

}
