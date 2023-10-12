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

trait GetNewMessagesXml {

  lazy val newMessageXml: Elem = <ns:NewMessagesDataResponse
  xmlns:ns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/NewMessagesData/3"
  xmlns:ns1="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3"
  xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
  xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.01"
  xmlns:urn2="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.01">
    <ns:Messages>
      <ns1:IE704>
        <ns1:Header>
          <urn:MessageSender>token</urn:MessageSender>
          <urn:MessageRecipient>token</urn:MessageRecipient>
          <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
          <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
          <urn:MessageIdentifier>token</urn:MessageIdentifier>
          <!--Optional:-->
          <urn:CorrelationIdentifier>token</urn:CorrelationIdentifier>
        </ns1:Header>
        <ns1:Body>
          <ns1:GenericRefusalMessage>
            <!--Optional:-->
            <ns1:Attributes>
              <!--Optional:-->
              <ns1:AdministrativeReferenceCode>tokentokentokentokent</ns1:AdministrativeReferenceCode>
              <!--Optional:-->
              <ns1:SequenceNumber>to</ns1:SequenceNumber>
              <!--Optional:-->
              <ns1:LocalReferenceNumber>token</ns1:LocalReferenceNumber>
            </ns1:Attributes>
            <!--1 or more repetitions:-->
            <ns1:FunctionalError>
              <ns1:ErrorType>4518</ns1:ErrorType>
              <ns1:ErrorReason>token</ns1:ErrorReason>
              <!--Optional:-->
              <ns1:ErrorLocation>token</ns1:ErrorLocation>
              <!--Optional:-->
              <ns1:OriginalAttributeValue>token</ns1:OriginalAttributeValue>
            </ns1:FunctionalError>
          </ns1:GenericRefusalMessage>
        </ns1:Body>
      </ns1:IE704>
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
      <urn2:IE802>
        <urn2:Header>
          <urn:MessageSender>token</urn:MessageSender>
          <urn:MessageRecipient>token</urn:MessageRecipient>
          <urn:DateOfPreparation>2015-08-24</urn:DateOfPreparation>
          <urn:TimeOfPreparation>23:07:00+01:00</urn:TimeOfPreparation>
          <urn:MessageIdentifier>token</urn:MessageIdentifier>
          <!--Optional:-->
          <urn:CorrelationIdentifier>token</urn:CorrelationIdentifier>
        </urn2:Header>
        <urn2:Body>
          <urn2:ReminderMessageForExciseMovement>
            <urn2:Attributes>
              <urn2:DateAndTimeOfIssuanceOfReminder>2000-04-21T01:36:55+01:00</urn2:DateAndTimeOfIssuanceOfReminder>
              <!--Optional:-->
              <urn2:ReminderInformation language="to">token</urn2:ReminderInformation>
              <urn2:LimitDateAndTime>2017-04-19T15:38:57+01:00</urn2:LimitDateAndTime>
              <urn2:ReminderMessageType>1</urn2:ReminderMessageType>
            </urn2:Attributes>
            <urn2:ExciseMovement>
              <urn2:AdministrativeReferenceCode>tokentokentokentokent</urn2:AdministrativeReferenceCode>
              <urn2:SequenceNumber>to</urn2:SequenceNumber>
            </urn2:ExciseMovement>
          </urn2:ReminderMessageForExciseMovement>
        </urn2:Body>
      </urn2:IE802>
    </ns:Messages>
    <ns:CountOfMessagesAvailable>3</ns:CountOfMessagesAvailable>
  </ns:NewMessagesDataResponse>

