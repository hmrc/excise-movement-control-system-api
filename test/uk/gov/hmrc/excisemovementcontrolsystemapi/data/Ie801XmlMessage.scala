/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.data

object Ie801XmlMessage {

  lazy val IE801 =
    <urn:IE801
        xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.01"
        xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn:Header>
        <urn1:MessageSender>token</urn1:MessageSender>
        <urn1:MessageRecipient>token</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2008-09-29</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>00:18:33</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>token</urn1:MessageIdentifier>
        <!--Optional:-->
        <urn1:CorrelationIdentifier>token</urn1:CorrelationIdentifier>
      </urn:Header>
      <urn:Body>
        <urn:EADESADContainer>
          <!--Optional:-->
          <urn:ConsigneeTrader language="to">
            <!--Optional:-->
            <urn:Traderid>token</urn:Traderid>
            <urn:TraderName>token</urn:TraderName>
            <urn:StreetName>token</urn:StreetName>
            <!--Optional:-->
            <urn:StreetNumber>token</urn:StreetNumber>
            <urn:Postcode>token</urn:Postcode>
            <urn:City>token</urn:City>
            <!--Optional:-->
            <urn:EoriNumber>token</urn:EoriNumber>
          </urn:ConsigneeTrader>
          <urn:ExciseMovement>
            <urn:AdministrativeReferenceCode>tokentokentokentokent</urn:AdministrativeReferenceCode>
            <urn:DateAndTimeOfValidationOfEadEsad>2006-08-19T18:27:14+01:00</urn:DateAndTimeOfValidationOfEadEsad>
          </urn:ExciseMovement>
          <urn:ConsignorTrader language="to">
            <urn:TraderExciseNumber>tokentokentok</urn:TraderExciseNumber>
            <urn:TraderName>token</urn:TraderName>
            <urn:StreetName>token</urn:StreetName>
            <!--Optional:-->
            <urn:StreetNumber>token</urn:StreetNumber>
            <urn:Postcode>token</urn:Postcode>
            <urn:City>token</urn:City>
          </urn:ConsignorTrader>
          <!--Optional:-->
          <urn:PlaceOfDispatchTrader language="to">
            <!--Optional:-->
            <urn:ReferenceOfTaxWarehouse>tokentokentok</urn:ReferenceOfTaxWarehouse>
            <!--Optional:-->
            <urn:TraderName>token</urn:TraderName>
            <!--Optional:-->
            <urn:StreetName>token</urn:StreetName>
            <!--Optional:-->
            <urn:StreetNumber>token</urn:StreetNumber>
            <!--Optional:-->
            <urn:Postcode>token</urn:Postcode>
            <!--Optional:-->
            <urn:City>token</urn:City>
          </urn:PlaceOfDispatchTrader>
          <!--Optional:-->
          <urn:DispatchImportOffice>
            <urn:ReferenceNumber>tokentok</urn:ReferenceNumber>
          </urn:DispatchImportOffice>
          <!--Optional:-->
          <urn:ComplementConsigneeTrader>
            <urn:MemberStateCode>to</urn:MemberStateCode>
            <!--Optional:-->
            <urn:SerialNumberOfCertificateOfExemption>token</urn:SerialNumberOfCertificateOfExemption>
          </urn:ComplementConsigneeTrader>
          <!--Optional:-->
          <urn:DeliveryPlaceTrader language="to">
            <!--Optional:-->
            <urn:Traderid>token</urn:Traderid>
            <!--Optional:-->
            <urn:TraderName>token</urn:TraderName>
            <!--Optional:-->
            <urn:StreetName>token</urn:StreetName>
            <!--Optional:-->
            <urn:StreetNumber>token</urn:StreetNumber>
            <!--Optional:-->
            <urn:Postcode>token</urn:Postcode>
            <!--Optional:-->
            <urn:City>token</urn:City>
          </urn:DeliveryPlaceTrader>
          <!--Optional:-->
          <urn:DeliveryPlaceCustomsOffice>
            <urn:ReferenceNumber>tokentok</urn:ReferenceNumber>
          </urn:DeliveryPlaceCustomsOffice>
          <urn:CompetentAuthorityDispatchOffice>
            <urn:ReferenceNumber>tokentok</urn:ReferenceNumber>
          </urn:CompetentAuthorityDispatchOffice>
          <!--Optional:-->
          <urn:TransportArrangerTrader language="to">
            <!--Optional:-->
            <urn:VatNumber>token</urn:VatNumber>
            <urn:TraderName>token</urn:TraderName>
            <urn:StreetName>token</urn:StreetName>
            <!--Optional:-->
            <urn:StreetNumber>token</urn:StreetNumber>
            <urn:Postcode>token</urn:Postcode>
            <urn:City>token</urn:City>
          </urn:TransportArrangerTrader>
          <!--Optional:-->
          <urn:FirstTransporterTrader language="to">
            <!--Optional:-->
            <urn:VatNumber>token</urn:VatNumber>
            <urn:TraderName>token</urn:TraderName>
            <urn:StreetName>token</urn:StreetName>
            <!--Optional:-->
            <urn:StreetNumber>token</urn:StreetNumber>
            <urn:Postcode>token</urn:Postcode>
            <urn:City>token</urn:City>
          </urn:FirstTransporterTrader>
          <!--0 to 9 repetitions:-->
          <urn:DocumentCertificate>
            <!--Optional:-->
            <urn:DocumentType>toke</urn:DocumentType>
            <!--Optional:-->
            <urn:DocumentReference>token</urn:DocumentReference>
            <!--Optional:-->
            <urn:DocumentDescription language="to">token</urn:DocumentDescription>
            <!--Optional:-->
            <urn:ReferenceOfDocument language="to">token</urn:ReferenceOfDocument>
          </urn:DocumentCertificate>
          <urn:EadEsad>
            <urn:LocalReferenceNumber>token</urn:LocalReferenceNumber>
            <urn:InvoiceNumber>token</urn:InvoiceNumber>
            <!--Optional:-->
            <urn:InvoiceDate>2009-05-16</urn:InvoiceDate>
            <urn:OriginTypeCode>2</urn:OriginTypeCode>
            <urn:DateOfDispatch>2002-11-05+00:00</urn:DateOfDispatch>
            <!--Optional:-->
            <urn:TimeOfDispatch>16:46:32+01:00</urn:TimeOfDispatch>
            <!--Optional:-->
            <urn:UpstreamArc>tokentokentokentokent</urn:UpstreamArc>
            <!--0 to 9 repetitions:-->
            <urn:ImportSad>
              <urn:ImportSadNumber>token</urn:ImportSadNumber>
            </urn:ImportSad>
          </urn:EadEsad>
          <urn:HeaderEadEsad>
            <urn:SequenceNumber>to</urn:SequenceNumber>
            <urn:DateAndTimeOfUpdateValidation>2016-02-20T14:56:29+00:00</urn:DateAndTimeOfUpdateValidation>
            <urn:DestinationTypeCode>7</urn:DestinationTypeCode>
            <urn:JourneyTime>tok</urn:JourneyTime>
            <urn:TransportArrangement>1</urn:TransportArrangement>
          </urn:HeaderEadEsad>
          <urn:TransportMode>
            <urn:TransportModeCode>to</urn:TransportModeCode>
            <!--Optional:-->
            <urn:ComplementaryInformation language="to">token</urn:ComplementaryInformation>
          </urn:TransportMode>
          <urn:MovementGuarantee>
            <urn:GuarantorTypeCode>234</urn:GuarantorTypeCode>
            <!--0 to 2 repetitions:-->
            <urn:GuarantorTrader language="to">
              <!--Optional:-->
              <urn:TraderExciseNumber>tokentokentok</urn:TraderExciseNumber>
              <!--Optional:-->
              <urn:TraderName>token</urn:TraderName>
              <!--Optional:-->
              <urn:StreetName>token</urn:StreetName>
              <!--Optional:-->
              <urn:StreetNumber>token</urn:StreetNumber>
              <!--Optional:-->
              <urn:City>token</urn:City>
              <!--Optional:-->
              <urn:Postcode>token</urn:Postcode>
              <!--Optional:-->
              <urn:VatNumber>token</urn:VatNumber>
            </urn:GuarantorTrader>
          </urn:MovementGuarantee>
          <!--1 or more repetitions:-->
          <urn:BodyEadEsad>
            <urn:BodyRecordUniqueReference>tok</urn:BodyRecordUniqueReference>
            <urn:ExciseProductCode>toke</urn:ExciseProductCode>
            <urn:CnCode>tokentok</urn:CnCode>
            <urn:Quantity>1000.00000000000</urn:Quantity>
            <urn:GrossMass>1000.000000000000</urn:GrossMass>
            <urn:NetMass>1000.000000000000</urn:NetMass>
            <!--Optional:-->
            <urn:AlcoholicStrengthByVolumeInPercentage>1000.00</urn:AlcoholicStrengthByVolumeInPercentage>
            <!--Optional:-->
            <urn:DegreePlato>1000.00</urn:DegreePlato>
            <!--Optional:-->
            <urn:FiscalMark language="to">token</urn:FiscalMark>
            <!--Optional:-->
            <urn:FiscalMarkUsedFlag>0</urn:FiscalMarkUsedFlag>
            <!--Optional:-->
            <urn:DesignationOfOrigin language="to">token</urn:DesignationOfOrigin>
            <!--Optional:-->
            <urn:SizeOfProducer>token</urn:SizeOfProducer>
            <!--Optional:-->
            <urn:Density>1000.00</urn:Density>
            <!--Optional:-->
            <urn:CommercialDescription language="to">token</urn:CommercialDescription>
            <!--Optional:-->
            <urn:BrandNameOfProducts language="to">token</urn:BrandNameOfProducts>
            <!--Optional:-->
            <urn:MaturationPeriodOrAgeOfProducts>token</urn:MaturationPeriodOrAgeOfProducts>
            <!--1 to 99 repetitions:-->
            <urn:Package>
              <urn:KindOfPackages>to</urn:KindOfPackages>
              <!--Optional:-->
              <urn:NumberOfPackages>token</urn:NumberOfPackages>
              <!--Optional:-->
              <urn:ShippingMarks>token</urn:ShippingMarks>
              <!--Optional:-->
              <urn:CommercialSealIdentification>token</urn:CommercialSealIdentification>
              <!--Optional:-->
              <urn:SealInformation language="to">token</urn:SealInformation>
            </urn:Package>
            <!--Optional:-->
            <urn:WineProduct>
              <urn:WineProductCategory>4</urn:WineProductCategory>
              <!--Optional:-->
              <urn:WineGrowingZoneCode>to</urn:WineGrowingZoneCode>
              <!--Optional:-->
              <urn:ThirdCountryOfOrigin>to</urn:ThirdCountryOfOrigin>
              <!--Optional:-->
              <urn:OtherInformation language="to">token</urn:OtherInformation>
              <!--0 to 99 repetitions:-->
              <urn:WineOperation>
                <urn:WineOperationCode>to</urn:WineOperationCode>
              </urn:WineOperation>
            </urn:WineProduct>
          </urn:BodyEadEsad>
          <!--1 to 99 repetitions:-->
          <urn:TransportDetails>
            <urn:TransportUnitCode>to</urn:TransportUnitCode>
            <!--Optional:-->
            <urn:IdentityOfTransportUnits>token</urn:IdentityOfTransportUnits>
            <!--Optional:-->
            <urn:CommercialSealIdentification>token</urn:CommercialSealIdentification>
            <!--Optional:-->
            <urn:ComplementaryInformation language="to">token</urn:ComplementaryInformation>
            <!--Optional:-->
            <urn:SealInformation language="to">token</urn:SealInformation>
          </urn:TransportDetails>
        </urn:EADESADContainer>
      </urn:Body>
    </urn:IE801>

}
