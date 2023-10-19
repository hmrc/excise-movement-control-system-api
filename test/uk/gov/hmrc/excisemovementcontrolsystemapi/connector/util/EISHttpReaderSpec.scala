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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connector.util


import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.EISHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISResponse}
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDateTime
import scala.reflect.runtime.universe.typeOf

class EISHttpReaderSpec extends PlaySpec with EitherValues {

  private val eisHttpParser = EISHttpReader("123", "GB123", "date time")
  "read" should {
    "return EISResponse" in {
      val eisResponse = EISResponse("ok", "Success", "123")

      val result = eisHttpParser.read(
        "ANY",
        "/foo",
        HttpResponse(200, Json.toJson(eisResponse).toString())
      )

      result mustBe Right(eisResponse)
    }

    val exampleError = Json.toJson(EISErrorResponse(
      LocalDateTime.of(2023, 9, 19, 15, 57, 23),
      "Error",
      "Error details",
      "123"
    ))

    forAll(Seq(
      (400, BadRequest(exampleError)),
      (404, NotFound(exampleError)),
      (500, InternalServerError(exampleError)),
      (503, ServiceUnavailable(exampleError)))) { case (statusCode, expectedResult) =>
      s"return $statusCode" when {
        "$status code has returned from HttpResponse" in {
          val result = eisHttpParser.read(
            "ANY",
            "/foo",
            HttpResponse(statusCode, exampleError.toString())
          )

          result.left.value mustBe expectedResult
        }
      }
    }

    "throw if cannot parse json" in {
      val ex = intercept[RuntimeException] {
        eisHttpParser.read(
          "ANY",
          "/foo",
          HttpResponse(200, """{"test":"test"}""")
        )
      }

      ex.getMessage mustBe s"Response body could not be read as type ${typeOf[EISResponse]}"
    }
  }
}
