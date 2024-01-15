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

trait TestXml {

  lazy val IE704: Elem = <ns1:IE704 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                    xmlns:ns1="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3">
    <ns1:Header>
      <urn:MessageSender>NDEA.XI</urn:MessageSender>
      <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
      <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
      <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
      <urn:MessageIdentifier>XI000001</urn:MessageIdentifier>
      <!--Optional:-->
      <urn:CorrelationIdentifier>9b8effe4-adca-4431-bfc2-d65bb5f1e15d</urn:CorrelationIdentifier>
    </ns1:Header>
    <ns1:Body>
      <ns1:GenericRefusalMessage>
        <!--Optional:-->
        <ns1:Attributes>
          <!--Optional:-->
          <ns1:AdministrativeReferenceCode>23XI00000000000000012</ns1:AdministrativeReferenceCode>
          <!--Optional:-->
          <ns1:SequenceNumber>1</ns1:SequenceNumber>
          <!--Optional:-->
          <ns1:LocalReferenceNumber>lrnie8158976912</ns1:LocalReferenceNumber>
        </ns1:Attributes>
        <!--1 or more repetitions:-->
        <ns1:FunctionalError>
          <ns1:ErrorType>4401</ns1:ErrorType>
          <ns1:ErrorReason>token</ns1:ErrorReason>
          <!--Optional:-->
          <ns1:ErrorLocation>token</ns1:ErrorLocation>
          <!--Optional:-->
          <ns1:OriginalAttributeValue>token</ns1:OriginalAttributeValue>
        </ns1:FunctionalError>
      </ns1:GenericRefusalMessage>
    </ns1:Body>
  </ns1:IE704>

  lazy val IE704NoArc: Elem = <ns1:IE704 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                    xmlns:ns1="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3">
    <ns1:Header>
      <urn:MessageSender>NDEA.XI</urn:MessageSender>
      <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
      <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
      <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
      <urn:MessageIdentifier>XI000001</urn:MessageIdentifier>
      <urn:CorrelationIdentifier>9b8effe4-adca-4431-bfc2-d65bb5f1e15d</urn:CorrelationIdentifier>
    </ns1:Header>
    <ns1:Body>
      <ns1:GenericRefusalMessage>
        <ns1:Attributes>
          <ns1:SequenceNumber>1</ns1:SequenceNumber>
          <ns1:LocalReferenceNumber>lrnie8158976912</ns1:LocalReferenceNumber>
        </ns1:Attributes>
        <ns1:FunctionalError>
          <ns1:ErrorType>4401</ns1:ErrorType>
          <ns1:ErrorReason>token</ns1:ErrorReason>
          <ns1:ErrorLocation>token</ns1:ErrorLocation>
          <ns1:OriginalAttributeValue>token</ns1:OriginalAttributeValue>
        </ns1:FunctionalError>
      </ns1:GenericRefusalMessage>
    </ns1:Body>
  </ns1:IE704>

  lazy val IE801: Elem =
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

  lazy val IE802: Elem = <urn7:IE802 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                     xmlns:urn7="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.01">
    <urn7:Header>
      <urn:MessageSender>CSMISE.EC</urn:MessageSender>
      <urn:MessageRecipient>CSMISE.EC</urn:MessageRecipient>
      <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
      <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
      <urn:MessageIdentifier>X00004</urn:MessageIdentifier>
      <!--Optional:-->
      <urn:CorrelationIdentifier>X00004</urn:CorrelationIdentifier>
    </urn7:Header>
    <urn7:Body>
      <urn7:ReminderMessageForExciseMovement>
        <urn7:Attributes>
          <urn7:DateAndTimeOfIssuanceOfReminder>2006-08-19T18:27:14</urn7:DateAndTimeOfIssuanceOfReminder>
          <!--Optional:-->
          <urn7:ReminderInformation language="to">token</urn7:ReminderInformation>
          <urn7:LimitDateAndTime>2009-05-16T13:42:28</urn7:LimitDateAndTime>
          <urn7:ReminderMessageType>2</urn7:ReminderMessageType>
        </urn7:Attributes>
        <urn7:ExciseMovement>
          <urn7:AdministrativeReferenceCode>23XI00000000000000090</urn7:AdministrativeReferenceCode>
          <urn7:SequenceNumber>10</urn7:SequenceNumber>
        </urn7:ExciseMovement>
      </urn7:ReminderMessageForExciseMovement>
    </urn7:Body>
  </urn7:IE802>

  lazy val IE803: Elem = <urn6:IE803 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                     xmlns:urn6="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE803:V3.01">
    <urn6:Header>
      <urn:MessageSender>NDEA.GB</urn:MessageSender>
      <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
      <urn:DateOfPreparation>2023-06-27</urn:DateOfPreparation>
      <urn:TimeOfPreparation>00:23:33</urn:TimeOfPreparation>
      <urn:MessageIdentifier>GB002312688</urn:MessageIdentifier>
      <urn:CorrelationIdentifier>6dddasfffff3abcb344bbcbcbcbc3435</urn:CorrelationIdentifier>
    </urn6:Header>
    <urn6:Body>
      <urn6:NotificationOfDivertedEADESAD>
        <urn6:ExciseNotification>
          <urn6:NotificationType>1</urn6:NotificationType>
          <urn6:NotificationDateAndTime>2023-06-26T23:56:46</urn6:NotificationDateAndTime>
          <urn6:AdministrativeReferenceCode>23XI00000000000056333</urn6:AdministrativeReferenceCode>
          <urn6:SequenceNumber>1</urn6:SequenceNumber>
        </urn6:ExciseNotification>
      </urn6:NotificationOfDivertedEADESAD>
    </urn6:Body>
  </urn6:IE803>

