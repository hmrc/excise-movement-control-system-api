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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response

import play.api.libs.json.{Format, Json, Writes}

sealed trait PreValidateTraderETDSResponse

object PreValidateTraderETDSResponse {
  implicit val preValidateTraderETDSResponseWrites: Writes[PreValidateTraderETDSResponse] =
    Writes[PreValidateTraderETDSResponse] {
      case validResponse: ExciseTraderValidationETDSResponse      =>
        Json.toJson(validResponse)
      case error400: PreValidateTraderETDS400ErrorMessageResponse =>
        Json.toJson(error400)
      case error500: PreValidateTraderETDS500ErrorMessageResponse =>
        Json.toJson(error500)
    }
}

case class ExciseTraderValidationETDSResponse(
  processingDateTime: String,
  exciseId: String,
  validationResult: String,
  failDetails: Option[ETDSFailDetails] = None
) extends PreValidateTraderETDSResponse

object ExciseTraderValidationETDSResponse {
  implicit val format: Format[ExciseTraderValidationETDSResponse] = Json.format[ExciseTraderValidationETDSResponse]
}

case class PreValidateTraderETDS400ErrorMessageResponse(
  processingDateTime: String,
  message: String
) extends PreValidateTraderETDSResponse

object PreValidateTraderETDS400ErrorMessageResponse {
  implicit val format: Format[PreValidateTraderETDS400ErrorMessageResponse] =
    Json.format[PreValidateTraderETDS400ErrorMessageResponse]
}

case class PreValidateTraderETDS500ErrorMessageResponse(
  processingDateTime: String,
  messages: Seq[String]
) extends PreValidateTraderETDSResponse

object PreValidateTraderETDS500ErrorMessageResponse {
  implicit val format: Format[PreValidateTraderETDS500ErrorMessageResponse] =
    Json.format[PreValidateTraderETDS500ErrorMessageResponse]
}
