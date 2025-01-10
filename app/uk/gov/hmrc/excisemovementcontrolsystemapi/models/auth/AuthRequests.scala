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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader

case class EnrolmentRequest[A](request: Request[A], erns: Set[String], userDetails: UserDetails)
    extends WrappedRequest[A](request) {
  def correlationId: String = request.headers
    .get(HttpHeader.xCorrelationId)
    .getOrElse(throw new Exception(s"${HttpHeader.xCorrelationId} not found"))
}
case class ParsedXmlRequest[A](
  request: EnrolmentRequest[A],
  ieMessage: IEMessage,
  erns: Set[String],
  userDetails: UserDetails
) extends WrappedRequest[A](request) {

  def headersAsMap: Map[String, String] =
    request.request.headers.headers.toMap

}
