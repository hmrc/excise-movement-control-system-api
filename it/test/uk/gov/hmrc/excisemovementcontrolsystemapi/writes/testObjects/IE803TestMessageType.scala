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

package uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects
import play.api.libs.json.{JsValue, Json}

object IE803TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse(
    "{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-06-27\",\"TimeOfPreparation\":\"00:23:33\",\"MessageIdentifier\":\"GB002312688\",\"CorrelationIdentifier\":\"PORTAL6de1b822562c43fb9220d236e487c920\"},\"Body\":{\"NotificationOfDivertedEADESAD\":{\"ExciseNotification\":{\"NotificationType\":\"1\",\"NotificationDateAndTime\":\"2023-06-26T23:56:46\",\"AdministrativeReferenceCode\":\"23XI00000000000056333\",\"SequenceNumber\":\"1\"},\"DownstreamArc\":[]}}}"
  )

  override def auditEvent: JsValue = Json.parse(
    """{"messageCode":"IE803","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.XI","DateOfPreparation":"2023-06-27","TimeOfPreparation":"00:23:33","MessageIdentifier":"GB002312688","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"NotificationOfDivertedEADESAD":{"ExciseNotification":{"NotificationType":"1","NotificationDateAndTime":"2023-06-26T23:56:46","AdministrativeReferenceCode":"23XI00000000000056333","SequenceNumber":"1"},"DownstreamArc":[]}}},"outcome":{"status":"SUCCESS"}}"""
  )

  override def auditFailure(failureReason: String): JsValue = Json.parse(
    s"""{"messageCode":"IE803","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.XI","DateOfPreparation":"2023-06-27","TimeOfPreparation":"00:23:33","MessageIdentifier":"GB002312688","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"NotificationOfDivertedEADESAD":{"ExciseNotification":{"NotificationType":"1","NotificationDateAndTime":"2023-06-26T23:56:46","AdministrativeReferenceCode":"23XI00000000000056333","SequenceNumber":"1"},"DownstreamArc":[]}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}"""
  )
}
