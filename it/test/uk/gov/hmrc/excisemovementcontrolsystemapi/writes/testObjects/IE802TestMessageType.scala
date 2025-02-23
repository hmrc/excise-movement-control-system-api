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

object IE802TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse(
    "{\"Header\":{\"MessageSender\":\"CSMISE.EC\",\"MessageRecipient\":\"CSMISE.EC\",\"DateOfPreparation\":\"2008-09-29\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"X00004\",\"CorrelationIdentifier\":\"PORTAL6de1b822562c43fb9220d236e487c920\"},\"Body\":{\"ReminderMessageForExciseMovement\":{\"AttributesValue\":{\"DateAndTimeOfIssuanceOfReminder\":\"2006-08-19T18:27:14\",\"ReminderInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"LimitDateAndTime\":\"2009-05-16T13:42:28\",\"ReminderMessageType\":\"2\"},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000090\",\"SequenceNumber\":\"10\"}}}}"
  )

  override def auditEvent: JsValue = Json.parse(
    """{"messageCode":"IE802","content":{"Header":{"MessageSender":"CSMISE.EC","MessageRecipient":"CSMISE.EC","DateOfPreparation":"2008-09-29","TimeOfPreparation":"00:18:33","MessageIdentifier":"X00004","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"ReminderMessageForExciseMovement":{"AttributesValue":{"DateAndTimeOfIssuanceOfReminder":"2006-08-19T18:27:14","ReminderInformation":{"value":"token","attributes":{"@language":"to"}},"LimitDateAndTime":"2009-05-16T13:42:28","ReminderMessageType":"2"},"ExciseMovement":{"AdministrativeReferenceCode":"23XI00000000000000090","SequenceNumber":"10"}}}},"outcome":{"status":"SUCCESS"}}"""
  )

  override def auditFailure(failureReason: String): JsValue = Json.parse(
    s"""{"messageCode":"IE802","content":{"Header":{"MessageSender":"CSMISE.EC","MessageRecipient":"CSMISE.EC","DateOfPreparation":"2008-09-29","TimeOfPreparation":"00:18:33","MessageIdentifier":"X00004","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"ReminderMessageForExciseMovement":{"AttributesValue":{"DateAndTimeOfIssuanceOfReminder":"2006-08-19T18:27:14","ReminderInformation":{"value":"token","attributes":{"@language":"to"}},"LimitDateAndTime":"2009-05-16T13:42:28","ReminderMessageType":"2"},"ExciseMovement":{"AdministrativeReferenceCode":"23XI00000000000000090","SequenceNumber":"10"}}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}"""
  )
}
