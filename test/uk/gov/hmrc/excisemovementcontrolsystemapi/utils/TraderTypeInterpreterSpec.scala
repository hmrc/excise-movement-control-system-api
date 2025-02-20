/*
 * Copyright 2025 HM Revenue & Customs
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

  "fromTraderTypeDescription" should {

    val toTest = List(
      ("Authorised Warehouse Keeper", "1"),
      ("Tax Warehouse", "2"),
      ("Registered Consignor", "3"),
      ("Registered Consignee", "4"),
      ("Temporary Registered Consignee", "5"),
      ("Temporary Registered Consignee Certification", "6"),
      ("Temporary Certified Consignor Certification", "6"),
      ("Temporary Certified Consignee Certification", "6"),
      ("Certified Consignor", "7"),
      ("Certified Consignee", "7"),
      ("Temporary Certified Consignor", "7"),
      ("Temporary Certified Consignee", "7"),
      ("Duty Representative", "7"),
      ("Registered Owner", "7"),
      ("Not Found", "7")
    )

    toTest.foreach { case (description: String, code: String) =>
      s"return type $code from TraderTypeDescription $description" in {

        TraderTypeInterpreter.fromTraderTypeDescription(description) mustBe code

      }
    }
  }

}
