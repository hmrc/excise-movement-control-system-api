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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import play.api.mvc.WrappedRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton //TODO: This is going to be removed in 820 (probably) as we source the id from the headercarriers
class CorrelationIdService @Inject() {

  def generateCorrelationId(): String = UUID.randomUUID().toString

  def guaranteeCorrelationId(request: ParsedXmlRequest[_]): WrappedRequest[_] =
    if (!request.headers.hasHeader(HttpHeader.xCorrelationId)) {
      val amendedHeaders = request.headers.add(HttpHeader.xCorrelationId -> generateCorrelationId())
      request.withHeaders(amendedHeaders)
    } else {
      request
    }
}

object HttpHeader {
  val xCorrelationId = "x-correlation-id"
}