  lazy val newMessageXml2: Elem = <ns:NewMessagesDataResponse
  xmlns:ns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/NewMessagesData/3"
  xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.01"
  xmlns:urn2="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.01"
  xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
    <ns:Messages>
      <urn1:IE810>
        <urn1:Header>
          <urn:MessageSender>token</urn:MessageSender>
          <urn:MessageRecipient>token</urn:MessageRecipient>
          <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
          <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
          <urn:MessageIdentifier>token</urn:MessageIdentifier>
          <urn:CorrelationIdentifier>token</urn:CorrelationIdentifier>
        </urn1:Header>
        <urn1:Body>
          <urn1:CancellationOfEAD>
            <urn1:Attributes>
              <urn1:DateAndTimeOfValidationOfCancellation>2006-08-19T18:27:14+01:00</urn1:DateAndTimeOfValidationOfCancellation>
            </urn1:Attributes>
            <urn1:ExciseMovementEad>
              <urn1:AdministrativeReferenceCode>tokentokentokentokent</urn1:AdministrativeReferenceCode>
            </urn1:ExciseMovementEad>
            <urn1:Cancellation>
              <urn1:CancellationReasonCode>t</urn1:CancellationReasonCode>
              <urn1:ComplementaryInformation language="to">token</urn1:ComplementaryInformation>
            </urn1:Cancellation>
          </urn1:CancellationOfEAD>
        </urn1:Body>
      </urn1:IE810>
        <urn2:IE818>
        <urn2:Header>
          <urn:MessageSender>token</urn:MessageSender>
          <urn:MessageRecipient>token</urn:MessageRecipient>
          <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
          <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
          <urn:MessageIdentifier>token</urn:MessageIdentifier>
          <urn:CorrelationIdentifier>token</urn:CorrelationIdentifier>
        </urn2:Header>
        <urn2:Body>
          <urn2:AcceptedOrRejectedReportOfReceiptExport>
            <urn2:Attributes>
              <urn2:DateAndTimeOfValidationOfReportOfReceiptExport>2006-08-19T18:27:14+01:00</urn2:DateAndTimeOfValidationOfReportOfReceiptExport>
            </urn2:Attributes>
            <urn2:ConsigneeTrader language="to">
              <urn2:Traderid>token</urn2:Traderid>
              <urn2:TraderName>token</urn2:TraderName>
              <urn2:StreetName>token</urn2:StreetName>
              <urn2:StreetNumber>token</urn2:StreetNumber>
              <urn2:Postcode>token</urn2:Postcode>
              <urn2:City>token</urn2:City>
              <urn2:EoriNumber>token</urn2:EoriNumber>
            </urn2:ConsigneeTrader>
            <urn2:ExciseMovement>
              <urn2:AdministrativeReferenceCode>tokentokentokentokent</urn2:AdministrativeReferenceCode>
              <urn2:SequenceNumber>to</urn2:SequenceNumber>
            </urn2:ExciseMovement>
            <urn2:DeliveryPlaceTrader language="to">
              <urn2:Traderid>token</urn2:Traderid>
              <urn2:TraderName>token</urn2:TraderName>
              <urn2:StreetName>token</urn2:StreetName>
              <urn2:StreetNumber>token</urn2:StreetNumber>
              <urn2:Postcode>token</urn2:Postcode>
              <urn2:City>token</urn2:City>
            </urn2:DeliveryPlaceTrader>
            <urn2:DestinationOffice>
              <urn2:ReferenceNumber>tokentok</urn2:ReferenceNumber>
            </urn2:DestinationOffice>
            <urn2:ReportOfReceiptExport>
              <urn2:DateOfArrivalOfExciseProducts>2009-05-16</urn2:DateOfArrivalOfExciseProducts>
              <urn2:GlobalConclusionOfReceipt>3</urn2:GlobalConclusionOfReceipt>
              <urn2:ComplementaryInformation language="to">token</urn2:ComplementaryInformation>
            </urn2:ReportOfReceiptExport>
            <urn2:BodyReportOfReceiptExport>
              <urn2:BodyRecordUniqueReference>tok</urn2:BodyRecordUniqueReference>
              <urn2:IndicatorOfShortageOrExcess>E</urn2:IndicatorOfShortageOrExcess>
              <urn2:ObservedShortageOrExcess>1000.00000000000</urn2:ObservedShortageOrExcess>
              <urn2:ExciseProductCode>toke</urn2:ExciseProductCode>
              <urn2:RefusedQuantity>1000.00000000000</urn2:RefusedQuantity>
              <urn2:UnsatisfactoryReason>
                <urn2:UnsatisfactoryReasonCode>to</urn2:UnsatisfactoryReasonCode>
                <urn2:ComplementaryInformation language="to">token</urn2:ComplementaryInformation>
              </urn2:UnsatisfactoryReason>
            </urn2:BodyReportOfReceiptExport>
          </urn2:AcceptedOrRejectedReportOfReceiptExport>
        </urn2:Body>
      </urn2:IE818>
    </ns:Messages>
    <ns:CountOfMessagesAvailable>2</ns:CountOfMessagesAvailable>
  </ns:NewMessagesDataResponse>
}
