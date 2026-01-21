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
  private def formatError(
    header: String,
    createDateTime: String,
    correlationId: String,
    messageTypes: String
  ): String =
    s"""$header
   | messageId: $correlationId,
   | correlationId: $correlationId,
   | messageType: $messageTypes,
   | timestamp: $createDateTime""".stripMargin

  def parseError(
    createDateTime: String,
    correlationId: String,
    messageTypes: String
  ): String =
    formatError("Error parsing response JSON:", createDateTime, correlationId, messageTypes)

  def readError(
    createDateTime: String,
    correlationId: String,
    messageTypes: String
  ): String =
    formatError("Error deserializing response JSON:", createDateTime, correlationId, messageTypes)

  def apply(
    createDateTime: String,
    message: String,
    correlationId: String,
    messageTypes: String
  ): String =
    formatError(s"EIS error with message: $message,", createDateTime, correlationId, messageTypes)
}
