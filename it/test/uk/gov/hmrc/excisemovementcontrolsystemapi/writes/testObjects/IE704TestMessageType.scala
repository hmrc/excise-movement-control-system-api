/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects
import play.api.libs.json.{JsValue, Json}

object IE704TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse(
    """{"Header":{"MessageSender":"NDEA.XI","MessageRecipient":"NDEA.XI","DateOfPreparation":"2008-09-29","TimeOfPreparation":"00:18:33","MessageIdentifier":"XI000001","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"GenericRefusalMessage":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000000012","SequenceNumber":"1","LocalReferenceNumber":"lrnie8158976912"},"FunctionalError":[{"ErrorType":"4401","ErrorReason":"token","ErrorLocation":"token","OriginalAttributeValue":"token"}]}}}""".stripMargin
  )

  override def auditEvent: JsValue = Json.parse(
    """{"messageCode":"IE704","content":{"Header":{"MessageSender":"NDEA.XI","MessageRecipient":"NDEA.XI","DateOfPreparation":"2008-09-29","TimeOfPreparation":"00:18:33","MessageIdentifier":"XI000001","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"GenericRefusalMessage":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000000012","SequenceNumber":"1","LocalReferenceNumber":"lrnie8158976912"},"FunctionalError":[{"ErrorType":"4401","ErrorReason":"token","ErrorLocation":"token","OriginalAttributeValue":"token"}]}}},"outcome":{"status":"SUCCESS"}}"""
  )

  override def auditFailure(failureReason: String): JsValue = Json.parse(
    s"""{"messageCode":"IE704","content":{"Header":{"MessageSender":"NDEA.XI","MessageRecipient":"NDEA.XI","DateOfPreparation":"2008-09-29","TimeOfPreparation":"00:18:33","MessageIdentifier":"XI000001","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"GenericRefusalMessage":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000000012","SequenceNumber":"1","LocalReferenceNumber":"lrnie8158976912"},"FunctionalError":[{"ErrorType":"4401","ErrorReason":"token","ErrorLocation":"token","OriginalAttributeValue":"token"}]}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}"""
  )
}
