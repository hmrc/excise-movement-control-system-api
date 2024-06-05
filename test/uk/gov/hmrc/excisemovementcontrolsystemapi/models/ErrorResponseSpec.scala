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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.ErrorResponseSupport

import java.time.Instant

class ErrorResponseSpec extends PlaySpec with ErrorResponseSupport {

  "ErrorResponse toJson" should {
    "display dateTime in milliseconds format when writing to Json" in {

      val dateTime = Instant.parse("2024-12-05T12:30:15.15632145Z")
      val error    = ErrorResponse(dateTime, "any message", "any debug message")

      Json.toJson(error) mustBe expectedJsonErrorResponse(
        "2024-12-05T12:30:15.156Z",
        "any message",
        "any debug message"
      )
    }
  }

  "EisErrorResponsePresentation" should {
    "display dateTime in milliseconds format when writing to Json" in {

      val dateTime = Instant.parse("2024-12-05T12:30:15.15632145Z")
      val error    = EisErrorResponsePresentation(dateTime, "any message", "any debug message", "correlationId")

      Json.toJson(error) mustBe expectedEisErrorResponsePresentation(
        "2024-12-05T12:30:15.156Z",
        "any message",
        "any debug message",
        "correlationId"
      )
    }
  }

}