  lazy val IE807: Elem = <urn4:IE807 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                     xmlns:urn4="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE807:V3.01">
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

  lazy val IE810: Elem = <urn:IE810 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.01"
                                    xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                    xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
                                    xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
                                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <urn:Header>
      <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
      <urn1:MessageRecipient>NDEA.GB</urn1:MessageRecipient>
      <urn1:DateOfPreparation>2023-06-13</urn1:DateOfPreparation>
      <urn1:TimeOfPreparation>10:16:58</urn1:TimeOfPreparation>
      <urn1:MessageIdentifier>GB100000000302249</urn1:MessageIdentifier>
      <urn1:CorrelationIdentifier>49ec29186e2c471eb1fb2e98313bd1ce</urn1:CorrelationIdentifier>
    </urn:Header>
    <urn:Body>
      <urn:CancellationOfEAD>
        <urn:Attributes>
          <urn:DateAndTimeOfValidationOfCancellation>2023-06-13T10:17:05</urn:DateAndTimeOfValidationOfCancellation>
        </urn:Attributes>
        <urn:ExciseMovementEad>
          <urn:AdministrativeReferenceCode>23GB00000000000377161</urn:AdministrativeReferenceCode>
        </urn:ExciseMovementEad>
        <urn:Cancellation>
          <urn:CancellationReasonCode>3</urn:CancellationReasonCode>
        </urn:Cancellation>
      </urn:CancellationOfEAD>
    </urn:Body>
  </urn:IE810>

  lazy val IE813: Elem = <urn:IE813 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE813:V3.01"
                                    xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                    xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
                                    xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
                                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <urn:Header>
      <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
      <urn1:MessageRecipient>NDEA.GB</urn1:MessageRecipient>
      <urn1:DateOfPreparation>2023-08-15</urn1:DateOfPreparation>
      <urn1:TimeOfPreparation>11:54:25</urn1:TimeOfPreparation>
      <urn1:MessageIdentifier>GB100000000302715</urn1:MessageIdentifier>
      <urn1:CorrelationIdentifier>PORTAL906384fb126d43e787a802683c03b44c</urn1:CorrelationIdentifier>
    </urn:Header>
    <urn:Body>
      <urn:ChangeOfDestination>
        <urn:Attributes>
          <urn:DateAndTimeOfValidationOfChangeOfDestination>2023-08-15T11:54:32</urn:DateAndTimeOfValidationOfChangeOfDestination>
        </urn:Attributes>
        <urn:UpdateEadEsad>
          <urn:AdministrativeReferenceCode>23GB00000000000378126</urn:AdministrativeReferenceCode>
          <urn:JourneyTime>D02</urn:JourneyTime>
          <urn:ChangedTransportArrangement>1</urn:ChangedTransportArrangement>
          <urn:SequenceNumber>3</urn:SequenceNumber>
          <urn:InvoiceNumber>5678</urn:InvoiceNumber>
          <urn:TransportModeCode>4</urn:TransportModeCode>
        </urn:UpdateEadEsad>
        <urn:DestinationChanged>
          <urn:DestinationTypeCode>1</urn:DestinationTypeCode>
          <urn:NewConsigneeTrader language="en">
            <urn:Traderid>GBWK240176600</urn:Traderid>
            <urn:TraderName>pqr</urn:TraderName>
            <urn:StreetName>Tattenhoe Park</urn:StreetName>
            <urn:StreetNumber>18 Priestly</urn:StreetNumber>
            <urn:Postcode>MK4 4NW</urn:Postcode>
            <urn:City>Milton Keynes</urn:City>
          </urn:NewConsigneeTrader>
          <urn:DeliveryPlaceTrader language="en">
            <urn:Traderid>GB00240176601</urn:Traderid>
            <urn:TraderName>lmn</urn:TraderName>
            <urn:StreetName>Tattenhoe Park</urn:StreetName>
            <urn:StreetNumber>18 Priestl</urn:StreetNumber>
            <urn:Postcode>MK4 4NW</urn:Postcode>
            <urn:City>Milton Keynes</urn:City>
          </urn:DeliveryPlaceTrader>
          <urn:MovementGuarantee>
            <urn:GuarantorTypeCode>1</urn:GuarantorTypeCode>
          </urn:MovementGuarantee>
        </urn:DestinationChanged>
        <urn:NewTransporterTrader language="en">
          <urn:TraderName>pqr</urn:TraderName>
          <urn:StreetName>Tattenhoe Park</urn:StreetName>
          <urn:StreetNumber>18 Priestly</urn:StreetNumber>
          <urn:Postcode>MK4 4NW</urn:Postcode>
          <urn:City>Milton Keynes</urn:City>
        </urn:NewTransporterTrader>
        <urn:TransportDetails>
          <urn:TransportUnitCode>1</urn:TransportUnitCode>
          <urn:IdentityOfTransportUnits>1</urn:IdentityOfTransportUnits>
        </urn:TransportDetails>
      </urn:ChangeOfDestination>
    </urn:Body>
  </urn:IE813>

