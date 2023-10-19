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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis

object EISErrorMessage {

  def apply(
    createDateTime: String,
    consignorId: String,
    message: String,
    correlationId: String,
    messageTypes: String
  ): String = {
    s"""EIS error with message: $message,
    | messageId: $correlationId,
    | correlationId: $correlationId,
    | messageType: $messageTypes,
    | timestamp: $createDateTime,
    | exciseId: $consignorId""".stripMargin
  }

}