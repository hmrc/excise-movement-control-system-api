package uk.gov.hmrc.excisemovementcontrolsystemapi.data

object NewMessagesXml {

  lazy val newMessageWithIE801 = <ns:NewMessagesDataResponse
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
}
