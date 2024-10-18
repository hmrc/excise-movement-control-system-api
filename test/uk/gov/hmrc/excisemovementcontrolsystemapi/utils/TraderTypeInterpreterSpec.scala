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

class TraderTypeInterpreterSpec extends PlaySpec {

  "TraderTypeInterpreter" should {

    "return 1 for a warehouse keeper" in {
      val result = TraderTypeInterpreter.fromExciseId("GBWK1234567WK")
      result mustBe "1"
    }

    "return 2 for a tax warehouse" in {
      val result1 = TraderTypeInterpreter.fromExciseId("GB00123456789")
      val result2 = TraderTypeInterpreter.fromExciseId("XI00123456789")
      result1 mustBe "2"
      result2 mustBe "2"
    }

    "return 3 for a registered consignor" in {
      val result1 = TraderTypeInterpreter.fromExciseId("GBRC1234567RC")
      val result2 = TraderTypeInterpreter.fromExciseId("XIRC123456789RC")
      result1 mustBe "3"
      result2 mustBe "3"
    }

    "return 4 for a registered consignee" in {
      val result = TraderTypeInterpreter.fromExciseId("XI00123456789RT")
      result mustBe "4"
    }

    "return 5 for a temporary registered consignee" in {
      val result = TraderTypeInterpreter.fromExciseId("XITC1234567TC")
      result mustBe "5"
    }

    "return 6 for a temporary consignee authorisation" in {
      val result = TraderTypeInterpreter.fromExciseId("XITCA12345678")
      result mustBe "6"
    }

    "return 7 for any other cases" in {
      val result = TraderTypeInterpreter.fromExciseId("XX000000XX")
      result mustBe "7"
    }

  }
}
