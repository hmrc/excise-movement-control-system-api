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

object IE829TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn2:IE829 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                xmlns:urn2="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.01">
      <urn2:Header>
        <urn:MessageSender>NDEA.XI</urn:MessageSender>
        <urn:MessageRecipient>NDEA.AT</urn:MessageRecipient>
        <urn:DateOfPreparation>2023-06-26</urn:DateOfPreparation>
        <urn:TimeOfPreparation>09:15:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>XI004321B</urn:MessageIdentifier>
        <urn:CorrelationIdentifier>6dddas342231ff3a67888bbcedec3435</urn:CorrelationIdentifier>
      </urn2:Header>
      <urn2:Body>
        <urn2:NotificationOfAcceptedExport>
          <urn2:Attributes>
            <urn2:DateAndTimeOfIssuance>2024-06-26T09:14:54</urn2:DateAndTimeOfIssuance>
          </urn2:Attributes>
          <urn2:ConsigneeTrader language="en">
            <urn2:Traderid>GBWK000612157</urn2:Traderid>
            <urn2:TraderName>Whale Oil Lamps Co.</urn2:TraderName>
            <urn2:StreetName>The Street</urn2:StreetName>
            <urn2:Postcode>MC232</urn2:Postcode>
            <urn2:City>Happy Town</urn2:City>
            <urn2:EoriNumber>7</urn2:EoriNumber>
          </urn2:ConsigneeTrader>
          <urn2:ExciseMovementEad>
            <urn2:AdministrativeReferenceCode>23XI00000000000056339</urn2:AdministrativeReferenceCode>
            <urn2:SequenceNumber>1</urn2:SequenceNumber>
          </urn2:ExciseMovementEad>
          <urn2:ExciseMovementEad>
            <urn2:AdministrativeReferenceCode>23XI00000000000056340</urn2:AdministrativeReferenceCode>
            <urn2:SequenceNumber>1</urn2:SequenceNumber>
          </urn2:ExciseMovementEad>
          <urn2:ExportPlaceCustomsOffice>
            <urn2:ReferenceNumber>AT633734</urn2:ReferenceNumber>
          </urn2:ExportPlaceCustomsOffice>
          <urn2:ExportAcceptance>
            <urn2:ReferenceNumberOfSenderCustomsOffice>AT324234</urn2:ReferenceNumberOfSenderCustomsOffice>
            <urn2:IdentificationOfSenderCustomsOfficer>84884</urn2:IdentificationOfSenderCustomsOfficer>
            <urn2:DateOfAcceptance>2023-06-26</urn2:DateOfAcceptance>
            <urn2:DocumentReferenceNumber>123123vmnfhsdf3AT</urn2:DocumentReferenceNumber>
          </urn2:ExportAcceptance>
        </urn2:NotificationOfAcceptedExport>
      </urn2:Body>
    </urn2:IE829>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.AT\",\"DateOfPreparation\":\"2023-06-26\",\"TimeOfPreparation\":\"09:15:33\",\"MessageIdentifier\":\"XI004321B\",\"CorrelationIdentifier\":\"6dddas342231ff3a67888bbcedec3435\"},\"Body\":{\"NotificationOfAcceptedExport\":{\"AttributesValue\":{\"DateAndTimeOfIssuance\":\"2024-06-26T09:14:54\"},\"ConsigneeTrader\":{\"Traderid\":\"GBWK000612157\",\"TraderName\":\"Whale Oil Lamps Co.\",\"StreetName\":\"The Street\",\"Postcode\":\"MC232\",\"City\":\"Happy Town\",\"EoriNumber\":\"7\",\"attributes\":{\"@language\":\"en\"}},\"ExciseMovementEad\":[{\"AdministrativeReferenceCode\":\"23XI00000000000056339\",\"SequenceNumber\":\"1\"},{\"AdministrativeReferenceCode\":\"23XI00000000000056340\",\"SequenceNumber\":\"1\"}],\"ExportPlaceCustomsOffice\":{\"ReferenceNumber\":\"AT633734\"},\"ExportAcceptance\":{\"ReferenceNumberOfSenderCustomsOffice\":\"AT324234\",\"IdentificationOfSenderCustomsOfficer\":\"84884\",\"DateOfAcceptance\":\"2023-06-26\",\"DocumentReferenceNumber\":\"123123vmnfhsdf3AT\"}}}}")
}
