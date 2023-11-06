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

object Ie818XmlMessage {
  lazy val IE818 = <IE818 xmlns:IE818="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.01">
    <IE818:Header>
      <MessageSender>token</MessageSender>
      <MessageRecipient>token</MessageRecipient>
      <DateOfPreparation>2008-09-29</DateOfPreparation>
      <TimeOfPreparation>00:18:33</TimeOfPreparation>
      <MessageIdentifier>token</MessageIdentifier>
      <CorrelationIdentifier>token</CorrelationIdentifier>
    </IE818:Header>
    <IE818:Body>
      <IE818:AcceptedOrRejectedReportOfReceiptExport>
        <IE818:Attributes>
          <IE818:DateAndTimeOfValidationOfReportOfReceiptExport>2006-08-19T18:27:14+01:00</IE818:DateAndTimeOfValidationOfReportOfReceiptExport>
        </IE818:Attributes>
        <IE818:ConsigneeTrader language="to">
          <IE818:Traderid>token</IE818:Traderid>
          <IE818:TraderName>token</IE818:TraderName>
          <IE818:StreetName>token</IE818:StreetName>
          <IE818:StreetNumber>token</IE818:StreetNumber>
          <IE818:Postcode>token</IE818:Postcode>
          <IE818:City>token</IE818:City>
          <IE818:EoriNumber>token</IE818:EoriNumber>
        </IE818:ConsigneeTrader>
        <IE818:ExciseMovement>
          <IE818:AdministrativeReferenceCode>tokentokentokentokent</IE818:AdministrativeReferenceCode>
          <IE818:SequenceNumber>to</IE818:SequenceNumber>
        </IE818:ExciseMovement>
        <IE818:DeliveryPlaceTrader language="to">
          <IE818:Traderid>token</IE818:Traderid>
          <IE818:TraderName>token</IE818:TraderName>
          <IE818:StreetName>token</IE818:StreetName>
          <IE818:StreetNumber>token</IE818:StreetNumber>
          <IE818:Postcode>token</IE818:Postcode>
          <IE818:City>token</IE818:City>
        </IE818:DeliveryPlaceTrader>
        <IE818:DestinationOffice>
          <IE818:ReferenceNumber>tokentok</IE818:ReferenceNumber>
        </IE818:DestinationOffice>
        <IE818:ReportOfReceiptExport>
          <IE818:DateOfArrivalOfExciseProducts>2009-05-16</IE818:DateOfArrivalOfExciseProducts>
          <IE818:GlobalConclusionOfReceipt>3</IE818:GlobalConclusionOfReceipt>
          <IE818:ComplementaryInformation language="to">token</IE818:ComplementaryInformation>
        </IE818:ReportOfReceiptExport>
        <IE818:BodyReportOfReceiptExport>
          <IE818:BodyRecordUniqueReference>tok</IE818:BodyRecordUniqueReference>
          <IE818:IndicatorOfShortageOrExcess>E</IE818:IndicatorOfShortageOrExcess>
          <IE818:ObservedShortageOrExcess>1000.00000000000</IE818:ObservedShortageOrExcess>
          <IE818:ExciseProductCode>toke</IE818:ExciseProductCode>
          <IE818:RefusedQuantity>1000.00000000000</IE818:RefusedQuantity>
          <IE818:UnsatisfactoryReason>
            <IE818:UnsatisfactoryReasonCode>to</IE818:UnsatisfactoryReasonCode>
            <IE818:ComplementaryInformation language="to">token</IE818:ComplementaryInformation>
          </IE818:UnsatisfactoryReason>
        </IE818:BodyReportOfReceiptExport>
      </IE818:AcceptedOrRejectedReportOfReceiptExport>
    </IE818:Body>
  </IE818>

}
