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

object IE807TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-06-27\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"GB0023121\",\"CorrelationIdentifier\":\"6de24ff423abcb344bbcbcbcbc3423\"},\"Body\":{\"InterruptionOfMovement\":{\"AttributesValue\":{\"AdministrativeReferenceCode\":\"23XI00000000000000331\",\"ComplementaryInformation\":{\"value\":\"Customs aren't happy :(\",\"attributes\":{\"@language\":\"to\"}},\"DateAndTimeOfIssuance\":\"2023-06-27T00:18:13\",\"ReasonForInterruptionCode\":\"1\",\"ReferenceNumberOfExciseOffice\":\"AB737333\",\"ExciseOfficerIdentification\":\"GB3939939393\"},\"ReferenceControlReport\":[{\"ControlReportReference\":\"GBAA2C3F4244ADB9\"},{\"ControlReportReference\":\"GBAA2C3F4244ADB8\"}],\"ReferenceEventReport\":[{\"EventReportNumber\":\"GBAA2C3F4244ADB3\"}]}}}")

  override def auditEvent: JsValue = Json.parse("""{"messageCode":"IE807","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.XI","DateOfPreparation":"2023-06-27","TimeOfPreparation":"00:18:33","MessageIdentifier":"GB0023121","CorrelationIdentifier":"6de24ff423abcb344bbcbcbcbc3423"},"Body":{"InterruptionOfMovement":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000000331","ComplementaryInformation":{"value":"Customs aren't happy :(","attributes":{"@language":"to"}},"DateAndTimeOfIssuance":"2023-06-27T00:18:13","ReasonForInterruptionCode":"1","ReferenceNumberOfExciseOffice":"AB737333","ExciseOfficerIdentification":"GB3939939393"},"ReferenceControlReport":[{"ControlReportReference":"GBAA2C3F4244ADB9"},{"ControlReportReference":"GBAA2C3F4244ADB8"}],"ReferenceEventReport":[{"EventReportNumber":"GBAA2C3F4244ADB3"}]}}},"outcome":{"status":"SUCCESS"}}""")

  override def auditFailure(failureReason: String): JsValue = Json.parse(s"""{"messageCode":"IE807","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.XI","DateOfPreparation":"2023-06-27","TimeOfPreparation":"00:18:33","MessageIdentifier":"GB0023121","CorrelationIdentifier":"6de24ff423abcb344bbcbcbcbc3423"},"Body":{"InterruptionOfMovement":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000000331","ComplementaryInformation":{"value":"Customs aren't happy :(","attributes":{"@language":"to"}},"DateAndTimeOfIssuance":"2023-06-27T00:18:13","ReasonForInterruptionCode":"1","ReferenceNumberOfExciseOffice":"AB737333","ExciseOfficerIdentification":"GB3939939393"},"ReferenceControlReport":[{"ControlReportReference":"GBAA2C3F4244ADB9"},{"ControlReportReference":"GBAA2C3F4244ADB8"}],"ReferenceEventReport":[{"EventReportNumber":"GBAA2C3F4244ADB3"}]}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}""")
}
