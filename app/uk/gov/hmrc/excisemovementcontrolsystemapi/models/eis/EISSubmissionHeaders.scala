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

import play.api.http.{ContentTypes, HeaderNames}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.Headers._

trait EISSubmissionHeaders extends Headers {

  override def build(correlationId: String, createdDateTime: String, bearerToken: String): Seq[(String, String)] =
    Seq(
      HeaderNames.ACCEPT       -> ContentTypes.JSON,
      HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
      DateTimeName             -> createdDateTime,
      XCorrelationIdName       -> correlationId,
      XForwardedHostName       -> MDTPHost,
      SourceName               -> APIPSource,
      Authorization            -> authorizationValue(bearerToken)
    )

  override def buildETDS(correlationId: String, createdDateTime: String, bearerToken: String): Seq[(String, String)] =
    Seq(
      HeaderNames.ACCEPT       -> ContentTypes.JSON,
      HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
      DateTimeName             -> createdDateTime,
      XCorrelationIdName       -> correlationId,
      XForwardedHostName       -> MDTPHost,
      SourceName               -> MDTPSource,
      Authorization            -> authorizationValue(bearerToken)
    )
}
