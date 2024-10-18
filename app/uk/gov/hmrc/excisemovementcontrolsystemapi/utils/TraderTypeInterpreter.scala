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
  def fromExciseId(exciseId: String) = {
    val prefix   = exciseId.take(2)
    val extended = exciseId.substring(2, 4)
    val suffix   = exciseId.takeRight(2)

    (prefix, extended) match {
      case ("GB" | "XI", "WK")                      => "1" //Warehouse Keeper
      case ("GB" | "XI", "00")                         => "2" //Tax Warehouse
      case ("XI", "00")                             => "4" //Registered Consignee
      case ("GB" | "XI", "RC")                      => "3" //Registered Consignor
      case ("XI", "TC") if exciseId.take(5) == "XITCA" => "6" //Temporary Consignee Authorisation
      case ("XI", "TC")                             => "5" //Temporary Registered Consignee

      case _                                              => "7" //Other
    }
  }
}