  lazy val IE815: Elem = IE815Template("GBWK002281023")
  lazy val IE815WithNoCosignor: Elem = IE815Template("")

  private def IE815Template(consignor: String): Elem = <urn:IE815 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.01"
                                                                  xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                                                  xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
                                                                  xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
                                                                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <urn:Header>
      <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
      <urn1:MessageRecipient>NDEA.GB</urn1:MessageRecipient>
      <urn1:DateOfPreparation>2023-09-09</urn1:DateOfPreparation>
      <urn1:TimeOfPreparation>03:22:47</urn1:TimeOfPreparation>
      <urn1:MessageIdentifier>6de1b822562c43fb9220d236e487c920</urn1:MessageIdentifier>
      <urn1:CorrelationIdentifier>PORTAL6de1b822562c43fb9220d236e487c920</urn1:CorrelationIdentifier>
    </urn:Header>
    <urn:Body>
      <urn:SubmittedDraftOfEADESAD>
        <urn:Attributes>
          <urn:SubmissionMessageType>1</urn:SubmissionMessageType>
        </urn:Attributes>
        <urn:ConsigneeTrader language="en">
          <urn:Traderid>GBWKQOZ8OVLYR</urn:Traderid>
          <urn:TraderName>WFlgUjfC</urn:TraderName>
          <urn:StreetName>xoL0NsNyDi</urn:StreetName>
          <urn:StreetNumber>67</urn:StreetNumber>
          <urn:Postcode>A1 1AA</urn:Postcode>
          <urn:City>l8WSaHS9</urn:City>
        </urn:ConsigneeTrader>
        <urn:ConsignorTrader language="en">
          <urn:TraderExciseNumber>{consignor}</urn:TraderExciseNumber>
          <urn:TraderName>DIAGEO PLC</urn:TraderName>
          <urn:StreetName>msfvZUL1Oe</urn:StreetName>
          <urn:StreetNumber>25</urn:StreetNumber>
          <urn:Postcode>A1 1AA</urn:Postcode>
          <urn:City>QDHwPa61</urn:City>
        </urn:ConsignorTrader>
        <urn:PlaceOfDispatchTrader language="en">
          <urn:ReferenceOfTaxWarehouse>GB00DO459DMNX</urn:ReferenceOfTaxWarehouse>
          <urn:TraderName>2z0waekA</urn:TraderName>
          <urn:StreetName>MhO1XtDIVr</urn:StreetName>
          <urn:StreetNumber>25</urn:StreetNumber>
          <urn:Postcode>A1 1AA</urn:Postcode>
          <urn:City>zPCc6skm</urn:City>
        </urn:PlaceOfDispatchTrader>
        <urn:DeliveryPlaceTrader language="en">
          <urn:Traderid>GB00AIP67RAO3</urn:Traderid>
          <urn:TraderName>BJpWdv2N</urn:TraderName>
          <urn:StreetName>C24vvUqCw6</urn:StreetName>
          <urn:StreetNumber>43</urn:StreetNumber>
          <urn:Postcode>A1 1AA</urn:Postcode>
          <urn:City>A9ZlElxP</urn:City>
        </urn:DeliveryPlaceTrader>
        <urn:CompetentAuthorityDispatchOffice>
          <urn:ReferenceNumber>GB004098</urn:ReferenceNumber>
        </urn:CompetentAuthorityDispatchOffice>
        <urn:FirstTransporterTrader language="en">
          <urn:VatNumber>123798354</urn:VatNumber>
          <urn:TraderName>Mr Delivery place trader 4</urn:TraderName>
          <urn:StreetName>Delplace Avenue</urn:StreetName>
          <urn:StreetNumber>05</urn:StreetNumber>
          <urn:Postcode>FR5 4RN</urn:Postcode>
          <urn:City>Delville</urn:City>
        </urn:FirstTransporterTrader>
        <urn:DocumentCertificate>
          <urn:DocumentType>9</urn:DocumentType>
          <urn:DocumentReference>DPdQsYktZEJEESpc7b32Ig0U6B34XmHmfZU</urn:DocumentReference>
        </urn:DocumentCertificate>
        <urn:HeaderEadEsad>
          <urn:DestinationTypeCode>1</urn:DestinationTypeCode>
          <urn:JourneyTime>D07</urn:JourneyTime>
          <urn:TransportArrangement>1</urn:TransportArrangement>
        </urn:HeaderEadEsad>
        <urn:TransportMode>
          <urn:TransportModeCode>3</urn:TransportModeCode>
        </urn:TransportMode>
        <urn:MovementGuarantee>
          <urn:GuarantorTypeCode>1</urn:GuarantorTypeCode>
        </urn:MovementGuarantee>
        <urn:BodyEadEsad>
          <urn:BodyRecordUniqueReference>1</urn:BodyRecordUniqueReference>
          <urn:ExciseProductCode>B000</urn:ExciseProductCode>
          <urn:CnCode>22030001</urn:CnCode>
          <urn:Quantity>2000</urn:Quantity>
          <urn:GrossMass>20000</urn:GrossMass>
          <urn:NetMass>19999</urn:NetMass>
          <urn:AlcoholicStrengthByVolumeInPercentage>0.5</urn:AlcoholicStrengthByVolumeInPercentage>
          <urn:FiscalMarkUsedFlag>0</urn:FiscalMarkUsedFlag>
          <urn:Package>
            <urn:KindOfPackages>BA</urn:KindOfPackages>
            <urn:NumberOfPackages>2</urn:NumberOfPackages>
          </urn:Package>
        </urn:BodyEadEsad>
        <urn:EadEsadDraft>
          <urn:LocalReferenceNumber>LRNQA20230909022221</urn:LocalReferenceNumber>
          <urn:InvoiceNumber>Test123</urn:InvoiceNumber>
          <urn:InvoiceDate>2023-09-09</urn:InvoiceDate>
          <urn:OriginTypeCode>1</urn:OriginTypeCode>
          <urn:DateOfDispatch>2023-09-09</urn:DateOfDispatch>
          <urn:TimeOfDispatch>12:00:00</urn:TimeOfDispatch>
        </urn:EadEsadDraft>
        <urn:TransportDetails>
          <urn:TransportUnitCode>1</urn:TransportUnitCode>
          <urn:IdentityOfTransportUnits>100</urn:IdentityOfTransportUnits>
        </urn:TransportDetails>
      </urn:SubmittedDraftOfEADESAD>
    </urn:Body>
  </urn:IE815>

