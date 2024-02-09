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

object IE839TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn4:IE839 xmlns:urn4="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn4:Header>
        <urn:MessageSender>NDEA.XI</urn:MessageSender>
        <urn:MessageRecipient>NDEA.FR</urn:MessageRecipient>
        <urn:DateOfPreparation>2024-06-26</urn:DateOfPreparation>
        <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>XI004322</urn:MessageIdentifier>
        <urn:CorrelationIdentifier>6dddas1231ff3a678fefffff3233</urn:CorrelationIdentifier>
      </urn4:Header>
      <urn4:Body>
        <urn4:RefusalByCustoms>
          <urn4:Attributes>
            <urn4:DateAndTimeOfIssuance>2023-06-24T18:27:14</urn4:DateAndTimeOfIssuance>
          </urn4:Attributes>
          <urn4:ConsigneeTrader language="en">
            <urn4:Traderid>GBWK000612158</urn4:Traderid>
            <urn4:TraderName>Chaz's Cigars</urn4:TraderName>
            <urn4:StreetName>The Street</urn4:StreetName>
            <urn4:Postcode>MC232</urn4:Postcode>
            <urn4:City>Happy Town</urn4:City>
            <urn4:EoriNumber>91</urn4:EoriNumber>
          </urn4:ConsigneeTrader>
          <urn4:ExportPlaceCustomsOffice>
            <urn4:ReferenceNumber>FR883393</urn4:ReferenceNumber>
          </urn4:ExportPlaceCustomsOffice>
          <urn4:ExportCrossCheckingDiagnoses>
            <urn4:LocalReferenceNumber>lrnie8155755329</urn4:LocalReferenceNumber>
            <urn4:DocumentReferenceNumber>123</urn4:DocumentReferenceNumber>
            <urn4:Diagnosis>
              <urn4:AdministrativeReferenceCode>23XI00000000000056341</urn4:AdministrativeReferenceCode>
              <urn4:BodyRecordUniqueReference>1</urn4:BodyRecordUniqueReference>
              <urn4:DiagnosisCode>5</urn4:DiagnosisCode>
            </urn4:Diagnosis>
          </urn4:ExportCrossCheckingDiagnoses>
          <urn4:Rejection>
            <urn4:RejectionDateAndTime>2023-06-22T02:02:49</urn4:RejectionDateAndTime>
            <urn4:RejectionReasonCode>4</urn4:RejectionReasonCode>
          </urn4:Rejection>
          <urn4:CEadVal>
            <urn4:AdministrativeReferenceCode>23XI00000000000056341</urn4:AdministrativeReferenceCode>
            <urn4:SequenceNumber>1</urn4:SequenceNumber>
          </urn4:CEadVal>
          <urn4:NEadSub>
            <urn4:LocalReferenceNumber>lrnie8155755329</urn4:LocalReferenceNumber>
          </urn4:NEadSub>
        </urn4:RefusalByCustoms>
      </urn4:Body>
    </urn4:IE839>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.FR\",\"DateOfPreparation\":\"2024-06-26\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"XI004322\",\"CorrelationIdentifier\":\"6dddas1231ff3a678fefffff3233\"},\"Body\":{\"RefusalByCustoms\":{\"AttributesValue\":{\"DateAndTimeOfIssuance\":\"2023-06-24T18:27:14\"},\"ConsigneeTrader\":{\"Traderid\":\"GBWK000612158\",\"TraderName\":\"Chaz's Cigars\",\"StreetName\":\"The Street\",\"Postcode\":\"MC232\",\"City\":\"Happy Town\",\"EoriNumber\":\"91\",\"attributes\":{\"@language\":\"en\"}},\"ExportPlaceCustomsOffice\":{\"ReferenceNumber\":\"FR883393\"},\"ExportCrossCheckingDiagnoses\":{\"LocalReferenceNumber\":\"lrnie8155755329\",\"DocumentReferenceNumber\":\"123\",\"Diagnosis\":[{\"AdministrativeReferenceCode\":\"23XI00000000000056341\",\"BodyRecordUniqueReference\":\"1\",\"DiagnosisCode\":\"5\"}]},\"Rejection\":{\"RejectionDateAndTime\":\"2023-06-22T02:02:49\",\"RejectionReasonCode\":\"4\"},\"CEadVal\":[{\"AdministrativeReferenceCode\":\"23XI00000000000056341\",\"SequenceNumber\":\"1\"}],\"NEadSub\":{\"LocalReferenceNumber\":\"lrnie8155755329\"}}}}")
}
