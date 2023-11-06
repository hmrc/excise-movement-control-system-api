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
  // Can we use this in the test?
  // This isn't a string, it's an XML nodeSeq I think
  // I borrowed this idea from common-transit-traders
  lazy val IE815: Elem = <urn:IE815 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.01"
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
          <urn:TraderExciseNumber>GBWK002281023</urn:TraderExciseNumber>
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
            <urn:SubmitterIdentification>GBWK240176600</urn:SubmitterIdentification>
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

}
