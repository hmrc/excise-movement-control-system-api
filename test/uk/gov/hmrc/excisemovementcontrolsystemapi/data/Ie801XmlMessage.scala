package uk.gov.hmrc.excisemovementcontrolsystemapi.data

object Ie801XmlMessage {

  lazy val IE801 =
    <IE801 xmlns:IE801="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.01">
      <IE801:Header>
        <MessageSender>token</MessageSender>
        <MessageRecipient>token</MessageRecipient>
        <DateOfPreparation>2018-11-01</DateOfPreparation>
        <TimeOfPreparation>02:02:49+01:00</TimeOfPreparation>
        <MessageIdentifier>token</MessageIdentifier>
        <CorrelationIdentifier>token</CorrelationIdentifier>
      </IE801:Header>
      <IE801:Body>
        <IE801:EADESADContainer>
          <IE801:ConsigneeTrader language="to">
            <IE801:Traderid>token</IE801:Traderid>
            <IE801:TraderName>token</IE801:TraderName>
            <IE801:StreetName>token</IE801:StreetName>
            <IE801:StreetNumber>token</IE801:StreetNumber>
            <IE801:Postcode>token</IE801:Postcode>
            <IE801:City>token</IE801:City>
            <IE801:EoriNumber>token</IE801:EoriNumber>
          </IE801:ConsigneeTrader>
          <IE801:ExciseMovement>
            <IE801:AdministrativeReferenceCode>tokentokentokentokent</IE801:AdministrativeReferenceCode>
            <IE801:DateAndTimeOfValidationOfEadEsad>2002-11-05T08:01:03</IE801:DateAndTimeOfValidationOfEadEsad>
          </IE801:ExciseMovement>
          <IE801:ConsignorTrader language="to">
            <IE801:TraderExciseNumber>tokentokentok</IE801:TraderExciseNumber>
            <IE801:TraderName>token</IE801:TraderName>
            <IE801:StreetName>token</IE801:StreetName>
            <IE801:StreetNumber>token</IE801:StreetNumber>
            <IE801:Postcode>token</IE801:Postcode>
            <IE801:City>token</IE801:City>
          </IE801:ConsignorTrader>
          <IE801:PlaceOfDispatchTrader language="to">
            <IE801:ReferenceOfTaxWarehouse>tokentokentok</IE801:ReferenceOfTaxWarehouse>
            <IE801:TraderName>token</IE801:TraderName>
            <IE801:StreetName>token</IE801:StreetName>
            <IE801:StreetNumber>token</IE801:StreetNumber>
            <IE801:Postcode>token</IE801:Postcode>
            <IE801:City>token</IE801:City>
          </IE801:PlaceOfDispatchTrader>
          <IE801:DispatchImportOffice>
            <IE801:ReferenceNumber>tokentok</IE801:ReferenceNumber>
          </IE801:DispatchImportOffice>
          <IE801:ComplementConsigneeTrader>
            <IE801:MemberStateCode>to</IE801:MemberStateCode>
            <IE801:SerialNumberOfCertificateOfExemption>token</IE801:SerialNumberOfCertificateOfExemption>
          </IE801:ComplementConsigneeTrader>
          <IE801:DeliveryPlaceTrader language="to">
            <IE801:Traderid>token</IE801:Traderid>
            <IE801:TraderName>token</IE801:TraderName>
            <IE801:StreetName>token</IE801:StreetName>
            <IE801:StreetNumber>token</IE801:StreetNumber>
            <IE801:Postcode>token</IE801:Postcode>
            <IE801:City>token</IE801:City>
          </IE801:DeliveryPlaceTrader>
          <IE801:DeliveryPlaceCustomsOffice>
            <IE801:ReferenceNumber>tokentok</IE801:ReferenceNumber>
          </IE801:DeliveryPlaceCustomsOffice>
          <IE801:CompetentAuthorityDispatchOffice>
            <IE801:ReferenceNumber>tokentok</IE801:ReferenceNumber>
          </IE801:CompetentAuthorityDispatchOffice>
          <IE801:TransportArrangerTrader language="to">
            <IE801:VatNumber>token</IE801:VatNumber>
            <IE801:TraderName>token</IE801:TraderName>
            <IE801:StreetName>token</IE801:StreetName>
            <IE801:StreetNumber>token</IE801:StreetNumber>
            <IE801:Postcode>token</IE801:Postcode>
            <IE801:City>token</IE801:City>
          </IE801:TransportArrangerTrader>
          <IE801:FirstTransporterTrader language="to">
            <IE801:VatNumber>token</IE801:VatNumber>
            <IE801:TraderName>token</IE801:TraderName>
            <IE801:StreetName>token</IE801:StreetName>
            <IE801:StreetNumber>token</IE801:StreetNumber>
            <IE801:Postcode>token</IE801:Postcode>
            <IE801:City>token</IE801:City>
          </IE801:FirstTransporterTrader>
          <IE801:DocumentCertificate>
            <IE801:DocumentType>toke</IE801:DocumentType>
            <IE801:DocumentReference>token</IE801:DocumentReference>
            <IE801:DocumentDescription language="to">token</IE801:DocumentDescription>
            <IE801:ReferenceOfDocument language="to">token</IE801:ReferenceOfDocument>
          </IE801:DocumentCertificate>
          <IE801:EadEsad>
            <IE801:LocalReferenceNumber>token</IE801:LocalReferenceNumber>
            <IE801:InvoiceNumber>token</IE801:InvoiceNumber>
            <IE801:InvoiceDate>2002-06-24+01:00</IE801:InvoiceDate>
            <IE801:OriginTypeCode>3</IE801:OriginTypeCode>
            <IE801:DateOfDispatch>2012-01-07</IE801:DateOfDispatch>
            <IE801:TimeOfDispatch>09:44:59</IE801:TimeOfDispatch>
            <IE801:UpstreamArc>tokentokentokentokent</IE801:UpstreamArc>
            <IE801:ImportSad>
              <IE801:ImportSadNumber>token</IE801:ImportSadNumber>
            </IE801:ImportSad>
          </IE801:EadEsad>
          <IE801:HeaderEadEsad>
            <IE801:SequenceNumber>to</IE801:SequenceNumber>
            <IE801:DateAndTimeOfUpdateValidation>2013-06-17T18:14:58</IE801:DateAndTimeOfUpdateValidation>
            <IE801:DestinationTypeCode>10</IE801:DestinationTypeCode>
            <IE801:JourneyTime>tok</IE801:JourneyTime>
            <IE801:TransportArrangement>4</IE801:TransportArrangement>
          </IE801:HeaderEadEsad>
          <IE801:TransportMode>
            <IE801:TransportModeCode>to</IE801:TransportModeCode>
            <IE801:ComplementaryInformation language="to">token</IE801:ComplementaryInformation>
          </IE801:TransportMode>
          <IE801:MovementGuarantee>
            <IE801:GuarantorTypeCode>12</IE801:GuarantorTypeCode>
            <IE801:GuarantorTrader language="to">
              <IE801:TraderExciseNumber>tokentokentok</IE801:TraderExciseNumber>
              <IE801:TraderName>token</IE801:TraderName>
              <IE801:StreetName>token</IE801:StreetName>
              <IE801:StreetNumber>token</IE801:StreetNumber>
              <IE801:City>token</IE801:City>
              <IE801:Postcode>token</IE801:Postcode>
              <IE801:VatNumber>token</IE801:VatNumber>
            </IE801:GuarantorTrader>
          </IE801:MovementGuarantee>
          <IE801:BodyEadEsad>
            <IE801:BodyRecordUniqueReference>tok</IE801:BodyRecordUniqueReference>
            <IE801:ExciseProductCode>toke</IE801:ExciseProductCode>
            <IE801:CnCode>tokentok</IE801:CnCode>
            <IE801:Quantity>1000.00000000000</IE801:Quantity>
            <IE801:GrossMass>1000.000000000000</IE801:GrossMass>
            <IE801:NetMass>1000.000000000000</IE801:NetMass>
            <IE801:AlcoholicStrengthByVolumeInPercentage>1000.00</IE801:AlcoholicStrengthByVolumeInPercentage>
            <IE801:DegreePlato>1000.00</IE801:DegreePlato>
            <IE801:FiscalMark language="to">token</IE801:FiscalMark>
            <IE801:FiscalMarkUsedFlag>1</IE801:FiscalMarkUsedFlag>
            <IE801:DesignationOfOrigin language="to">token</IE801:DesignationOfOrigin>
            <IE801:SizeOfProducer>token</IE801:SizeOfProducer>
            <IE801:Density>1000.00</IE801:Density>
            <IE801:CommercialDescription language="to">token</IE801:CommercialDescription>
            <IE801:BrandNameOfProducts language="to">token</IE801:BrandNameOfProducts>
            <IE801:MaturationPeriodOrAgeOfProducts>token</IE801:MaturationPeriodOrAgeOfProducts>
            <IE801:Package>
              <IE801:KindOfPackages>to</IE801:KindOfPackages>
              <IE801:NumberOfPackages>token</IE801:NumberOfPackages>
              <IE801:ShippingMarks>token</IE801:ShippingMarks>
              <IE801:CommercialSealIdentification>token</IE801:CommercialSealIdentification>
              <IE801:SealInformation language="to">token</IE801:SealInformation>
            </IE801:Package>
            <IE801:WineProduct>
              <IE801:WineProductCategory>2</IE801:WineProductCategory>
              <IE801:WineGrowingZoneCode>to</IE801:WineGrowingZoneCode>
              <IE801:ThirdCountryOfOrigin>to</IE801:ThirdCountryOfOrigin>
              <IE801:OtherInformation language="to">token</IE801:OtherInformation>
              <IE801:WineOperation>
                <IE801:WineOperationCode>to</IE801:WineOperationCode>
              </IE801:WineOperation>
            </IE801:WineProduct>
          </IE801:BodyEadEsad>
          <IE801:TransportDetails>
            <IE801:TransportUnitCode>to</IE801:TransportUnitCode>
            <IE801:IdentityOfTransportUnits>token</IE801:IdentityOfTransportUnits>
            <IE801:CommercialSealIdentification>token</IE801:CommercialSealIdentification>
            <IE801:ComplementaryInformation language="to">token</IE801:ComplementaryInformation>
            <IE801:SealInformation language="to">token</IE801:SealInformation>
          </IE801:TransportDetails>
        </IE801:EADESADContainer>
      </IE801:Body>
    </IE801>

}
