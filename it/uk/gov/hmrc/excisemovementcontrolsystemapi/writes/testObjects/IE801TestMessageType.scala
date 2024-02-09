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

object IE801TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn1:IE801 xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.01"
                xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn1:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.AT</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-06-22</tms:DateOfPreparation>
        <tms:TimeOfPreparation>12:37:08.755</tms:TimeOfPreparation>
        <tms:MessageIdentifier>XI000002</tms:MessageIdentifier>
      </urn1:Header>
      <urn1:Body>
        <urn1:EADESADContainer>
          <urn1:ConsigneeTrader language="en">
            <urn1:Traderid>AT00000602078</urn1:Traderid>
            <urn1:TraderName>AFOR KALE LTD</urn1:TraderName>
            <urn1:StreetName>The Street</urn1:StreetName>
            <urn1:Postcode>AT123</urn1:Postcode>
            <urn1:City>The City</urn1:City>
          </urn1:ConsigneeTrader>
          <urn1:ExciseMovement>
            <urn1:AdministrativeReferenceCode>23XI00000000000000013</urn1:AdministrativeReferenceCode>
            <urn1:DateAndTimeOfValidationOfEadEsad>2023-06-22T11:37:10.345739396</urn1:DateAndTimeOfValidationOfEadEsad>
          </urn1:ExciseMovement>
          <urn1:ConsignorTrader language="en">
            <urn1:TraderExciseNumber>XIWK000467015</urn1:TraderExciseNumber>
            <urn1:TraderName>Clarkys Eagles</urn1:TraderName>
            <urn1:StreetName>Happy Street</urn1:StreetName>
            <urn1:Postcode>BT1 1BG</urn1:Postcode>
            <urn1:City>The City</urn1:City>
          </urn1:ConsignorTrader>
          <urn1:PlaceOfDispatchTrader language="en">
            <urn1:ReferenceOfTaxWarehouse>XI00000467014</urn1:ReferenceOfTaxWarehouse>
          </urn1:PlaceOfDispatchTrader>
          <urn1:DeliveryPlaceTrader language="en">
            <urn1:Traderid>AT00000602078</urn1:Traderid>
            <urn1:TraderName>METEST BOND
              &amp;
              STTSTGE</urn1:TraderName>
            <urn1:StreetName>WHITETEST ROAD METEST CITY ESTATE</urn1:StreetName>
            <urn1:Postcode>BN2 4KX</urn1:Postcode>
            <urn1:City>STTEST,KENT</urn1:City>
          </urn1:DeliveryPlaceTrader>
          <urn1:CompetentAuthorityDispatchOffice>
            <urn1:ReferenceNumber>GB004098</urn1:ReferenceNumber>
          </urn1:CompetentAuthorityDispatchOffice>
          <urn1:EadEsad>
            <urn1:LocalReferenceNumber>lrnie8156540856</urn1:LocalReferenceNumber>
            <urn1:InvoiceNumber>INVOICE001</urn1:InvoiceNumber>
            <urn1:InvoiceDate>2018-04-04</urn1:InvoiceDate>
            <urn1:OriginTypeCode>1</urn1:OriginTypeCode>
            <urn1:DateOfDispatch>2021-12-02</urn1:DateOfDispatch>
            <urn1:TimeOfDispatch>22:37:00</urn1:TimeOfDispatch>
          </urn1:EadEsad>
          <urn1:HeaderEadEsad>
            <urn1:SequenceNumber>1</urn1:SequenceNumber>
            <urn1:DateAndTimeOfUpdateValidation>2023-06-22T11:37:10.345801029</urn1:DateAndTimeOfUpdateValidation>
            <urn1:DestinationTypeCode>1</urn1:DestinationTypeCode>
            <urn1:JourneyTime>D01</urn1:JourneyTime>
            <urn1:TransportArrangement>1</urn1:TransportArrangement>
          </urn1:HeaderEadEsad>
          <urn1:TransportMode>
            <urn1:TransportModeCode>1</urn1:TransportModeCode>
          </urn1:TransportMode>
          <urn1:MovementGuarantee>
            <urn1:GuarantorTypeCode>1</urn1:GuarantorTypeCode>
          </urn1:MovementGuarantee>
          <urn1:BodyEadEsad>
            <urn1:BodyRecordUniqueReference>1</urn1:BodyRecordUniqueReference>
            <urn1:ExciseProductCode>E410</urn1:ExciseProductCode>
            <urn1:CnCode>27101231</urn1:CnCode>
            <urn1:Quantity>100.000</urn1:Quantity>
            <urn1:GrossMass>100.00</urn1:GrossMass>
            <urn1:NetMass>90.00</urn1:NetMass>
            <urn1:Density>10.00</urn1:Density>
            <urn1:Package>
              <urn1:KindOfPackages>BH</urn1:KindOfPackages>
              <urn1:NumberOfPackages>2</urn1:NumberOfPackages>
              <urn1:ShippingMarks>Subhasis Swain1</urn1:ShippingMarks>
            </urn1:Package>
            <urn1:Package>
              <urn1:KindOfPackages>BH</urn1:KindOfPackages>
              <urn1:NumberOfPackages>2</urn1:NumberOfPackages>
              <urn1:ShippingMarks>Subhasis Swain 2</urn1:ShippingMarks>
            </urn1:Package>
          </urn1:BodyEadEsad>
          <urn1:TransportDetails>
            <urn1:TransportUnitCode>1</urn1:TransportUnitCode>
            <urn1:IdentityOfTransportUnits>Transformers robots in disguise</urn1:IdentityOfTransportUnits>
          </urn1:TransportDetails>
          <urn1:TransportDetails>
            <urn1:TransportUnitCode>2</urn1:TransportUnitCode>
            <urn1:IdentityOfTransportUnits>MACHINES</urn1:IdentityOfTransportUnits>
          </urn1:TransportDetails>
          <urn1:TransportDetails>
            <urn1:TransportUnitCode>3</urn1:TransportUnitCode>
            <urn1:IdentityOfTransportUnits>MORE MACHINES</urn1:IdentityOfTransportUnits>
          </urn1:TransportDetails>
        </urn1:EADESADContainer>
      </urn1:Body>
    </urn1:IE801>


  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.AT\",\"DateOfPreparation\":\"2023-06-22\",\"TimeOfPreparation\":\"12:37:08.755\",\"MessageIdentifier\":\"XI000002\"},\"Body\":{\"EADESADContainer\":{\"ConsigneeTrader\":{\"Traderid\":\"AT00000602078\",\"TraderName\":\"AFOR KALE LTD\",\"StreetName\":\"The Street\",\"Postcode\":\"AT123\",\"City\":\"The City\",\"attributes\":{\"@language\":\"en\"}},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000013\",\"DateAndTimeOfValidationOfEadEsad\":\"2023-06-22T11:37:10.345739396\"},\"ConsignorTrader\":{\"TraderExciseNumber\":\"XIWK000467015\",\"TraderName\":\"Clarkys Eagles\",\"StreetName\":\"Happy Street\",\"Postcode\":\"BT1 1BG\",\"City\":\"The City\",\"attributes\":{\"@language\":\"en\"}},\"PlaceOfDispatchTrader\":{\"ReferenceOfTaxWarehouse\":\"XI00000467014\",\"attributes\":{\"@language\":\"en\"}},\"DeliveryPlaceTrader\":{\"Traderid\":\"AT00000602078\",\"TraderName\":\"METEST BOND\\n              &\\n              STTSTGE\",\"StreetName\":\"WHITETEST ROAD METEST CITY ESTATE\",\"Postcode\":\"BN2 4KX\",\"City\":\"STTEST,KENT\",\"attributes\":{\"@language\":\"en\"}},\"CompetentAuthorityDispatchOffice\":{\"ReferenceNumber\":\"GB004098\"},\"DocumentCertificate\":[],\"EadEsad\":{\"LocalReferenceNumber\":\"lrnie8156540856\",\"InvoiceNumber\":\"INVOICE001\",\"InvoiceDate\":\"2018-04-04\",\"OriginTypeCode\":\"1\",\"DateOfDispatch\":\"2021-12-02\",\"TimeOfDispatch\":\"22:37:00\",\"ImportSad\":[]},\"HeaderEadEsad\":{\"SequenceNumber\":\"1\",\"DateAndTimeOfUpdateValidation\":\"2023-06-22T11:37:10.345801029\",\"DestinationTypeCode\":\"1\",\"JourneyTime\":\"D01\",\"TransportArrangement\":\"1\"},\"TransportMode\":{\"TransportModeCode\":\"1\"},\"MovementGuarantee\":{\"GuarantorTypeCode\":\"1\",\"GuarantorTrader\":[]},\"BodyEadEsad\":[{\"BodyRecordUniqueReference\":\"1\",\"ExciseProductCode\":\"E410\",\"CnCode\":\"27101231\",\"Quantity\":100,\"GrossMass\":100,\"NetMass\":90,\"Density\":10,\"PackageValue\":[{\"KindOfPackages\":\"BH\",\"NumberOfPackages\":\"2\",\"ShippingMarks\":\"Subhasis Swain1\"},{\"KindOfPackages\":\"BH\",\"NumberOfPackages\":\"2\",\"ShippingMarks\":\"Subhasis Swain 2\"}]}],\"TransportDetails\":[{\"TransportUnitCode\":\"1\",\"IdentityOfTransportUnits\":\"Transformers robots in disguise\"},{\"TransportUnitCode\":\"2\",\"IdentityOfTransportUnits\":\"MACHINES\"},{\"TransportUnitCode\":\"3\",\"IdentityOfTransportUnits\":\"MORE MACHINES\"}]}}}")
}
