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

import scala.xml.Elem

object NewMessagesXml {

  lazy val newMessageWithIE801: Elem = <ns:NewMessagesDataResponse
  xmlns:ns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/NewMessagesData/3"
  xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.01"
  xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
    <ns:Messages>
      <urn1:IE801>
        <urn1:Header>
          <urn:MessageSender>token</urn:MessageSender>
          <urn:MessageRecipient>token</urn:MessageRecipient>
          <urn:DateOfPreparation>2018-11-01</urn:DateOfPreparation>
          <urn:TimeOfPreparation>02:02:49+01:00</urn:TimeOfPreparation>
          <urn:MessageIdentifier>token</urn:MessageIdentifier>
          <!--Optional:-->
          <urn:CorrelationIdentifier>token</urn:CorrelationIdentifier>
        </urn1:Header>
        <urn1:Body>
          <urn1:EADESADContainer>
            <!--Optional:-->
            <urn1:ConsigneeTrader language="to">
              <!--Optional:-->
              <urn1:Traderid>token</urn1:Traderid>
              <urn1:TraderName>token</urn1:TraderName>
              <urn1:StreetName>token</urn1:StreetName>
              <!--Optional:-->
              <urn1:StreetNumber>token</urn1:StreetNumber>
              <urn1:Postcode>token</urn1:Postcode>
              <urn1:City>token</urn1:City>
              <!--Optional:-->
              <urn1:EoriNumber>token</urn1:EoriNumber>
            </urn1:ConsigneeTrader>
            <urn1:ExciseMovement>
              <urn1:AdministrativeReferenceCode>tokentokentokentokent</urn1:AdministrativeReferenceCode>
              <urn1:DateAndTimeOfValidationOfEadEsad>2002-11-05T08:01:03</urn1:DateAndTimeOfValidationOfEadEsad>
            </urn1:ExciseMovement>
            <urn1:ConsignorTrader language="to">
              <urn1:TraderExciseNumber>tokentokentok</urn1:TraderExciseNumber>
              <urn1:TraderName>token</urn1:TraderName>
              <urn1:StreetName>token</urn1:StreetName>
              <!--Optional:-->
              <urn1:StreetNumber>token</urn1:StreetNumber>
              <urn1:Postcode>token</urn1:Postcode>
              <urn1:City>token</urn1:City>
            </urn1:ConsignorTrader>
            <!--Optional:-->
            <urn1:PlaceOfDispatchTrader language="to">
              <!--Optional:-->
              <urn1:ReferenceOfTaxWarehouse>tokentokentok</urn1:ReferenceOfTaxWarehouse>
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
            </urn1:PlaceOfDispatchTrader>
            <!--Optional:-->
            <urn1:DispatchImportOffice>
              <urn1:ReferenceNumber>tokentok</urn1:ReferenceNumber>
            </urn1:DispatchImportOffice>
            <!--Optional:-->
            <urn1:ComplementConsigneeTrader>
              <urn1:MemberStateCode>to</urn1:MemberStateCode>
              <!--Optional:-->
              <urn1:SerialNumberOfCertificateOfExemption>token</urn1:SerialNumberOfCertificateOfExemption>
            </urn1:ComplementConsigneeTrader>
            <!--Optional:-->
            <urn1:DeliveryPlaceTrader language="to">
              <!--Optional:-->
              <urn1:Traderid>token</urn1:Traderid>
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
              <urn1:ReferenceNumber>tokentok</urn1:ReferenceNumber>
            </urn1:DeliveryPlaceCustomsOffice>
            <urn1:CompetentAuthorityDispatchOffice>
              <urn1:ReferenceNumber>tokentok</urn1:ReferenceNumber>
            </urn1:CompetentAuthorityDispatchOffice>
            <!--Optional:-->
            <urn1:TransportArrangerTrader language="to">
              <!--Optional:-->
              <urn1:VatNumber>token</urn1:VatNumber>
              <urn1:TraderName>token</urn1:TraderName>
              <urn1:StreetName>token</urn1:StreetName>
              <!--Optional:-->
              <urn1:StreetNumber>token</urn1:StreetNumber>
              <urn1:Postcode>token</urn1:Postcode>
              <urn1:City>token</urn1:City>
            </urn1:TransportArrangerTrader>
            <!--Optional:-->
            <urn1:FirstTransporterTrader language="to">
              <!--Optional:-->
              <urn1:VatNumber>token</urn1:VatNumber>
              <urn1:TraderName>token</urn1:TraderName>
              <urn1:StreetName>token</urn1:StreetName>
              <!--Optional:-->
              <urn1:StreetNumber>token</urn1:StreetNumber>
              <urn1:Postcode>token</urn1:Postcode>
              <urn1:City>token</urn1:City>
            </urn1:FirstTransporterTrader>
            <!--0 to 9 repetitions:-->
            <urn1:DocumentCertificate>
              <!--Optional:-->
              <urn1:DocumentType>toke</urn1:DocumentType>
              <!--Optional:-->
              <urn1:DocumentReference>token</urn1:DocumentReference>
              <!--Optional:-->
              <urn1:DocumentDescription language="to">token</urn1:DocumentDescription>
              <!--Optional:-->
              <urn1:ReferenceOfDocument language="to">token</urn1:ReferenceOfDocument>
            </urn1:DocumentCertificate>
            <urn1:EadEsad>
              <urn1:LocalReferenceNumber>token</urn1:LocalReferenceNumber>
              <urn1:InvoiceNumber>token</urn1:InvoiceNumber>
              <!--Optional:-->
              <urn1:InvoiceDate>2002-06-24+01:00</urn1:InvoiceDate>
              <urn1:OriginTypeCode>3</urn1:OriginTypeCode>
              <urn1:DateOfDispatch>2012-01-07</urn1:DateOfDispatch>
              <!--Optional:-->
              <urn1:TimeOfDispatch>09:44:59</urn1:TimeOfDispatch>
              <!--Optional:-->
              <urn1:UpstreamArc>tokentokentokentokent</urn1:UpstreamArc>
              <!--0 to 9 repetitions:-->
              <urn1:ImportSad>
                <urn1:ImportSadNumber>token</urn1:ImportSadNumber>
              </urn1:ImportSad>
            </urn1:EadEsad>
            <urn1:HeaderEadEsad>
              <urn1:SequenceNumber>to</urn1:SequenceNumber>
              <urn1:DateAndTimeOfUpdateValidation>2013-06-17T18:14:58</urn1:DateAndTimeOfUpdateValidation>
              <urn1:DestinationTypeCode>10</urn1:DestinationTypeCode>
              <urn1:JourneyTime>tok</urn1:JourneyTime>
              <urn1:TransportArrangement>4</urn1:TransportArrangement>
            </urn1:HeaderEadEsad>
            <urn1:TransportMode>
              <urn1:TransportModeCode>to</urn1:TransportModeCode>
              <!--Optional:-->
              <urn1:ComplementaryInformation language="to">token</urn1:ComplementaryInformation>
            </urn1:TransportMode>
            <urn1:MovementGuarantee>
              <urn1:GuarantorTypeCode>12</urn1:GuarantorTypeCode>
              <!--0 to 2 repetitions:-->
              <urn1:GuarantorTrader language="to">
                <!--Optional:-->
                <urn1:TraderExciseNumber>tokentokentok</urn1:TraderExciseNumber>
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
            <!--1 or more repetitions:-->
            <urn1:BodyEadEsad>
              <urn1:BodyRecordUniqueReference>tok</urn1:BodyRecordUniqueReference>
              <urn1:ExciseProductCode>toke</urn1:ExciseProductCode>
              <urn1:CnCode>tokentok</urn1:CnCode>
              <urn1:Quantity>1000.00000000000</urn1:Quantity>
              <urn1:GrossMass>1000.000000000000</urn1:GrossMass>
              <urn1:NetMass>1000.000000000000</urn1:NetMass>
              <!--Optional:-->
              <urn1:AlcoholicStrengthByVolumeInPercentage>1000.00</urn1:AlcoholicStrengthByVolumeInPercentage>
              <!--Optional:-->
              <urn1:DegreePlato>1000.00</urn1:DegreePlato>
              <!--Optional:-->
              <urn1:FiscalMark language="to">token</urn1:FiscalMark>
              <!--Optional:-->
              <urn1:FiscalMarkUsedFlag>1</urn1:FiscalMarkUsedFlag>
              <!--Optional:-->
              <urn1:DesignationOfOrigin language="to">token</urn1:DesignationOfOrigin>
              <!--Optional:-->
              <urn1:SizeOfProducer>token</urn1:SizeOfProducer>
              <!--Optional:-->
              <urn1:Density>1000.00</urn1:Density>
              <!--Optional:-->
              <urn1:CommercialDescription language="to">token</urn1:CommercialDescription>
              <!--Optional:-->
              <urn1:BrandNameOfProducts language="to">token</urn1:BrandNameOfProducts>
              <!--Optional:-->
              <urn1:MaturationPeriodOrAgeOfProducts>token</urn1:MaturationPeriodOrAgeOfProducts>
              <!--1 to 99 repetitions:-->
              <urn1:Package>
                <urn1:KindOfPackages>to</urn1:KindOfPackages>
                <!--Optional:-->
                <urn1:NumberOfPackages>token</urn1:NumberOfPackages>
                <!--Optional:-->
                <urn1:ShippingMarks>token</urn1:ShippingMarks>
                <!--Optional:-->
                <urn1:CommercialSealIdentification>token</urn1:CommercialSealIdentification>
                <!--Optional:-->
                <urn1:SealInformation language="to">token</urn1:SealInformation>
              </urn1:Package>
              <!--Optional:-->
              <urn1:WineProduct>
                <urn1:WineProductCategory>2</urn1:WineProductCategory>
                <!--Optional:-->
                <urn1:WineGrowingZoneCode>to</urn1:WineGrowingZoneCode>
                <!--Optional:-->
                <urn1:ThirdCountryOfOrigin>to</urn1:ThirdCountryOfOrigin>
                <!--Optional:-->
                <urn1:OtherInformation language="to">token</urn1:OtherInformation>
                <!--0 to 99 repetitions:-->
                <urn1:WineOperation>
                  <urn1:WineOperationCode>to</urn1:WineOperationCode>
                </urn1:WineOperation>
              </urn1:WineProduct>
            </urn1:BodyEadEsad>
            <!--1 to 99 repetitions:-->
            <urn1:TransportDetails>
              <urn1:TransportUnitCode>to</urn1:TransportUnitCode>
              <!--Optional:-->
              <urn1:IdentityOfTransportUnits>token</urn1:IdentityOfTransportUnits>
              <!--Optional:-->
              <urn1:CommercialSealIdentification>token</urn1:CommercialSealIdentification>
              <!--Optional:-->
              <urn1:ComplementaryInformation language="to">token</urn1:ComplementaryInformation>
              <!--Optional:-->
              <urn1:SealInformation language="to">token</urn1:SealInformation>
            </urn1:TransportDetails>
          </urn1:EADESADContainer>
        </urn1:Body>
      </urn1:IE801>
    </ns:Messages>
    <ns:CountOfMessagesAvailable>1</ns:CountOfMessagesAvailable>
  </ns:NewMessagesDataResponse>

  lazy val newMessageWith2IE801sXml: Elem = <ns:NewMessagesDataResponse
  xmlns:ns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/NewMessagesData/3"
  xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.01"
  xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
    <ns:Messages>
      <urn:IE801>
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
              <urn:LocalReferenceNumber>token1</urn:LocalReferenceNumber>
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
        <urn:IE801>
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
    </ns:Messages>
    <ns:CountOfMessagesAvailable>2</ns:CountOfMessagesAvailable>
  </ns:NewMessagesDataResponse>

  lazy val emptyNewMessageDataXml: Elem = <ns:NewMessagesDataResponse
  xmlns:ns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/NewMessagesData/3">
    <ns:Messages>
    </ns:Messages>
    <ns:CountOfMessagesAvailable>0</ns:CountOfMessagesAvailable>
  </ns:NewMessagesDataResponse>
}

