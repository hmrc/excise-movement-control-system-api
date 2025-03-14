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

object IE810TestMessageType extends TestMessageType {
  override def json1: JsValue = Json.parse(
    "{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-06-13\",\"TimeOfPreparation\":\"10:16:58\",\"MessageIdentifier\":\"GB100000000302249\",\"CorrelationIdentifier\":\"PORTAL6de1b822562c43fb9220d236e487c920\"},\"Body\":{\"CancellationOfEAD\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfCancellation\":\"2023-06-13T10:17:05\"},\"ExciseMovementEad\":{\"AdministrativeReferenceCode\":\"23GB00000000000377161\"},\"Cancellation\":{\"CancellationReasonCode\":\"3\"}}}}"
  )

  override def auditEvent: JsValue = Json.parse(
    """{"messageCode":"IE810","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-06-13","TimeOfPreparation":"10:16:58","MessageIdentifier":"GB100000000302249","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"CancellationOfEAD":{"AttributesValue":{"DateAndTimeOfValidationOfCancellation":"2023-06-13T10:17:05"},"ExciseMovementEad":{"AdministrativeReferenceCode":"23GB00000000000377161"},"Cancellation":{"CancellationReasonCode":"3"}}}},"outcome":{"status":"SUCCESS"}}"""
  )

  override def auditFailure(failureReason: String): JsValue = Json.parse(
    s"""{"messageCode":"IE810","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-06-13","TimeOfPreparation":"10:16:58","MessageIdentifier":"GB100000000302249","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"CancellationOfEAD":{"AttributesValue":{"DateAndTimeOfValidationOfCancellation":"2023-06-13T10:17:05"},"ExciseMovementEad":{"AdministrativeReferenceCode":"23GB00000000000377161"},"Cancellation":{"CancellationReasonCode":"3"}}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}"""
  )
}
