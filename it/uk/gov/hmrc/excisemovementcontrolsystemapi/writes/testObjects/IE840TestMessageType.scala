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

object IE840TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn5:IE840 xmlns:urn5="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE840:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
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

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-06-28\",\"TimeOfPreparation\":\"00:17:33\",\"MessageIdentifier\":\"XI0003265\",\"CorrelationIdentifier\":\"6de24ff423abcb344bbcbcbcbc3499\"},\"Body\":{\"EventReportEnvelope\":{\"AttributesValue\":{\"EventReportMessageType\":\"1\",\"DateAndTimeOfValidationOfEventReport\":\"2023-06-28T00:17:33\"},\"HeaderEventReport\":{\"EventReportNumber\":\"GBAA2C3F4244ADB3\",\"MsOfSubmissionEventReportReference\":\"GB\",\"ReferenceNumberOfExciseOffice\":\"GB848884\",\"MemberStateOfEvent\":\"GB\"},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000333\",\"SequenceNumber\":\"1\"},\"OtherAccompanyingDocument\":{\"OtherAccompanyingDocumentType\":\"0\",\"ShortDescriptionOfOtherAccompanyingDocument\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"OtherAccompanyingDocumentNumber\":\"jfj3423jfsjf\",\"OtherAccompanyingDocumentDate\":\"2023-06-15\",\"ImageOfOtherAccompanyingDocument\":[102,114,101,109,117,110,116],\"MemberStateOfDispatch\":\"to\",\"MemberStateOfDestination\":\"to\",\"PersonInvolvedInMovementTrader\":[{\"TraderExciseNumber\":\"IT34HD7413733\",\"Traderid\":\"jj4123\",\"TraderName\":\"Roman Parties Ltd\",\"TraderPersonType\":\"5\",\"MemberStateCode\":\"IT\",\"StreetName\":\"Circus Maximus\",\"StreetNumber\":\"3\",\"Postcode\":\"RM32\",\"City\":\"Rome\",\"PhoneNumber\":\"992743614324\",\"FaxNumber\":\"848848484\",\"EmailAddress\":\"test@example.com\",\"attributes\":{\"@language\":\"to\"}}],\"GoodsItem\":[{\"DescriptionOfTheGoods\":\"Booze\",\"CnCode\":\"84447744\",\"CommercialDescriptionOfTheGoods\":\"Alcoholic beverages\",\"AdditionalCode\":\"token\",\"Quantity\":1000,\"UnitOfMeasureCode\":\"12\",\"GrossMass\":1000,\"NetMass\":1000}],\"MeansOfTransport\":{\"TraderName\":\"Big Lorries Ltd\",\"StreetName\":\"Magic Road\",\"StreetNumber\":\"42\",\"TransporterCountry\":\"FR\",\"Postcode\":\"DRA231\",\"City\":\"Paris\",\"TransportModeCode\":\"12\",\"AcoComplementaryInformation\":{\"value\":\"The lorry is taking the goods\",\"attributes\":{\"@language\":\"en\"}},\"Registration\":\"tj32343\",\"CountryOfRegistration\":\"FR\"}},\"EventReport\":{\"DateOfEvent\":\"2023-06-28\",\"PlaceOfEvent\":{\"value\":\"Dover\",\"attributes\":{\"@language\":\"en\"}},\"ExciseOfficerIdentification\":\"NG28324234\",\"SubmittingPerson\":\"NG2j23432\",\"SubmittingPersonCode\":\"12\",\"SubmittingPersonComplement\":{\"value\":\"Looking good\",\"attributes\":{\"@language\":\"en\"}},\"ChangedTransportArrangement\":\"2\",\"Comments\":{\"value\":\"words\",\"attributes\":{\"@language\":\"en\"}}},\"EvidenceOfEvent\":[{\"IssuingAuthority\":{\"value\":\"CSN123\",\"attributes\":{\"@language\":\"en\"}},\"EvidenceTypeCode\":\"7\",\"ReferenceOfEvidence\":{\"value\":\"token\",\"attributes\":{\"@language\":\"en\"}},\"ImageOfEvidence\":[116,117,114,98,105,110,101],\"EvidenceTypeComplement\":{\"value\":\"Nice job\",\"attributes\":{\"@language\":\"en\"}}}],\"NewTransportArrangerTrader\":{\"VatNumber\":\"GB823482\",\"TraderName\":\"New Transport Arrangers Inc.\",\"StreetName\":\"Main Street\",\"StreetNumber\":\"7655\",\"Postcode\":\"ZL23 XD1\",\"City\":\"Atlantis\",\"attributes\":{\"@language\":\"en\"}},\"NewTransporterTrader\":{\"VatNumber\":\"GDM23423\",\"TraderName\":\"New Transporters Co.\",\"StreetName\":\"Main Street\",\"StreetNumber\":\"7654\",\"Postcode\":\"ZL23 XD1\",\"City\":\"Atlantis\",\"attributes\":{\"@language\":\"en\"}},\"TransportDetails\":[{\"TransportUnitCode\":\"12\",\"IdentityOfTransportUnits\":\"Boxes\",\"CommercialSealIdentification\":\"Sealed\",\"ComplementaryInformation\":{\"value\":\"Boxes are sealed\",\"attributes\":{\"@language\":\"en\"}},\"SealInformation\":{\"value\":\"There are 33 species of seal\",\"attributes\":{\"@language\":\"en\"}}}],\"BodyEventReport\":[{\"EventTypeCode\":\"5\",\"AssociatedInformation\":{\"value\":\"Customs aren't happy\",\"attributes\":{\"@language\":\"en\"}},\"BodyRecordUniqueReference\":\"244\",\"DescriptionOfTheGoods\":\"some are missing\",\"CnCode\":\"43478962\",\"AdditionalCode\":\"NFH412\",\"IndicatorOfShortageOrExcess\":\"S\",\"ObservedShortageOrExcess\":112}]}}}")
}
