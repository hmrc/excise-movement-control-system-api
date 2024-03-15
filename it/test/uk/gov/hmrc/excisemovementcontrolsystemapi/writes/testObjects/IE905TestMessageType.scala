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

object IE905TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-07-02\",\"TimeOfPreparation\":\"21:23:41\",\"MessageIdentifier\":\"XI00432RR\",\"CorrelationIdentifier\":\"6774741231ff3111f3233\"},\"Body\":{\"StatusResponse\":{\"AttributesValue\":{\"AdministrativeReferenceCode\":\"23XI00000000000056349\",\"SequenceNumber\":\"1\",\"Status\":\"X07\",\"LastReceivedMessageType\":\"IE881\"}}}}")

  override def auditEvent: JsValue = Json.parse("""{"messageCode":"IE905","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.XI","DateOfPreparation":"2023-07-02","TimeOfPreparation":"21:23:41","MessageIdentifier":"XI00432RR","CorrelationIdentifier":"6774741231ff3111f3233"},"Body":{"StatusResponse":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000056349","SequenceNumber":"1","Status":"X07","LastReceivedMessageType":"IE881"}}}},"outcome":{"status":"SUCCESS"}}""")

  override def auditFailure(failureReason: String) = Json.parse(s"""{"messageCode":"IE905","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.XI","DateOfPreparation":"2023-07-02","TimeOfPreparation":"21:23:41","MessageIdentifier":"XI00432RR","CorrelationIdentifier":"6774741231ff3111f3233"},"Body":{"StatusResponse":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000056349","SequenceNumber":"1","Status":"X07","LastReceivedMessageType":"IE881"}}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}""")
}