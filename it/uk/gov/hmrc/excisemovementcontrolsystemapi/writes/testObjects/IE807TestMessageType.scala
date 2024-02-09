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

object IE807TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn4:IE807 xmlns:urn4="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE807:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn4:Header>
        <urn:MessageSender>NDEA.GB</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2023-06-27</urn:DateOfPreparation>
        <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>GB0023121</urn:MessageIdentifier>
        <urn:CorrelationIdentifier>6de24ff423abcb344bbcbcbcbc3423</urn:CorrelationIdentifier>
      </urn4:Header>
      <urn4:Body>
        <urn4:InterruptionOfMovement>
          <urn4:Attributes>
            <urn4:AdministrativeReferenceCode>23XI00000000000000331</urn4:AdministrativeReferenceCode>
            <urn4:ComplementaryInformation language="to">Customs aren't happy :(</urn4:ComplementaryInformation>
            <urn4:DateAndTimeOfIssuance>2023-06-27T00:18:13</urn4:DateAndTimeOfIssuance>
            <urn4:ReasonForInterruptionCode>1</urn4:ReasonForInterruptionCode>
            <urn4:ReferenceNumberOfExciseOffice>AB737333</urn4:ReferenceNumberOfExciseOffice>
            <urn4:ExciseOfficerIdentification>GB3939939393</urn4:ExciseOfficerIdentification>
          </urn4:Attributes>
          <urn4:ReferenceControlReport>
            <urn4:ControlReportReference>GBAA2C3F4244ADB9</urn4:ControlReportReference>
          </urn4:ReferenceControlReport>
          <urn4:ReferenceControlReport>
            <urn4:ControlReportReference>GBAA2C3F4244ADB8</urn4:ControlReportReference>
          </urn4:ReferenceControlReport>
          <urn4:ReferenceEventReport>
            <urn4:EventReportNumber>GBAA2C3F4244ADB3</urn4:EventReportNumber>
          </urn4:ReferenceEventReport>
        </urn4:InterruptionOfMovement>
      </urn4:Body>
    </urn4:IE807>


  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-06-27\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"GB0023121\",\"CorrelationIdentifier\":\"6de24ff423abcb344bbcbcbcbc3423\"},\"Body\":{\"InterruptionOfMovement\":{\"AttributesValue\":{\"AdministrativeReferenceCode\":\"23XI00000000000000331\",\"ComplementaryInformation\":{\"value\":\"Customs aren't happy :(\",\"attributes\":{\"@language\":\"to\"}},\"DateAndTimeOfIssuance\":\"2023-06-27T00:18:13\",\"ReasonForInterruptionCode\":\"1\",\"ReferenceNumberOfExciseOffice\":\"AB737333\",\"ExciseOfficerIdentification\":\"GB3939939393\"},\"ReferenceControlReport\":[{\"ControlReportReference\":\"GBAA2C3F4244ADB9\"},{\"ControlReportReference\":\"GBAA2C3F4244ADB8\"}],\"ReferenceEventReport\":[{\"EventReportNumber\":\"GBAA2C3F4244ADB3\"}]}}}")
}