  lazy val IE818: Elem = <urn:IE818 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.01"
                                    xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                    xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
                                    xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
                                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <urn:Header>
      <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
      <urn1:MessageRecipient>NDEA.GB</urn1:MessageRecipient>
      <urn1:DateOfPreparation>2023-08-30</urn1:DateOfPreparation>
      <urn1:TimeOfPreparation>13:53:53.425279</urn1:TimeOfPreparation>
      <urn1:MessageIdentifier>GB100000000302814</urn1:MessageIdentifier>
      <urn1:CorrelationIdentifier>73e87e9b-1145-4dbf-8ee3-807ac103ba62</urn1:CorrelationIdentifier>
    </urn:Header>
    <urn:Body>
      <urn:AcceptedOrRejectedReportOfReceiptExport>
        <urn:Attributes>
          <urn:DateAndTimeOfValidationOfReportOfReceiptExport>2023-08-30T14:53:56</urn:DateAndTimeOfValidationOfReportOfReceiptExport>
        </urn:Attributes>
        <urn:ConsigneeTrader language="en">
          <urn:Traderid>GBWK002281023</urn:Traderid>
          <urn:TraderName>Meredith Ent</urn:TraderName>
          <urn:StreetName>Romanus Crescent</urn:StreetName>
          <urn:StreetNumber>38</urn:StreetNumber>
          <urn:Postcode>SE24 5GY</urn:Postcode>
          <urn:City>London</urn:City>
        </urn:ConsigneeTrader>
        <urn:ExciseMovement>
          <urn:AdministrativeReferenceCode>23GB00000000000378553</urn:AdministrativeReferenceCode>
          <urn:SequenceNumber>1</urn:SequenceNumber>
        </urn:ExciseMovement>
        <urn:DeliveryPlaceTrader language="en">
          <urn:Traderid>GB00002281023</urn:Traderid>
          <urn:TraderName>Meredith Ent</urn:TraderName>
          <urn:StreetName>Romanus Crescent</urn:StreetName>
          <urn:StreetNumber>38</urn:StreetNumber>
          <urn:Postcode>SE24 5GY</urn:Postcode>
          <urn:City>London</urn:City>
        </urn:DeliveryPlaceTrader>
        <urn:DestinationOffice>
          <urn:ReferenceNumber>GB004098</urn:ReferenceNumber>
        </urn:DestinationOffice>
        <urn:ReportOfReceiptExport>
          <urn:DateOfArrivalOfExciseProducts>2023-08-30</urn:DateOfArrivalOfExciseProducts>
          <urn:GlobalConclusionOfReceipt>3</urn:GlobalConclusionOfReceipt>
        </urn:ReportOfReceiptExport>
        <urn:BodyReportOfReceiptExport>
          <urn:BodyRecordUniqueReference>1</urn:BodyRecordUniqueReference>
          <urn:ExciseProductCode>B000</urn:ExciseProductCode>
          <urn:UnsatisfactoryReason>
            <urn:UnsatisfactoryReasonCode>2</urn:UnsatisfactoryReasonCode>
            <urn:ComplementaryInformation language="en">All is good :)</urn:ComplementaryInformation>
          </urn:UnsatisfactoryReason>
        </urn:BodyReportOfReceiptExport>
      </urn:AcceptedOrRejectedReportOfReceiptExport>
    </urn:Body>
  </urn:IE818>

  lazy val IE819: Elem =
    <urn:IE819 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE819:V3.01"
               xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
               xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
               xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <urn:Header>
        <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
        <urn1:MessageRecipient>NDEA.GB</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2023-08-31</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>11:32:45</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>GB100000000302820</urn1:MessageIdentifier>
        <urn1:CorrelationIdentifier>PORTAL1a027311ecef42ef90e40d7201b4f5a7</urn1:CorrelationIdentifier>
      </urn:Header>
      <urn:Body>
        <urn:AlertOrRejectionOfEADESAD>
          <urn:Attributes>
            <urn:DateAndTimeOfValidationOfAlertRejection>2023-08-31T11:32:47</urn:DateAndTimeOfValidationOfAlertRejection>
          </urn:Attributes>
          <urn:ConsigneeTrader language="en">
            <urn:Traderid>GBWK002281023</urn:Traderid>
            <urn:TraderName>Roms PLC</urn:TraderName>
            <urn:StreetName>Bellhouston Road</urn:StreetName>
            <urn:StreetNumber>420</urn:StreetNumber>
            <urn:Postcode>G41 5BS</urn:Postcode>
            <urn:City>Glasgow</urn:City>
          </urn:ConsigneeTrader>
          <urn:ExciseMovement>
            <urn:AdministrativeReferenceCode>23GB00000000000378574</urn:AdministrativeReferenceCode>
            <urn:SequenceNumber>1</urn:SequenceNumber>
          </urn:ExciseMovement>
          <urn:DestinationOffice>
            <urn:ReferenceNumber>GB004098</urn:ReferenceNumber>
          </urn:DestinationOffice>
          <urn:AlertOrRejection>
            <urn:DateOfAlertOrRejection>2023-08-31</urn:DateOfAlertOrRejection>
            <urn:EadEsadRejectedFlag>1</urn:EadEsadRejectedFlag>
          </urn:AlertOrRejection>
          <urn:AlertOrRejectionOfEadEsadReason>
            <urn:AlertOrRejectionOfMovementReasonCode>2</urn:AlertOrRejectionOfMovementReasonCode>
            <urn:ComplementaryInformation language="en">test</urn:ComplementaryInformation>
          </urn:AlertOrRejectionOfEadEsadReason>
        </urn:AlertOrRejectionOfEADESAD>
      </urn:Body>
    </urn:IE819>

  lazy val IE829: Elem = <urn2:IE829 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
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
          <urn2:Traderid>AT00000612157</urn2:Traderid>
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

  lazy val IE837WithConsignor: Elem =
    <urn:IE837 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.01"
               xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
               xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
               xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <urn:Header>
        <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
        <urn1:MessageRecipient>NDEA.EU</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2023-08-10</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>09:56:40.695540</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>GB100000000302681</urn1:MessageIdentifier>
        <urn1:CorrelationIdentifier>a2f65a81-c297-4117-bea5-556129529463</urn1:CorrelationIdentifier>
      </urn:Header>
      <urn:Body>
        <urn:ExplanationOnDelayForDelivery>
          <urn:Attributes>
            <urn:SubmitterIdentification>GBWK240176600</urn:SubmitterIdentification>
            <urn:SubmitterType>1</urn:SubmitterType>
            <urn:ExplanationCode>6</urn:ExplanationCode>
            <urn:ComplementaryInformation language="en">Accident on M5</urn:ComplementaryInformation>
            <urn:MessageRole>1</urn:MessageRole>
            <urn:DateAndTimeOfValidationOfExplanationOnDelay>2023-08-10T10:56:42</urn:DateAndTimeOfValidationOfExplanationOnDelay>
          </urn:Attributes>
          <urn:ExciseMovement>
            <urn:AdministrativeReferenceCode>16GB00000000000192223</urn:AdministrativeReferenceCode>
            <urn:SequenceNumber>2</urn:SequenceNumber>
          </urn:ExciseMovement>
        </urn:ExplanationOnDelayForDelivery>
      </urn:Body>
    </urn:IE837>

  lazy val IE837WithConsignee: Elem =
    <urn:IE837 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.01"
               xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
               xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
               xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <urn:Header>
        <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
        <urn1:MessageRecipient>NDEA.EU</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2023-08-10</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>09:56:40.695540</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>GB100000000302681</urn1:MessageIdentifier>
        <urn1:CorrelationIdentifier>a2f65a81-c297-4117-bea5-556129529463</urn1:CorrelationIdentifier>
      </urn:Header>
      <urn:Body>
        <urn:ExplanationOnDelayForDelivery>
          <urn:Attributes>
            <urn:SubmitterIdentification>GBWK002281023</urn:SubmitterIdentification>
            <urn:SubmitterType>2</urn:SubmitterType>
            <urn:ExplanationCode>6</urn:ExplanationCode>
            <urn:ComplementaryInformation language="en">Accident on M5</urn:ComplementaryInformation>
            <urn:MessageRole>1</urn:MessageRole>
            <urn:DateAndTimeOfValidationOfExplanationOnDelay>2023-08-10T10:56:42</urn:DateAndTimeOfValidationOfExplanationOnDelay>
          </urn:Attributes>
          <urn:ExciseMovement>
            <urn:AdministrativeReferenceCode>16GB00000000000192223</urn:AdministrativeReferenceCode>
            <urn:SequenceNumber>2</urn:SequenceNumber>
          </urn:ExciseMovement>
        </urn:ExplanationOnDelayForDelivery>
      </urn:Body>
    </urn:IE837>

  lazy val IE839: Elem = <urn4:IE839 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                     xmlns:urn4="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.01">
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
          <urn4:Traderid>AT00000612158</urn4:Traderid>
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

  lazy val IE839MultipleArcs: Elem = <urn4:IE839 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                     xmlns:urn4="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.01">
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
          <urn4:Traderid>AT00000612158</urn4:Traderid>
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
        <urn4:CEadVal>
          <urn4:AdministrativeReferenceCode>23XI00000000000056342</urn4:AdministrativeReferenceCode>
          <urn4:SequenceNumber>1</urn4:SequenceNumber>
        </urn4:CEadVal>
        <urn4:CEadVal>
          <urn4:AdministrativeReferenceCode>23XI00000000000056343</urn4:AdministrativeReferenceCode>
          <urn4:SequenceNumber>1</urn4:SequenceNumber>
        </urn4:CEadVal>
        <urn4:NEadSub>
          <urn4:LocalReferenceNumber>lrnie8155755329</urn4:LocalReferenceNumber>
        </urn4:NEadSub>
      </urn4:RefusalByCustoms>
    </urn4:Body>
  </urn4:IE839>

  lazy val IE840: Elem = <urn5:IE840 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
  xmlns:urn5="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE840:V3.01">
    <urn5:Header>
      <urn:MessageSender>NDEA.XI</urn:MessageSender>
      <urn:MessageRecipient>NDEA.GB</urn:MessageRecipient>
      <urn:DateOfPreparation>2023-06-28</urn:DateOfPreparation>
      <urn:TimeOfPreparation>00:17:33</urn:TimeOfPreparation>
      <urn:MessageIdentifier>XI0003265</urn:MessageIdentifier>
      <urn:CorrelationIdentifier>6de24ff423abcb344bbcbcbcbc3499</urn:CorrelationIdentifier>
    </urn5:Header>
    <urn5:Body>
      <urn5:EventReportEnvelope>
        <urn5:Attributes>
          <urn5:EventReportMessageType>1</urn5:EventReportMessageType>
          <urn5:DateAndTimeOfValidationOfEventReport>2023-06-28T00:17:33</urn5:DateAndTimeOfValidationOfEventReport>
        </urn5:Attributes>
        <urn5:HeaderEventReport>
          <urn5:EventReportNumber>GBAA2C3F4244ADB3</urn5:EventReportNumber>
          <urn5:MsOfSubmissionEventReportReference>GB</urn5:MsOfSubmissionEventReportReference>
          <urn5:ReferenceNumberOfExciseOffice>GB848884</urn5:ReferenceNumberOfExciseOffice>
          <urn5:MemberStateOfEvent>GB</urn5:MemberStateOfEvent>
        </urn5:HeaderEventReport>
        <urn5:ExciseMovement>
          <urn5:AdministrativeReferenceCode>23XI00000000000000333</urn5:AdministrativeReferenceCode>
          <urn5:SequenceNumber>1</urn5:SequenceNumber>
        </urn5:ExciseMovement>
        <urn5:OtherAccompanyingDocument>
          <urn5:OtherAccompanyingDocumentType>0</urn5:OtherAccompanyingDocumentType>
          <urn5:ShortDescriptionOfOtherAccompanyingDocument language="to">token</urn5:ShortDescriptionOfOtherAccompanyingDocument>
          <urn5:OtherAccompanyingDocumentNumber>jfj3423jfsjf</urn5:OtherAccompanyingDocumentNumber>
          <urn5:OtherAccompanyingDocumentDate>2023-06-15</urn5:OtherAccompanyingDocumentDate>
          <urn5:ImageOfOtherAccompanyingDocument>ZnJlbXVudA==</urn5:ImageOfOtherAccompanyingDocument>
          <urn5:MemberStateOfDispatch>to</urn5:MemberStateOfDispatch>
          <urn5:MemberStateOfDestination>to</urn5:MemberStateOfDestination>
          <urn5:PersonInvolvedInMovementTrader language="to">
            <urn5:TraderExciseNumber>IT34HD7413733</urn5:TraderExciseNumber>
            <urn5:Traderid>jj4123</urn5:Traderid>
            <urn5:TraderName>Roman Parties Ltd</urn5:TraderName>
            <urn5:TraderPersonType>5</urn5:TraderPersonType>
            <urn5:MemberStateCode>IT</urn5:MemberStateCode>
            <urn5:StreetName>Circus Maximus</urn5:StreetName>
            <urn5:StreetNumber>3</urn5:StreetNumber>
            <urn5:Postcode>RM32</urn5:Postcode>
            <urn5:City>Rome</urn5:City>
            <urn5:PhoneNumber>992743614324</urn5:PhoneNumber>
            <urn5:FaxNumber>848848484</urn5:FaxNumber>
            <urn5:EmailAddress>test@example.com</urn5:EmailAddress>
          </urn5:PersonInvolvedInMovementTrader>
          <urn5:GoodsItem>
            <urn5:DescriptionOfTheGoods>Booze</urn5:DescriptionOfTheGoods>
            <urn5:CnCode>84447744</urn5:CnCode>
            <urn5:CommercialDescriptionOfTheGoods>Alcoholic beverages</urn5:CommercialDescriptionOfTheGoods>
            <urn5:AdditionalCode>token</urn5:AdditionalCode>
            <urn5:Quantity>1000</urn5:Quantity>
            <urn5:UnitOfMeasureCode>12</urn5:UnitOfMeasureCode>
            <urn5:GrossMass>1000</urn5:GrossMass>
            <urn5:NetMass>1000</urn5:NetMass>
          </urn5:GoodsItem>
          <urn5:MeansOfTransport>
            <urn5:TraderName>Big Lorries Ltd</urn5:TraderName>
            <urn5:StreetName>Magic Road</urn5:StreetName>
            <urn5:StreetNumber>42</urn5:StreetNumber>
            <urn5:TransporterCountry>FR</urn5:TransporterCountry>
            <urn5:Postcode>DRA231</urn5:Postcode>
            <urn5:City>Paris</urn5:City>
            <urn5:TransportModeCode>12</urn5:TransportModeCode>
            <urn5:AcoComplementaryInformation language="en">The lorry is taking the goods</urn5:AcoComplementaryInformation>
            <urn5:Registration>tj32343</urn5:Registration>
            <urn5:CountryOfRegistration>FR</urn5:CountryOfRegistration>
          </urn5:MeansOfTransport>
        </urn5:OtherAccompanyingDocument>
        <urn5:EventReport>
          <urn5:DateOfEvent>2023-06-28</urn5:DateOfEvent>
          <urn5:PlaceOfEvent language="en">Dover</urn5:PlaceOfEvent>
          <urn5:ExciseOfficerIdentification>NG28324234</urn5:ExciseOfficerIdentification>
          <urn5:SubmittingPerson>NG2j23432</urn5:SubmittingPerson>
          <urn5:SubmittingPersonCode>12</urn5:SubmittingPersonCode>
          <urn5:SubmittingPersonComplement language="en">Looking good</urn5:SubmittingPersonComplement>
          <urn5:ChangedTransportArrangement>2</urn5:ChangedTransportArrangement>
          <urn5:Comments language="en">words</urn5:Comments>
        </urn5:EventReport>
        <urn5:EvidenceOfEvent>
          <urn5:IssuingAuthority language="en">CSN123</urn5:IssuingAuthority>
          <urn5:EvidenceTypeCode>7</urn5:EvidenceTypeCode>
          <urn5:ReferenceOfEvidence language="en">token</urn5:ReferenceOfEvidence>
          <urn5:ImageOfEvidence>dHVyYmluZQ==</urn5:ImageOfEvidence>
          <urn5:EvidenceTypeComplement language="en">Nice job</urn5:EvidenceTypeComplement>
        </urn5:EvidenceOfEvent>
        <urn5:NewTransportArrangerTrader language="en">
          <urn5:VatNumber>GB823482</urn5:VatNumber>
          <urn5:TraderName>New Transport Arrangers Inc.</urn5:TraderName>
          <urn5:StreetName>Main Street</urn5:StreetName>
          <urn5:StreetNumber>7655</urn5:StreetNumber>
          <urn5:Postcode>ZL23 XD1</urn5:Postcode>
          <urn5:City>Atlantis</urn5:City>
        </urn5:NewTransportArrangerTrader>
        <urn5:NewTransporterTrader language="en">
          <urn5:VatNumber>GDM23423</urn5:VatNumber>
          <urn5:TraderName>New Transporters Co.</urn5:TraderName>
          <urn5:StreetName>Main Street</urn5:StreetName>
          <urn5:StreetNumber>7654</urn5:StreetNumber>
          <urn5:Postcode>ZL23 XD1</urn5:Postcode>
          <urn5:City>Atlantis</urn5:City>
        </urn5:NewTransporterTrader>
        <urn5:TransportDetails>
          <urn5:TransportUnitCode>12</urn5:TransportUnitCode>
          <urn5:IdentityOfTransportUnits>Boxes</urn5:IdentityOfTransportUnits>
          <urn5:CommercialSealIdentification>Sealed</urn5:CommercialSealIdentification>
          <urn5:ComplementaryInformation language="en">Boxes are sealed</urn5:ComplementaryInformation>
          <urn5:SealInformation language="en">There are 33 species of seal</urn5:SealInformation>
        </urn5:TransportDetails>
        <urn5:BodyEventReport>
          <urn5:EventTypeCode>5</urn5:EventTypeCode>
          <urn5:AssociatedInformation language="en">Customs aren't happy</urn5:AssociatedInformation>
          <urn5:BodyRecordUniqueReference>244</urn5:BodyRecordUniqueReference>
          <urn5:DescriptionOfTheGoods>some are missing</urn5:DescriptionOfTheGoods>
          <urn5:CnCode>43478962</urn5:CnCode>
          <urn5:AdditionalCode>NFH412</urn5:AdditionalCode>
          <urn5:IndicatorOfShortageOrExcess>S</urn5:IndicatorOfShortageOrExcess>
          <urn5:ObservedShortageOrExcess>112</urn5:ObservedShortageOrExcess>
        </urn5:BodyEventReport>
      </urn5:EventReportEnvelope>
    </urn5:Body>
  </urn5:IE840>

  lazy val IE871: Elem =
    <urn:IE871 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE871:V3.01"
               xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
               xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
               xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <urn:Header>
        <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
        <urn1:MessageRecipient>NDEA.GB</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2023-08-15</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>09:57:17</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>GB100000000302708</urn1:MessageIdentifier>
        <urn1:CorrelationIdentifier>PORTAL56f290f317b947c79ee93b806800351b</urn1:CorrelationIdentifier>
      </urn:Header> <urn:Body>
      <urn:ExplanationOnReasonForShortage>
        <urn:Attributes>
          <urn:SubmitterType>1</urn:SubmitterType>
          <urn:DateAndTimeOfValidationOfExplanationOnShortage>2023-08-15T09:57:19</urn:DateAndTimeOfValidationOfExplanationOnShortage>
        </urn:Attributes>
        <urn:ExciseMovement>
          <urn:AdministrativeReferenceCode>23GB00000000000377768</urn:AdministrativeReferenceCode>
          <urn:SequenceNumber>1</urn:SequenceNumber>
        </urn:ExciseMovement>
        <urn:ConsignorTrader language="en">
          <urn:TraderExciseNumber>GBWK240176600</urn:TraderExciseNumber>
          <urn:TraderName>CHARLES HASWELL AND PARTNERS LTD</urn:TraderName>
          <urn:StreetName>1</urn:StreetName>
          <urn:Postcode>AA11AA</urn:Postcode>
          <urn:City>1</urn:City>
        </urn:ConsignorTrader>
        <urn:Analysis>
          <urn:DateOfAnalysis>2023-08-15</urn:DateOfAnalysis>
          <urn:GlobalExplanation language="en">Courier drank the wine</urn:GlobalExplanation>
        </urn:Analysis>
      </urn:ExplanationOnReasonForShortage>
    </urn:Body>
    </urn:IE871>

  lazy val IE881: Elem =
     <urn5:IE881 xmlns:urn5="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE881:V3.01"
                 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
        <urn5:Header>
          <urn:MessageSender>NDEA.GB</urn:MessageSender>
          <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
          <urn:DateOfPreparation>2023-07-01</urn:DateOfPreparation>
          <urn:TimeOfPreparation>03:18:33</urn:TimeOfPreparation>
          <urn:MessageIdentifier>XI00432M</urn:MessageIdentifier>
          <urn:CorrelationIdentifier>6dddas1231ff3111f3233</urn:CorrelationIdentifier>
        </urn5:Header>
        <urn5:Body>
          <urn5:ManualClosureResponse>
            <urn5:Attributes>
              <urn5:AdministrativeReferenceCode>23XI00000000000056349</urn5:AdministrativeReferenceCode>
              <urn5:SequenceNumber>1</urn5:SequenceNumber>
              <urn5:DateOfArrivalOfExciseProducts>2023-06-30</urn5:DateOfArrivalOfExciseProducts>
              <urn5:GlobalConclusionOfReceipt>3</urn5:GlobalConclusionOfReceipt>
              <urn5:ComplementaryInformation language="en">Manual closure request recieved</urn5:ComplementaryInformation>
              <urn5:ManualClosureRequestReasonCode>1</urn5:ManualClosureRequestReasonCode>
              <urn5:ManualClosureRequestReasonCodeComplement language="en">Nice try</urn5:ManualClosureRequestReasonCodeComplement>
              <urn5:ManualClosureRequestAccepted>1</urn5:ManualClosureRequestAccepted>
            </urn5:Attributes>
            <urn5:SupportingDocuments>
              <urn5:SupportingDocumentDescription language="en">XI8466333A</urn5:SupportingDocumentDescription>
              <urn5:ReferenceOfSupportingDocument language="en">Closure request</urn5:ReferenceOfSupportingDocument>
              <urn5:ImageOfDocument>Y2lyY3Vt</urn5:ImageOfDocument>
              <urn5:SupportingDocumentType>pdf</urn5:SupportingDocumentType>
            </urn5:SupportingDocuments>
            <urn5:SupportingDocuments>
              <urn5:SupportingDocumentDescription language="en">XI8466333B</urn5:SupportingDocumentDescription>
              <urn5:ReferenceOfSupportingDocument language="en">Closure request</urn5:ReferenceOfSupportingDocument>
              <urn5:ImageOfDocument>Y2lyY3Vt</urn5:ImageOfDocument>
              <urn5:SupportingDocumentType>pdf</urn5:SupportingDocumentType>
            </urn5:SupportingDocuments>
            <urn5:BodyManualClosure>
              <urn5:BodyRecordUniqueReference>11</urn5:BodyRecordUniqueReference>
              <urn5:IndicatorOfShortageOrExcess>S</urn5:IndicatorOfShortageOrExcess>
              <urn5:ObservedShortageOrExcess>1000</urn5:ObservedShortageOrExcess>
              <urn5:ExciseProductCode>W200</urn5:ExciseProductCode>
              <urn5:RefusedQuantity>1000</urn5:RefusedQuantity>
              <urn5:ComplementaryInformation language="en">Not supplied goods promised</urn5:ComplementaryInformation>
            </urn5:BodyManualClosure>
          </urn5:ManualClosureResponse>
        </urn5:Body>
      </urn5:IE881>

  lazy val IE905: Elem = <urn6:IE905 xmlns:urn6="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE905:V3.01"
                                       xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn6:Header>
        <urn:MessageSender>NDEA.GB</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2023-07-02</urn:DateOfPreparation>
        <urn:TimeOfPreparation>21:23:41</urn:TimeOfPreparation>
        <urn:MessageIdentifier>XI00432RR</urn:MessageIdentifier>
        <urn:CorrelationIdentifier>6774741231ff3111f3233</urn:CorrelationIdentifier>
      </urn6:Header>
      <urn6:Body>
        <urn6:StatusResponse>
          <urn6:Attributes>
            <urn6:AdministrativeReferenceCode>23XI00000000000056349</urn6:AdministrativeReferenceCode>
            <urn6:SequenceNumber>1</urn6:SequenceNumber>
            <urn6:Status>X07</urn6:Status>
            <urn6:LastReceivedMessageType>IE881</urn6:LastReceivedMessageType>
          </urn6:Attributes>
        </urn6:StatusResponse>
      </urn6:Body>
    </urn6:IE905>

}
