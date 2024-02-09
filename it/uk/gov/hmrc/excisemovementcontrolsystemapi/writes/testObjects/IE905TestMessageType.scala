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

import scala.xml.NodeSeq

object IE905TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn6:IE905 xmlns:urn6="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE905:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn6:Header>
        <urn:MessageSender>NDEA.GB</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2023-07-02</urn:DateOfPreparation>
        <urn:TimeOfPreparation>21:23:41</urn:TimeOfPreparation>
        <urn:MessageIdentifier>XI00432RR</urn:MessageIdentifier>
        <urn:CorrelationIdentifier>6774741231ff3111f3233</urn:CorrelationIdentifier>
      </urn6:Header>
      <urn6:Body>
        <urn6:StatusResponse>
          <urn6:Attributes>
            <urn6:AdministrativeReferenceCode>23XI00000000000056349</urn6:AdministrativeReferenceCode>
            <urn6:SequenceNumber>1</urn6:SequenceNumber>
            <urn6:Status>X07</urn6:Status>
            <urn6:LastReceivedMessageType>IE881</urn6:LastReceivedMessageType>
          </urn6:Attributes>
        </urn6:StatusResponse>
      </urn6:Body>
    </urn6:IE905>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-07-02\",\"TimeOfPreparation\":\"21:23:41\",\"MessageIdentifier\":\"XI00432RR\",\"CorrelationIdentifier\":\"6774741231ff3111f3233\"},\"Body\":{\"StatusResponse\":{\"AttributesValue\":{\"AdministrativeReferenceCode\":\"23XI00000000000056349\",\"SequenceNumber\":\"1\",\"Status\":\"X07\",\"LastReceivedMessageType\":\"IE881\"}}}}")
}
