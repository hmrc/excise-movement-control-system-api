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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.{LocalDateTime, ZoneOffset}

class MessageReceiptSuccessResponseSpec extends AnyFreeSpec with Matchers {

  private val expected = MessageReceiptSuccessResponse(
    dateTime = LocalDateTime.of(2024, 2, 1, 12, 30, 45).toInstant(ZoneOffset.UTC),
    exciseRegistrationNumber = "someern",
    recordsAffected = 1337
  )

  "must read recordsAffected from a number" in {

    val json = Json.obj(
      "dateTime" -> expected.dateTime,
      "exciseRegistrationNumber" -> "someern",
      "recordsAffected" -> 1337
    )

    val result = json.validate[MessageReceiptSuccessResponse]

    result.isSuccess mustBe true
    result.get mustBe expected
  }

  "must read recordsAffected from a string" in {

    val json = Json.obj(
      "dateTime" -> expected.dateTime,
      "exciseRegistrationNumber" -> "someern",
      "recordsAffected" -> "1337"
    )

    val result = json.validate[MessageReceiptSuccessResponse]

    result.isSuccess mustBe true
    result.get mustBe expected
  }

  "must fail to read recordsAffected from an invalid string" in {

    val json = Json.obj(
      "dateTime" -> expected.dateTime,
      "exciseRegistrationNumber" -> "someern",
      "recordsAffected" -> "foobar"
    )

    val result = json.validate[MessageReceiptSuccessResponse]

    result.isSuccess mustBe false
  }
}
