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

import java.time.Instant

class ErrorResponseSpec extends PlaySpec {

  "ErrorResponse toJson" should {
    "display dateTime in milliseconds format" in {

      val dateTime = Instant.parse("2024-12-05T12:30:15.15632145Z")

      Json.toJson(ErrorResponse(dateTime, "any message", "any debug message")) mustBe
        Json.parse(
          s"""
             |{
             |   "dateTime":"2024-12-05T12:30:15.156Z",
             |   "message":"any message",
             |   "debugMessage": "any debug message"
             |}
             |""".stripMargin)
    }
  }

}
