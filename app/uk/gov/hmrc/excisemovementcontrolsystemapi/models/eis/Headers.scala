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

import play.api.http.HeaderNames

trait Headers {
  def build(correlationId: String, createdDateTime: String, bearerToken: String): Seq[(String, String)]
  def buildETDS(correlationId: String, createdDateTime: String, bearerToken: String): Seq[(String, String)]
}

object Headers {
  val APIPSource: String         = "APIP"
  val MDTPSource: String         = "MDTP"
  val MDTPHost: String           = "MDTP"
  val SourceName: String         = "Source"
  val XCorrelationIdName: String = "X-Correlation-Id"
  val DateTimeName: String       = "DateTime"
  val XForwardedHostName: String = HeaderNames.X_FORWARDED_HOST
  val Authorization: String      = HeaderNames.AUTHORIZATION

  def authorizationValue(bearerToken: String) = s"Bearer $bearerToken"
}
