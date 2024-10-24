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

object TraderTypeInterpreter {
  def fromTraderTypeDescription(traderTypeDescription: String): String = traderTypeDescription match {
    case "Authorised Warehouse Keeper"                  => "1"
    case "Tax Warehouse"                                => "2"
    case "Registered Consignor"                         => "3"
    case "Registered Consignee"                         => "4"
    case "Temporary Registered Consignee"               => "5"
    case "Temporary Registered Consignee Certification" => "6"
    case "Temporary Certified Consignor Certification"  => "6"
    case "Temporary Certified Consignee Certification"  => "6"
    case _                                              => "7"

  }
}
