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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages

import generated.{Number1Value31, Number2Value30}
import org.scalatestplus.play.PlaySpec

class IEMessageSpec extends PlaySpec {
  "convertSubmitterType" should {
    "return Consignor" when {
      "submitter type is consignor" in {
        IEMessage.convertSubmitterType(Number1Value31) mustBe Consignor
      }
    }
    "return Consignee" when {
      "submitter type is consignee" in {
        IEMessage.convertSubmitterType(Number2Value30) mustBe Consignee
      }
    }
  }
}
