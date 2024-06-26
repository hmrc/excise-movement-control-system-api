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

import play.api.libs.json.{Json, OFormat}

case class RimValidatorResults(
  errorCategory: Option[String],
  errorType: Option[BigInt],
  errorReason: Option[String],
  errorLocation: Option[String],
  originalAttributeValue: Option[String]
)

object RimValidatorResults {
  implicit val format: OFormat[RimValidatorResults] = Json.format[RimValidatorResults]
}

case class RimValidationErrorResponse(
  emcsCorrelationId: String,
  message: Seq[String],
  validatorResults: Seq[RimValidatorResults]
)

object RimValidationErrorResponse {
  implicit val format: OFormat[RimValidationErrorResponse] = Json.format[RimValidationErrorResponse]
}
