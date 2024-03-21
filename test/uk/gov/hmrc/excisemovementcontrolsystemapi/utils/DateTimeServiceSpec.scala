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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat

import java.time.Instant

class DateTimeServiceSpec extends PlaySpec {

  "toStringInMillis" should {
    "return time in milliseconds" when {
      "time is in seconds" in {
        val dateTime = Instant.parse("2023-02-03T05:06:07Z")
        dateTime.toStringInMillis mustBe "2023-02-03T05:06:07.000Z"
      }

      "time is in deci seconds" in {
        val dateTime = Instant.parse("2023-02-03T05:06:07.1Z")
        dateTime.toStringInMillis mustBe "2023-02-03T05:06:07.100Z"
      }
      "time is in centi seconds" in {
        val dateTime = Instant.parse("2023-02-03T05:06:07.12Z")
        dateTime.toStringInMillis mustBe "2023-02-03T05:06:07.120Z"
      }

      "time is in centi nano seconds" in {
        val dateTime = Instant.parse("2023-02-03T05:06:07.124562Z")
        dateTime.toStringInMillis mustBe "2023-02-03T05:06:07.124Z"
      }
    }
  }

}
