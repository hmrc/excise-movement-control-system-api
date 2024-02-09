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

object IE813TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn1:IE813 xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE813:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn1:Header>
        <urn:MessageSender>NDEA.XI</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2012-01-08</urn:DateOfPreparation>
        <urn:TimeOfPreparation>13:43:21</urn:TimeOfPreparation>
        <urn:MessageIdentifier>19fc52f6-4b67-4fde-afab-ebedaadfb3f2</urn:MessageIdentifier>
        <!--Optional:-->
        <urn:CorrelationIdentifier>3e5a6399-0152-4883-9d37-9d11e635ddc5</urn:CorrelationIdentifier>
      </urn1:Header>
      <urn1:Body>
        <urn1:ChangeOfDestination>
          <urn1:Attributes>
            <!--Optional:-->
            <urn1:DateAndTimeOfValidationOfChangeOfDestination>2001-08-29T10:32:42</urn1:DateAndTimeOfValidationOfChangeOfDestination>
          </urn1:Attributes>
          <!--Optional:-->
          <urn1:NewTransportArrangerTrader language="to">
            <!--Optional:-->
            <urn1:VatNumber>token</urn1:VatNumber>
            <urn1:TraderName>token</urn1:TraderName>
            <urn1:StreetName>token</urn1:StreetName>
            <!--Optional:-->
            <urn1:StreetNumber>token</urn1:StreetNumber>
            <urn1:Postcode>token</urn1:Postcode>
            <urn1:City>token</urn1:City>
          </urn1:NewTransportArrangerTrader>
          <urn1:UpdateEadEsad>
            <urn1:AdministrativeReferenceCode>23XI00000000000000021</urn1:AdministrativeReferenceCode>
            <!--Optional:-->
            <urn1:JourneyTime>D01</urn1:JourneyTime>
            <!--Optional:-->
            <urn1:ChangedTransportArrangement>4</urn1:ChangedTransportArrangement>
            <!--Optional:-->
            <urn1:SequenceNumber>2</urn1:SequenceNumber>
            <!--Optional:-->
            <urn1:InvoiceDate>2019-05-04</urn1:InvoiceDate>
            <!--Optional:-->
            <urn1:InvoiceNumber>token</urn1:InvoiceNumber>
            <!--Optional:-->
            <urn1:TransportModeCode>4</urn1:TransportModeCode>
            <!--Optional:-->
            <urn1:ComplementaryInformation language="to">token</urn1:ComplementaryInformation>
          </urn1:UpdateEadEsad>
          <urn1:DestinationChanged>
            <urn1:DestinationTypeCode>3</urn1:DestinationTypeCode>
            <!--Optional:-->
            <urn1:NewConsigneeTrader language="to">
              <!--Optional:-->
              <urn1:Traderid>GBWK002281067</urn1:Traderid>
              <urn1:TraderName>token</urn1:TraderName>
              <urn1:StreetName>token</urn1:StreetName>
              <!--Optional:-->
              <urn1:StreetNumber>token</urn1:StreetNumber>
              <urn1:Postcode>token</urn1:Postcode>
              <urn1:City>token</urn1:City>
              <!--Optional:-->
              <urn1:EoriNumber>token</urn1:EoriNumber>
            </urn1:NewConsigneeTrader>
            <!--Optional:-->
            <urn1:DeliveryPlaceTrader language="to">
              <!--Optional:-->
              <urn1:Traderid>GBWK005981023</urn1:Traderid>
              <!--Optional:-->
              <urn1:TraderName>token</urn1:TraderName>
              <!--Optional:-->
              <urn1:StreetName>token</urn1:StreetName>
              <!--Optional:-->
              <urn1:StreetNumber>token</urn1:StreetNumber>
              <!--Optional:-->
              <urn1:Postcode>token</urn1:Postcode>
              <!--Optional:-->
              <urn1:City>token</urn1:City>
            </urn1:DeliveryPlaceTrader>
            <!--Optional:-->
            <urn1:DeliveryPlaceCustomsOffice>
              <urn1:ReferenceNumber>GB004049</urn1:ReferenceNumber>
            </urn1:DeliveryPlaceCustomsOffice>
            <!--Optional:-->
            <urn1:MovementGuarantee>
              <urn1:GuarantorTypeCode>12</urn1:GuarantorTypeCode>
              <!--0 to 2 repetitions:-->
              <urn1:GuarantorTrader language="to">
                <!--Optional:-->
                <urn1:TraderExciseNumber>GBWK001281028</urn1:TraderExciseNumber>
                <!--Optional:-->
                <urn1:TraderName>token</urn1:TraderName>
                <!--Optional:-->
                <urn1:StreetName>token</urn1:StreetName>
                <!--Optional:-->
                <urn1:StreetNumber>token</urn1:StreetNumber>
                <!--Optional:-->
                <urn1:City>token</urn1:City>
                <!--Optional:-->
                <urn1:Postcode>token</urn1:Postcode>
                <!--Optional:-->
                <urn1:VatNumber>token</urn1:VatNumber>
              </urn1:GuarantorTrader>
            </urn1:MovementGuarantee>
          </urn1:DestinationChanged>
          <!--Optional:-->
          <urn1:NewTransporterTrader language="to">
            <!--Optional:-->
            <urn1:VatNumber>token</urn1:VatNumber>
            <urn1:TraderName>token</urn1:TraderName>
            <urn1:StreetName>token</urn1:StreetName>
            <!--Optional:-->
            <urn1:StreetNumber>token</urn1:StreetNumber>
            <urn1:Postcode>token</urn1:Postcode>
            <urn1:City>token</urn1:City>
          </urn1:NewTransporterTrader>
          <!--0 to 99 repetitions:-->
          <urn1:TransportDetails>
            <urn1:TransportUnitCode>12</urn1:TransportUnitCode>
            <!--Optional:-->
            <urn1:IdentityOfTransportUnits>token</urn1:IdentityOfTransportUnits>
            <!--Optional:-->
            <urn1:CommercialSealIdentification>token</urn1:CommercialSealIdentification>
            <!--Optional:-->
            <urn1:ComplementaryInformation language="to">token</urn1:ComplementaryInformation>
            <!--Optional:-->
            <urn1:SealInformation language="to">token</urn1:SealInformation>
          </urn1:TransportDetails>
        </urn1:ChangeOfDestination>
      </urn1:Body>
    </urn1:IE813>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2012-01-08\",\"TimeOfPreparation\":\"13:43:21\",\"MessageIdentifier\":\"19fc52f6-4b67-4fde-afab-ebedaadfb3f2\",\"CorrelationIdentifier\":\"3e5a6399-0152-4883-9d37-9d11e635ddc5\"},\"Body\":{\"ChangeOfDestination\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfChangeOfDestination\":\"2001-08-29T10:32:42\"},\"NewTransportArrangerTrader\":{\"VatNumber\":\"token\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"UpdateEadEsad\":{\"AdministrativeReferenceCode\":\"23XI00000000000000021\",\"JourneyTime\":\"D01\",\"ChangedTransportArrangement\":\"4\",\"SequenceNumber\":\"2\",\"InvoiceDate\":\"2019-05-04\",\"InvoiceNumber\":\"token\",\"TransportModeCode\":\"4\",\"ComplementaryInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}}},\"DestinationChanged\":{\"DestinationTypeCode\":\"3\",\"NewConsigneeTrader\":{\"Traderid\":\"GBWK002281067\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"EoriNumber\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"DeliveryPlaceTrader\":{\"Traderid\":\"GBWK005981023\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"DeliveryPlaceCustomsOffice\":{\"ReferenceNumber\":\"GB004049\"},\"MovementGuarantee\":{\"GuarantorTypeCode\":\"12\",\"GuarantorTrader\":[{\"TraderExciseNumber\":\"GBWK001281028\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"City\":\"token\",\"Postcode\":\"token\",\"VatNumber\":\"token\",\"attributes\":{\"@language\":\"to\"}}]}},\"NewTransporterTrader\":{\"VatNumber\":\"token\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"TransportDetails\":[{\"TransportUnitCode\":\"12\",\"IdentityOfTransportUnits\":\"token\",\"CommercialSealIdentification\":\"token\",\"ComplementaryInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"SealInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}}}]}}}")
}
