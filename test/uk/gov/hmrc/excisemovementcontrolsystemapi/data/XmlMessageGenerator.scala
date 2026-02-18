/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes._

import scala.xml.NodeSeq

trait XmlMessageGenerator {
  def generate(ern: String, params: MessageParams): NodeSeq
}

object XmlMessageGeneratorFactory extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    params.messageType match {
      case IE704 => IE704XmlMessageGenerator.generate(ern, params)
      case IE801 => IE801XmlMessageGenerator.generate(ern, params)
      case IE802 => IE802XmlMessageGenerator.generate(ern, params)
      case IE803 => IE803XmlMessageGenerator.generate(ern, params)
      case IE807 => IE807XmlMessageGenerator.generate(ern, params)
      case IE810 => IE810XmlMessageGenerator.generate(ern, params)
      case IE813 => IE813XmlMessageGenerator.generate(ern, params)
      case IE818 => IE818XmlMessageGenerator.generate(ern, params)
      case IE819 => IE819XmlMessageGenerator.generate(ern, params)
      case IE829 => IE829XmlMessageGenerator.generate(ern, params)
      case IE837 => IE837XmlMessageGenerator.generate(ern, params)
      case IE839 => IE839XmlMessageGenerator.generate(ern, params)
      case IE840 => IE840XmlMessageGenerator.generate(ern, params)
      case IE871 => IE871XmlMessageGenerator.generate(ern, params)
      case IE881 => IE881XmlMessageGenerator.generate(ern, params)
      case IE905 => IE905XmlMessageGenerator.generate(ern, params)
      case _     => NodeSeq.Empty
    }
}

final case class MessageParams(
  messageType: MessageTypes,
  messageIdentifier: String,
  consigneeErn: Option[String] = None,
  administrativeReferenceCode: Option[String] = None,
  localReferenceNumber: Option[String] = None,
  sequenceNumber: Int = 1,
  awaitingAcknowledgement: Option[Boolean] = None
)

object MessageParams {
  implicit val format: OFormat[MessageParams] = Json.format[MessageParams]
}

private case object IE704XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie704:IE704 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie704="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3">
      <ie704:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2008-09-29</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:18:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>9b8effe4-adca-4431-bfc2-d65bb5f1e15d</tms:CorrelationIdentifier>
      </ie704:Header>
      <ie704:Body>
        <ie704:GenericRefusalMessage>
          <ie704:Attributes>
            {
      if (params.administrativeReferenceCode.isDefined) <ie704:AdministrativeReferenceCode>{
        params.administrativeReferenceCode.get
      }</ie704:AdministrativeReferenceCode>
    }
            {
      if (params.localReferenceNumber.isDefined) <ie704:LocalReferenceNumber>{
        params.localReferenceNumber.get
      }</ie704:LocalReferenceNumber>
    }
          </ie704:Attributes>
          <ie704:FunctionalError>
            <ie704:ErrorType>4401</ie704:ErrorType>
            <ie704:ErrorReason>token</ie704:ErrorReason>
            <ie704:ErrorLocation>token</ie704:ErrorLocation>
            <ie704:OriginalAttributeValue>token</ie704:OriginalAttributeValue>
          </ie704:FunctionalError>
        </ie704:GenericRefusalMessage>
      </ie704:Body>
    </ie704:IE704>
}

private case object IE801XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie801:IE801 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie801="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.23">
      <ie801:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.AT</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-06-22</tms:DateOfPreparation>
        <tms:TimeOfPreparation>12:37:08.755</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
      </ie801:Header>
      <ie801:Body>
        <ie801:EADESADContainer>
          <ie801:ConsigneeTrader language="en">
            <ie801:Traderid>{params.consigneeErn.get}</ie801:Traderid>
            <ie801:TraderName>AFOR KALE LTD</ie801:TraderName>
            <ie801:StreetName>The Street</ie801:StreetName>
            <ie801:Postcode>AT123</ie801:Postcode>
            <ie801:City>The City</ie801:City>
          </ie801:ConsigneeTrader>
          <ie801:ExciseMovement>
            <ie801:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie801:AdministrativeReferenceCode>
            <ie801:DateAndTimeOfValidationOfEadEsad>2023-06-22T11:37:10.345739396</ie801:DateAndTimeOfValidationOfEadEsad>
          </ie801:ExciseMovement>
          <ie801:ConsignorTrader language="en">
            <ie801:TraderExciseNumber>{ern}</ie801:TraderExciseNumber>
            <ie801:TraderName>Clarkys Eagles</ie801:TraderName>
            <ie801:StreetName>Happy Street</ie801:StreetName>
            <ie801:Postcode>BT1 1BG</ie801:Postcode>
            <ie801:City>The City</ie801:City>
          </ie801:ConsignorTrader>
          <ie801:PlaceOfDispatchTrader language="en">
            <ie801:ReferenceOfTaxWarehouse>XI00000467014</ie801:ReferenceOfTaxWarehouse>
          </ie801:PlaceOfDispatchTrader>
          <ie801:DeliveryPlaceTrader language="en">
            <ie801:Traderid>AT00000602078</ie801:Traderid>
            <ie801:TraderName>METEST BOND STTSTGE</ie801:TraderName>
            <ie801:StreetName>WHITETEST ROAD METEST CITY ESTATE</ie801:StreetName>
            <ie801:Postcode>BN2 4KX</ie801:Postcode>
            <ie801:City>STTEST,KENT</ie801:City>
          </ie801:DeliveryPlaceTrader>
          <ie801:CompetentAuthorityDispatchOffice>
            <ie801:ReferenceNumber>GB004098</ie801:ReferenceNumber>
          </ie801:CompetentAuthorityDispatchOffice>
          <ie801:EadEsad>
            <ie801:LocalReferenceNumber>{params.localReferenceNumber.get}</ie801:LocalReferenceNumber>
            <ie801:InvoiceNumber>INVOICE001</ie801:InvoiceNumber>
            <ie801:InvoiceDate>2018-04-04</ie801:InvoiceDate>
            <ie801:OriginTypeCode>1</ie801:OriginTypeCode>
            <ie801:DateOfDispatch>2021-12-02</ie801:DateOfDispatch>
            <ie801:TimeOfDispatch>22:37:00</ie801:TimeOfDispatch>
          </ie801:EadEsad>
          <ie801:HeaderEadEsad>
            <ie801:SequenceNumber>{params.sequenceNumber}</ie801:SequenceNumber>
            <ie801:DateAndTimeOfUpdateValidation>2023-06-22T11:37:10.345801029</ie801:DateAndTimeOfUpdateValidation>
            <ie801:DestinationTypeCode>1</ie801:DestinationTypeCode>
            <ie801:JourneyTime>D01</ie801:JourneyTime>
            <ie801:TransportArrangement>1</ie801:TransportArrangement>
          </ie801:HeaderEadEsad>
          <ie801:TransportMode>
            <ie801:TransportModeCode>1</ie801:TransportModeCode>
          </ie801:TransportMode>
          <ie801:MovementGuarantee>
            <ie801:GuarantorTypeCode>1</ie801:GuarantorTypeCode>
          </ie801:MovementGuarantee>
          <ie801:BodyEadEsad>
            <ie801:BodyRecordUniqueReference>1</ie801:BodyRecordUniqueReference>
            <ie801:ExciseProductCode>E410</ie801:ExciseProductCode>
            <ie801:CnCode>27101231</ie801:CnCode>
            <ie801:Quantity>100.000</ie801:Quantity>
            <ie801:GrossMass>100.00</ie801:GrossMass>
            <ie801:NetMass>90.00</ie801:NetMass>
            <ie801:Density>10.00</ie801:Density>
            <ie801:Package>
              <ie801:KindOfPackages>BH</ie801:KindOfPackages>
              <ie801:NumberOfPackages>2</ie801:NumberOfPackages>
              <ie801:ShippingMarks>Subhasis Swain1</ie801:ShippingMarks>
            </ie801:Package>
            <ie801:Package>
              <ie801:KindOfPackages>BH</ie801:KindOfPackages>
              <ie801:NumberOfPackages>2</ie801:NumberOfPackages>
              <ie801:ShippingMarks>Subhasis Swain 2</ie801:ShippingMarks>
            </ie801:Package>
          </ie801:BodyEadEsad>
          <ie801:TransportDetails>
            <ie801:TransportUnitCode>1</ie801:TransportUnitCode>
            <ie801:IdentityOfTransportUnits>Transformers robots in disguise</ie801:IdentityOfTransportUnits>
          </ie801:TransportDetails>
          <ie801:TransportDetails>
            <ie801:TransportUnitCode>2</ie801:TransportUnitCode>
            <ie801:IdentityOfTransportUnits>MACHINES</ie801:IdentityOfTransportUnits>
          </ie801:TransportDetails>
          <ie801:TransportDetails>
            <ie801:TransportUnitCode>3</ie801:TransportUnitCode>
            <ie801:IdentityOfTransportUnits>MORE MACHINES</ie801:IdentityOfTransportUnits>
          </ie801:TransportDetails>
        </ie801:EADESADContainer>
      </ie801:Body>
    </ie801:IE801>
}

private case object IE802XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie802:IE802 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie802="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.23">
      <ie802:Header>
        <tms:MessageSender>CSMISE.EC</tms:MessageSender>
        <tms:MessageRecipient>CSMISE.EC</tms:MessageRecipient>
        <tms:DateOfPreparation>2008-09-29</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:18:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>X00004</tms:CorrelationIdentifier>
      </ie802:Header>
      <ie802:Body>
        <ie802:ReminderMessageForExciseMovement>
          <ie802:Attributes>
            <ie802:DateAndTimeOfIssuanceOfReminder>2006-08-19T18:27:14</ie802:DateAndTimeOfIssuanceOfReminder>
            <ie802:ReminderInformation language="to">token</ie802:ReminderInformation>
            <ie802:LimitDateAndTime>2009-05-16T13:42:28</ie802:LimitDateAndTime>
            <ie802:ReminderMessageType>2</ie802:ReminderMessageType>
          </ie802:Attributes>
          <ie802:ExciseMovement>
            <ie802:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie802:AdministrativeReferenceCode>
            <ie802:SequenceNumber>{params.sequenceNumber}</ie802:SequenceNumber>
          </ie802:ExciseMovement>
        </ie802:ReminderMessageForExciseMovement>
      </ie802:Body>
    </ie802:IE802>
}

private case object IE803XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie803:IE803 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie803="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE803:V3.23">
      <ie803:Header>
        <tms:MessageSender>NDEA.GB</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-06-27</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:23:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>6dddasfffff3abcb344bbcbcbcbc3435</tms:CorrelationIdentifier>
      </ie803:Header>
      <ie803:Body>
        <ie803:NotificationOfDivertedEADESAD>
          <ie803:ExciseNotification>
            <ie803:NotificationType>1</ie803:NotificationType>
            <ie803:NotificationDateAndTime>2023-06-26T23:56:46</ie803:NotificationDateAndTime>
            <ie803:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie803:AdministrativeReferenceCode>
            <ie803:SequenceNumber>1</ie803:SequenceNumber>
          </ie803:ExciseNotification>
        </ie803:NotificationOfDivertedEADESAD>
      </ie803:Body>
    </ie803:IE803>
}

private case object IE807XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie807:IE807 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie807="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE807:V3.23">
      <ie807:Header>
        <tms:MessageSender>NDEA.GB</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-06-27</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:18:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>6de24ff423abcb344bbcbcbcbc3423</tms:CorrelationIdentifier>
      </ie807:Header>
      <ie807:Body>
        <ie807:InterruptionOfMovement>
          <ie807:Attributes>
            <ie807:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie807:AdministrativeReferenceCode>
            <ie807:ComplementaryInformation language="to">Customs aren't happy :(</ie807:ComplementaryInformation>
            <ie807:DateAndTimeOfIssuance>2023-06-27T00:18:13</ie807:DateAndTimeOfIssuance>
            <ie807:ReasonForInterruptionCode>1</ie807:ReasonForInterruptionCode>
            <ie807:ReferenceNumberOfExciseOffice>AB737333</ie807:ReferenceNumberOfExciseOffice>
            <ie807:ExciseOfficerIdentification>GB3939939393</ie807:ExciseOfficerIdentification>
          </ie807:Attributes>
          <ie807:ReferenceControlReport>
            <ie807:ControlReportReference>GBAA2C3F4244ADB9</ie807:ControlReportReference>
          </ie807:ReferenceControlReport>
          <ie807:ReferenceControlReport>
            <ie807:ControlReportReference>GBAA2C3F4244ADB8</ie807:ControlReportReference>
          </ie807:ReferenceControlReport>
          <ie807:ReferenceEventReport>
            <ie807:EventReportNumber>GBAA2C3F4244ADB3</ie807:EventReportNumber>
          </ie807:ReferenceEventReport>
        </ie807:InterruptionOfMovement>
      </ie807:Body>
    </ie807:IE807>
}

private case object IE810XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie810:IE810 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie810="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.23">
      <ie810:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2008-09-29</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:18:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>a4dd7694ccef4055abb1dfef7ff06f49</tms:CorrelationIdentifier>
      </ie810:Header>
      <ie810:Body>
        <ie810:CancellationOfEAD>
          <ie810:Attributes>
            <ie810:DateAndTimeOfValidationOfCancellation>2006-08-19T18:27:14</ie810:DateAndTimeOfValidationOfCancellation>
          </ie810:Attributes>
          <ie810:ExciseMovementEad>
            <ie810:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie810:AdministrativeReferenceCode>
          </ie810:ExciseMovementEad>
          <ie810:Cancellation>
            <ie810:CancellationReasonCode>1</ie810:CancellationReasonCode>
            <ie810:ComplementaryInformation language="to">token</ie810:ComplementaryInformation>
          </ie810:Cancellation>
        </ie810:CancellationOfEAD>
      </ie810:Body>
    </ie810:IE810>
}

private case object IE813XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie813:IE813 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie813="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE813:V3.23">
      <ie813:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2012-01-08</tms:DateOfPreparation>
        <tms:TimeOfPreparation>13:43:21</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>3e5a6399-0152-4883-9d37-9d11e635ddc5</tms:CorrelationIdentifier>
      </ie813:Header>
      <ie813:Body>
        <ie813:ChangeOfDestination>
          <ie813:Attributes>
            <ie813:DateAndTimeOfValidationOfChangeOfDestination>2001-08-29T10:32:42</ie813:DateAndTimeOfValidationOfChangeOfDestination>
          </ie813:Attributes>
          <ie813:NewTransportArrangerTrader language="to">
            <ie813:VatNumber>token</ie813:VatNumber>
            <ie813:TraderName>token</ie813:TraderName>
            <ie813:StreetName>token</ie813:StreetName>
            <ie813:StreetNumber>token</ie813:StreetNumber>
            <ie813:Postcode>token</ie813:Postcode>
            <ie813:City>token</ie813:City>
          </ie813:NewTransportArrangerTrader>
          <ie813:UpdateEadEsad>
            <ie813:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie813:AdministrativeReferenceCode>
            <ie813:JourneyTime>D01</ie813:JourneyTime>
            <ie813:ChangedTransportArrangement>4</ie813:ChangedTransportArrangement>
            <ie813:SequenceNumber>{params.sequenceNumber}</ie813:SequenceNumber>
            <ie813:InvoiceDate>2019-05-04</ie813:InvoiceDate>
            <ie813:InvoiceNumber>token</ie813:InvoiceNumber>
            <ie813:TransportModeCode>4</ie813:TransportModeCode>
            <ie813:ComplementaryInformation language="to">token</ie813:ComplementaryInformation>
          </ie813:UpdateEadEsad>
          <ie813:DestinationChanged>
            <ie813:DestinationTypeCode>3</ie813:DestinationTypeCode>
            <ie813:NewConsigneeTrader language="to">
              <ie813:Traderid>{params.consigneeErn.get}</ie813:Traderid>
              <ie813:TraderName>token</ie813:TraderName>
              <ie813:StreetName>token</ie813:StreetName>
              <ie813:StreetNumber>token</ie813:StreetNumber>
              <ie813:Postcode>token</ie813:Postcode>
              <ie813:City>token</ie813:City>
              <ie813:EoriNumber>token</ie813:EoriNumber>
            </ie813:NewConsigneeTrader>
            <ie813:DeliveryPlaceTrader language="to">
              <ie813:Traderid>GBWK005981023</ie813:Traderid>
              <ie813:TraderName>token</ie813:TraderName>
              <ie813:StreetName>token</ie813:StreetName>
              <ie813:StreetNumber>token</ie813:StreetNumber>
              <ie813:Postcode>token</ie813:Postcode>
              <ie813:City>token</ie813:City>
            </ie813:DeliveryPlaceTrader>
            <ie813:DeliveryPlaceCustomsOffice>
              <ie813:ReferenceNumber>GB004049</ie813:ReferenceNumber>
            </ie813:DeliveryPlaceCustomsOffice>
            <ie813:MovementGuarantee>
              <ie813:GuarantorTypeCode>12</ie813:GuarantorTypeCode>
              <ie813:GuarantorTrader language="to">
                <ie813:TraderExciseNumber>{ern}</ie813:TraderExciseNumber>
                <ie813:TraderName>token</ie813:TraderName>
                <ie813:StreetName>token</ie813:StreetName>
                <ie813:StreetNumber>token</ie813:StreetNumber>
                <ie813:City>token</ie813:City>
                <ie813:Postcode>token</ie813:Postcode>
                <ie813:VatNumber>token</ie813:VatNumber>
              </ie813:GuarantorTrader>
            </ie813:MovementGuarantee>
          </ie813:DestinationChanged>
          <ie813:NewTransporterTrader language="to">
            <ie813:VatNumber>token</ie813:VatNumber>
            <ie813:TraderName>token</ie813:TraderName>
            <ie813:StreetName>token</ie813:StreetName>
            <ie813:StreetNumber>token</ie813:StreetNumber>
            <ie813:Postcode>token</ie813:Postcode>
            <ie813:City>token</ie813:City>
          </ie813:NewTransporterTrader>
          <ie813:TransportDetails>
            <ie813:TransportUnitCode>12</ie813:TransportUnitCode>
            <ie813:IdentityOfTransportUnits>token</ie813:IdentityOfTransportUnits>
            <ie813:CommercialSealIdentification>token</ie813:CommercialSealIdentification>
            <ie813:ComplementaryInformation language="to">token</ie813:ComplementaryInformation>
            <ie813:SealInformation language="to">token</ie813:SealInformation>
          </ie813:TransportDetails>
        </ie813:ChangeOfDestination>
      </ie813:Body>
    </ie813:IE813>
}

private case object IE818XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie818:IE818 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie818="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.23">
      <ie818:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2006-08-04</tms:DateOfPreparation>
        <tms:TimeOfPreparation>09:43:40</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>8a4127de-904f-46ca-a779-70238f21c4bd</tms:CorrelationIdentifier>
      </ie818:Header>
      <ie818:Body>
        <ie818:AcceptedOrRejectedReportOfReceiptExport>
          <ie818:Attributes>
            <ie818:DateAndTimeOfValidationOfReportOfReceiptExport>2001-01-03T11:25:01</ie818:DateAndTimeOfValidationOfReportOfReceiptExport>
          </ie818:Attributes>
          <ie818:ConsigneeTrader language="to">
            <ie818:Traderid>{params.consigneeErn.get}</ie818:Traderid>
            <ie818:TraderName>token</ie818:TraderName>
            <ie818:StreetName>token</ie818:StreetName>
            <ie818:StreetNumber>token</ie818:StreetNumber>
            <ie818:Postcode>token</ie818:Postcode>
            <ie818:City>token</ie818:City>
            <ie818:EoriNumber>token</ie818:EoriNumber>
          </ie818:ConsigneeTrader>
          <ie818:ExciseMovement>
            <ie818:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie818:AdministrativeReferenceCode>
            <ie818:SequenceNumber>{params.sequenceNumber}</ie818:SequenceNumber>
          </ie818:ExciseMovement>
          <ie818:DeliveryPlaceTrader language="to">
            <ie818:Traderid>token</ie818:Traderid>
            <ie818:TraderName>token</ie818:TraderName>
            <ie818:StreetName>token</ie818:StreetName>
            <ie818:StreetNumber>token</ie818:StreetNumber>
            <ie818:Postcode>token</ie818:Postcode>
            <ie818:City>token</ie818:City>
          </ie818:DeliveryPlaceTrader>
          <ie818:DestinationOffice>
            <ie818:ReferenceNumber>GB005045</ie818:ReferenceNumber>
          </ie818:DestinationOffice>
          <ie818:ReportOfReceiptExport>
            <ie818:DateOfArrivalOfExciseProducts>2014-01-10</ie818:DateOfArrivalOfExciseProducts>
            <ie818:GlobalConclusionOfReceipt>22</ie818:GlobalConclusionOfReceipt>
            <ie818:ComplementaryInformation language="to">token</ie818:ComplementaryInformation>
          </ie818:ReportOfReceiptExport>
          <ie818:BodyReportOfReceiptExport>
            <ie818:BodyRecordUniqueReference>123</ie818:BodyRecordUniqueReference>
            <ie818:IndicatorOfShortageOrExcess>S</ie818:IndicatorOfShortageOrExcess>
            <ie818:ObservedShortageOrExcess>1000.0</ie818:ObservedShortageOrExcess>
            <ie818:ExciseProductCode>toke</ie818:ExciseProductCode>
            <ie818:RefusedQuantity>1000.0</ie818:RefusedQuantity>
            <ie818:UnsatisfactoryReason>
              <ie818:UnsatisfactoryReasonCode>12</ie818:UnsatisfactoryReasonCode>
              <ie818:ComplementaryInformation language="to">token</ie818:ComplementaryInformation>
            </ie818:UnsatisfactoryReason>
          </ie818:BodyReportOfReceiptExport>
        </ie818:AcceptedOrRejectedReportOfReceiptExport>
      </ie818:Body>
    </ie818:IE818>
}

private case object IE819XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie819:IE819 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie819="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE819:V3.23">
      <ie819:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2008-09-29</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:18:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>708d70b7-bbc5-4790-92aa-cfb4b19abc42</tms:CorrelationIdentifier>
      </ie819:Header>
      <ie819:Body>
        <ie819:AlertOrRejectionOfEADESAD>
          <ie819:Attributes>
            <ie819:DateAndTimeOfValidationOfAlertRejection>2006-08-19T18:27:14</ie819:DateAndTimeOfValidationOfAlertRejection>
          </ie819:Attributes>
          <ie819:ConsigneeTrader language="to">
            <ie819:Traderid>{params.consigneeErn.get}</ie819:Traderid>
            <ie819:TraderName>token</ie819:TraderName>
            <ie819:StreetName>token</ie819:StreetName>
            <ie819:StreetNumber>token</ie819:StreetNumber>
            <ie819:Postcode>token</ie819:Postcode>
            <ie819:City>token</ie819:City>
            <ie819:EoriNumber>token</ie819:EoriNumber>
          </ie819:ConsigneeTrader>
          <ie819:ExciseMovement>
            <ie819:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie819:AdministrativeReferenceCode>
            <ie819:SequenceNumber>12</ie819:SequenceNumber>
          </ie819:ExciseMovement>
          <ie819:DestinationOffice>
            <ie819:ReferenceNumber>GB004022</ie819:ReferenceNumber>
          </ie819:DestinationOffice>
          <ie819:AlertOrRejection>
            <ie819:DateOfAlertOrRejection>2009-05-16</ie819:DateOfAlertOrRejection>
            <ie819:EadEsadRejectedFlag>1</ie819:EadEsadRejectedFlag>
          </ie819:AlertOrRejection>
          <ie819:AlertOrRejectionOfEadEsadReason>
            <ie819:AlertOrRejectionOfMovementReasonCode>3</ie819:AlertOrRejectionOfMovementReasonCode>
            <ie819:ComplementaryInformation language="to">token</ie819:ComplementaryInformation>
          </ie819:AlertOrRejectionOfEadEsadReason>
        </ie819:AlertOrRejectionOfEADESAD>
      </ie819:Body>
    </ie819:IE819>
}

private case object IE829XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie829:IE829 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie829="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.23">
      <ie829:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.AT</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-06-26</tms:DateOfPreparation>
        <tms:TimeOfPreparation>09:15:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>6dddas342231ff3a67888bbcedec3435</tms:CorrelationIdentifier>
      </ie829:Header>
      <ie829:Body>
        <ie829:NotificationOfAcceptedExport>
          <ie829:Attributes>
            <ie829:DateAndTimeOfIssuance>2024-06-26T09:14:54</ie829:DateAndTimeOfIssuance>
          </ie829:Attributes>
          <ie829:ConsigneeTrader language="en">
            <ie829:Traderid>{params.consigneeErn.get}</ie829:Traderid>
            <ie829:TraderName>Whale Oil Lamps Co.</ie829:TraderName>
            <ie829:StreetName>The Street</ie829:StreetName>
            <ie829:Postcode>MC232</ie829:Postcode>
            <ie829:City>Happy Town</ie829:City>
            <ie829:EoriNumber>7</ie829:EoriNumber>
          </ie829:ConsigneeTrader>
          <ie829:ExciseMovementEad>
            <ie829:AdministrativeReferenceCode>23XI00000000000056339</ie829:AdministrativeReferenceCode>
            <ie829:SequenceNumber>1</ie829:SequenceNumber>
          </ie829:ExciseMovementEad>
          <ie829:ExciseMovementEad>
            <ie829:AdministrativeReferenceCode>23XI00000000000056340</ie829:AdministrativeReferenceCode>
            <ie829:SequenceNumber>1</ie829:SequenceNumber>
          </ie829:ExciseMovementEad>
          <ie829:ExportPlaceCustomsOffice>
            <ie829:ReferenceNumber>AT633734</ie829:ReferenceNumber>
          </ie829:ExportPlaceCustomsOffice>
          <ie829:ExportDeclarationAcceptanceRelease>
            <ie829:ReferenceNumberOfSenderCustomsOffice>AT324234</ie829:ReferenceNumberOfSenderCustomsOffice>
            <ie829:IdentificationOfSenderCustomsOfficer>84884</ie829:IdentificationOfSenderCustomsOfficer>
            <ie829:DateOfAcceptance>2023-06-26</ie829:DateOfAcceptance>
            <ie829:DocumentReferenceNumber>123123vmnfhsdf3AT</ie829:DocumentReferenceNumber>
            <ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>0</ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>
          </ie829:ExportDeclarationAcceptanceRelease>
        </ie829:NotificationOfAcceptedExport>
      </ie829:Body>
    </ie829:IE829>
}

private case object IE837XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <urn:IE837 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.23"
               xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23">
      <urn:Header>
        <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
        <urn1:MessageRecipient>NDEA.EU</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2023-08-10</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>09:56:40.695540</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>{params.messageIdentifier}</urn1:MessageIdentifier>
        <urn1:CorrelationIdentifier>a2f65a81-c297-4117-bea5-556129529463</urn1:CorrelationIdentifier>
      </urn:Header>
      <urn:Body>
        <urn:ExplanationOnDelayForDelivery>
          <urn:Attributes>
            <urn:SubmitterIdentification>{ern}</urn:SubmitterIdentification>
            <urn:SubmitterType>2</urn:SubmitterType>
            <urn:ExplanationCode>6</urn:ExplanationCode>
            <urn:ComplementaryInformation language="en">Accident on M5</urn:ComplementaryInformation>
            <urn:MessageRole>1</urn:MessageRole>
            <urn:DateAndTimeOfValidationOfExplanationOnDelay>2023-08-10T10:56:42</urn:DateAndTimeOfValidationOfExplanationOnDelay>
          </urn:Attributes>
          <urn:ExciseMovement>
            <urn:AdministrativeReferenceCode>{params.administrativeReferenceCode}</urn:AdministrativeReferenceCode>
            <urn:SequenceNumber>2</urn:SequenceNumber>
          </urn:ExciseMovement>
        </urn:ExplanationOnDelayForDelivery>
      </urn:Body>
    </urn:IE837>
}

private case object IE839XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie839:IE839 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie839="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.23">
      <ie839:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.FR</tms:MessageRecipient>
        <tms:DateOfPreparation>2024-06-26</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:18:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>6dddas1231ff3a678fefffff3233</tms:CorrelationIdentifier>
      </ie839:Header>
      <ie839:Body>
        <ie839:RefusalByCustoms>
          <ie839:Attributes>
            <ie839:DateAndTimeOfIssuance>2023-06-24T18:27:14</ie839:DateAndTimeOfIssuance>
          </ie839:Attributes>
          <ie839:ConsigneeTrader language="en">
            <ie839:Traderid>{params.consigneeErn.get}</ie839:Traderid>
            <ie839:TraderName>Chaz's Cigars</ie839:TraderName>
            <ie839:StreetName>The Street</ie839:StreetName>
            <ie839:Postcode>MC232</ie839:Postcode>
            <ie839:City>Happy Town</ie839:City>
            <ie839:EoriNumber>91</ie839:EoriNumber>
          </ie839:ConsigneeTrader>
          <ie839:ExportPlaceCustomsOffice>
            <ie839:ReferenceNumber>FR883393</ie839:ReferenceNumber>
          </ie839:ExportPlaceCustomsOffice>
          <ie839:Rejection>
            <ie839:RejectionDateAndTime>2023-06-22T02:02:49</ie839:RejectionDateAndTime>
            <ie839:RejectionReasonCode>4</ie839:RejectionReasonCode>
          </ie839:Rejection>
          <ie839:ExportDeclarationInformation>
            <ie839:LocalReferenceNumber>{params.localReferenceNumber.get}</ie839:LocalReferenceNumber>
            <ie839:DocumentReferenceNumber>123</ie839:DocumentReferenceNumber>
            <ie839:NegativeCrosscheckValidationResults>
              <ie839:UbrCrosscheckResult>
                <ie839:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie839:AdministrativeReferenceCode>
                <ie839:BodyRecordUniqueReference>1</ie839:BodyRecordUniqueReference>
                <ie839:DiagnosisCode>5</ie839:DiagnosisCode>
                <ie839:ValidationResult>1</ie839:ValidationResult>
              </ie839:UbrCrosscheckResult>
            </ie839:NegativeCrosscheckValidationResults>
          </ie839:ExportDeclarationInformation>
          <ie839:CEadVal>
            <ie839:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie839:AdministrativeReferenceCode>
            <ie839:SequenceNumber>1</ie839:SequenceNumber>
          </ie839:CEadVal>
          <ie839:NEadSub>
            <ie839:LocalReferenceNumber>{params.localReferenceNumber.get}</ie839:LocalReferenceNumber>
          </ie839:NEadSub>
        </ie839:RefusalByCustoms>
      </ie839:Body>
    </ie839:IE839>
}

private case object IE840XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq = {
    <ie840:IE840 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie840="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE840:V3.23">
      <ie840:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.GB</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-06-28</tms:DateOfPreparation>
        <tms:TimeOfPreparation>00:17:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>6de24ff423abcb344bbcbcbcbc3499</tms:CorrelationIdentifier>
      </ie840:Header>
      <ie840:Body>
        <ie840:EventReportEnvelope>
          <ie840:Attributes>
            <ie840:EventReportMessageType>1</ie840:EventReportMessageType>
            <ie840:DateAndTimeOfValidationOfEventReport>2023-06-28T00:17:33</ie840:DateAndTimeOfValidationOfEventReport>
          </ie840:Attributes>
          <ie840:HeaderEventReport>
            <ie840:EventReportNumber>GBAA2C3F4244ADB3</ie840:EventReportNumber>
            <ie840:MsOfSubmissionEventReportReference>GB</ie840:MsOfSubmissionEventReportReference>
            <ie840:ReferenceNumberOfExciseOffice>GB848884</ie840:ReferenceNumberOfExciseOffice>
            <ie840:MemberStateOfEvent>GB</ie840:MemberStateOfEvent>
          </ie840:HeaderEventReport>
          <ie840:ExciseMovement>
            <ie840:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie840:AdministrativeReferenceCode>
            <ie840:SequenceNumber>1</ie840:SequenceNumber>
          </ie840:ExciseMovement>
          <ie840:OtherAccompanyingDocument>
            <ie840:OtherAccompanyingDocumentType>0</ie840:OtherAccompanyingDocumentType>
            <ie840:ShortDescriptionOfOtherAccompanyingDocument language="to">token</ie840:ShortDescriptionOfOtherAccompanyingDocument>
            <ie840:OtherAccompanyingDocumentNumber>jfj3423jfsjf</ie840:OtherAccompanyingDocumentNumber>
            <ie840:OtherAccompanyingDocumentDate>2023-06-15</ie840:OtherAccompanyingDocumentDate>
            <ie840:ImageOfOtherAccompanyingDocument>ZnJlbXVudA==</ie840:ImageOfOtherAccompanyingDocument>
            <ie840:MemberStateOfDispatch>to</ie840:MemberStateOfDispatch>
            <ie840:MemberStateOfDestination>to</ie840:MemberStateOfDestination>
            <ie840:PersonInvolvedInMovementTrader language="to">
              <ie840:TraderExciseNumber>IT34HD7413733</ie840:TraderExciseNumber>
              <ie840:Traderid>jj4123</ie840:Traderid>
              <ie840:TraderName>Roman Parties Ltd</ie840:TraderName>
              <ie840:TraderPersonType>5</ie840:TraderPersonType>
              <ie840:MemberStateCode>IT</ie840:MemberStateCode>
              <ie840:StreetName>Circus Maximus</ie840:StreetName>
              <ie840:StreetNumber>3</ie840:StreetNumber>
              <ie840:Postcode>RM32</ie840:Postcode>
              <ie840:City>Rome</ie840:City>
              <ie840:PhoneNumber>992743614324</ie840:PhoneNumber>
              <ie840:FaxNumber>848848484</ie840:FaxNumber>
              <ie840:EmailAddress>test@example.com</ie840:EmailAddress>
            </ie840:PersonInvolvedInMovementTrader>
            <ie840:GoodsItem>
              <ie840:DescriptionOfTheGoods>Booze</ie840:DescriptionOfTheGoods>
              <ie840:CnCode>84447744</ie840:CnCode>
              <ie840:CommercialDescriptionOfTheGoods>Alcoholic beverages</ie840:CommercialDescriptionOfTheGoods>
              <ie840:AdditionalCode>token</ie840:AdditionalCode>
              <ie840:Quantity>1000</ie840:Quantity>
              <ie840:UnitOfMeasureCode>12</ie840:UnitOfMeasureCode>
              <ie840:GrossMass>1000</ie840:GrossMass>
              <ie840:NetMass>1000</ie840:NetMass>
            </ie840:GoodsItem>
            <ie840:MeansOfTransport>
              <ie840:TraderName>Big Lorries Ltd</ie840:TraderName>
              <ie840:StreetName>Magic Road</ie840:StreetName>
              <ie840:StreetNumber>42</ie840:StreetNumber>
              <ie840:TransporterCountry>FR</ie840:TransporterCountry>
              <ie840:Postcode>DRA231</ie840:Postcode>
              <ie840:City>Paris</ie840:City>
              <ie840:TransportModeCode>12</ie840:TransportModeCode>
              <ie840:AcoComplementaryInformation language="en">The lorry is taking the goods</ie840:AcoComplementaryInformation>
              <ie840:Registration>tj32343</ie840:Registration>
              <ie840:CountryOfRegistration>FR</ie840:CountryOfRegistration>
            </ie840:MeansOfTransport>
          </ie840:OtherAccompanyingDocument>
          <ie840:EventReport>
            <ie840:DateOfEvent>2023-06-28</ie840:DateOfEvent>
            <ie840:PlaceOfEvent language="en">Dover</ie840:PlaceOfEvent>
            <ie840:ExciseOfficerIdentification>NG28324234</ie840:ExciseOfficerIdentification>
            <ie840:SubmittingPerson>NG2j23432</ie840:SubmittingPerson>
            <ie840:SubmittingPersonCode>12</ie840:SubmittingPersonCode>
            <ie840:SubmittingPersonComplement language="en">Looking good</ie840:SubmittingPersonComplement>
            <ie840:ChangedTransportArrangement>2</ie840:ChangedTransportArrangement>
            <ie840:Comments language="en">words</ie840:Comments>
          </ie840:EventReport>
          <ie840:EvidenceOfEvent>
            <ie840:IssuingAuthority language="en">CSN123</ie840:IssuingAuthority>
            <ie840:EvidenceTypeCode>7</ie840:EvidenceTypeCode>
            <ie840:ReferenceOfEvidence language="en">token</ie840:ReferenceOfEvidence>
            <ie840:ImageOfEvidence>dHVyYmluZQ==</ie840:ImageOfEvidence>
            <ie840:EvidenceTypeComplement language="en">Nice job</ie840:EvidenceTypeComplement>
          </ie840:EvidenceOfEvent>
          <ie840:NewTransportArrangerTrader language="en">
            <ie840:VatNumber>GB823482</ie840:VatNumber>
            <ie840:TraderName>New Transport Arrangers Inc.</ie840:TraderName>
            <ie840:StreetName>Main Street</ie840:StreetName>
            <ie840:StreetNumber>7655</ie840:StreetNumber>
            <ie840:Postcode>ZL23 XD1</ie840:Postcode>
            <ie840:City>Atlantis</ie840:City>
          </ie840:NewTransportArrangerTrader>
          <ie840:NewTransporterTrader language="en">
            <ie840:VatNumber>GDM23423</ie840:VatNumber>
            <ie840:TraderName>New Transporters Co.</ie840:TraderName>
            <ie840:StreetName>Main Street</ie840:StreetName>
            <ie840:StreetNumber>7654</ie840:StreetNumber>
            <ie840:Postcode>ZL23 XD1</ie840:Postcode>
            <ie840:City>Atlantis</ie840:City>
          </ie840:NewTransporterTrader>
          <ie840:TransportDetails>
            <ie840:TransportUnitCode>12</ie840:TransportUnitCode>
            <ie840:IdentityOfTransportUnits>Boxes</ie840:IdentityOfTransportUnits>
            <ie840:CommercialSealIdentification>Sealed</ie840:CommercialSealIdentification>
            <ie840:ComplementaryInformation language="en">Boxes are sealed</ie840:ComplementaryInformation>
            <ie840:SealInformation language="en">There are 33 species of seal</ie840:SealInformation>
          </ie840:TransportDetails>
          <ie840:BodyEventReport>
            <ie840:EventTypeCode>5</ie840:EventTypeCode>
            <ie840:AssociatedInformation language="en">Customs aren't happy</ie840:AssociatedInformation>
            <ie840:BodyRecordUniqueReference>244</ie840:BodyRecordUniqueReference>
            <ie840:DescriptionOfTheGoods>some are missing</ie840:DescriptionOfTheGoods>
            <ie840:CnCode>43478962</ie840:CnCode>
            <ie840:AdditionalCode>NFH412</ie840:AdditionalCode>
            <ie840:IndicatorOfShortageOrExcess>S</ie840:IndicatorOfShortageOrExcess>
            <ie840:ObservedShortageOrExcess>112</ie840:ObservedShortageOrExcess>
          </ie840:BodyEventReport>
        </ie840:EventReportEnvelope>
      </ie840:Body>
    </ie840:IE840>
  }
}

private case object IE871XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie871:IE871 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie871="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE871:V3.23">
      <ie871:Header>
        <tms:MessageSender>NDEA.XI</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2015-08-06</tms:DateOfPreparation>
        <tms:TimeOfPreparation>03:44:00</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>054f764f07d24664b0d2351dfd19d09d</tms:CorrelationIdentifier>
      </ie871:Header>
      <ie871:Body>
        <ie871:ExplanationOnReasonForShortage>
          <ie871:Attributes>
            <ie871:SubmitterType>2</ie871:SubmitterType>
            <ie871:DateAndTimeOfValidationOfExplanationOnShortage>2006-12-27T09:49:58</ie871:DateAndTimeOfValidationOfExplanationOnShortage>
          </ie871:Attributes>
          <ie871:ConsigneeTrader language="to">
            <ie871:Traderid>{params.consigneeErn.get}</ie871:Traderid>
            <ie871:TraderName>token</ie871:TraderName>
            <ie871:StreetName>token</ie871:StreetName>
            <ie871:StreetNumber>token</ie871:StreetNumber>
            <ie871:Postcode>token</ie871:Postcode>
            <ie871:City>token</ie871:City>
            <ie871:EoriNumber>token</ie871:EoriNumber>
          </ie871:ConsigneeTrader>
          <ie871:ExciseMovement>
            <ie871:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie871:AdministrativeReferenceCode>
            <ie871:SequenceNumber>12</ie871:SequenceNumber>
          </ie871:ExciseMovement>
          <ie871:ConsignorTrader language="to">
            <ie871:TraderExciseNumber>{ern}</ie871:TraderExciseNumber>
            <ie871:TraderName>token</ie871:TraderName>
            <ie871:StreetName>token</ie871:StreetName>
            <ie871:StreetNumber>token</ie871:StreetNumber>
            <ie871:Postcode>token</ie871:Postcode>
            <ie871:City>token</ie871:City>
          </ie871:ConsignorTrader>
          <ie871:Analysis>
            <ie871:DateOfAnalysis>2002-02-01</ie871:DateOfAnalysis>
            <ie871:GlobalExplanation language="to">token</ie871:GlobalExplanation>
          </ie871:Analysis>
          <ie871:BodyAnalysis>
            <ie871:ExciseProductCode>toke</ie871:ExciseProductCode>
            <ie871:BodyRecordUniqueReference>45</ie871:BodyRecordUniqueReference>
            <ie871:Explanation language="to">token</ie871:Explanation>
            <ie871:ActualQuantity>1000.0</ie871:ActualQuantity>
          </ie871:BodyAnalysis>
        </ie871:ExplanationOnReasonForShortage>
      </ie871:Body>
    </ie871:IE871>
}

private case object IE881XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie881:IE881 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie881="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE881:V3.23">
      <ie881:Header>
        <tms:MessageSender>NDEA.GB</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-07-01</tms:DateOfPreparation>
        <tms:TimeOfPreparation>03:18:33</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>6dddas1231ff3111f3233</tms:CorrelationIdentifier>
      </ie881:Header>
      <ie881:Body>
        <ie881:ManualClosureResponse>
          <ie881:Attributes>
            <ie881:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie881:AdministrativeReferenceCode>
            <ie881:SequenceNumber>1</ie881:SequenceNumber>
            <ie881:DateOfArrivalOfExciseProducts>2023-06-30</ie881:DateOfArrivalOfExciseProducts>
            <ie881:GlobalConclusionOfReceipt>3</ie881:GlobalConclusionOfReceipt>
            <ie881:ComplementaryInformation language="en">Manual closure request recieved</ie881:ComplementaryInformation>
            <ie881:ManualClosureRequestReasonCode>1</ie881:ManualClosureRequestReasonCode>
            <ie881:ManualClosureRequestReasonCodeComplement language="en">Nice try</ie881:ManualClosureRequestReasonCodeComplement>
            <ie881:ManualClosureRequestAccepted>1</ie881:ManualClosureRequestAccepted>
          </ie881:Attributes>
          <ie881:SupportingDocuments>
            <ie881:SupportingDocumentDescription language="en">XI8466333A</ie881:SupportingDocumentDescription>
            <ie881:ReferenceOfSupportingDocument language="en">Closure request</ie881:ReferenceOfSupportingDocument>
            <ie881:ImageOfDocument>Y2lyY3Vt</ie881:ImageOfDocument>
            <ie881:SupportingDocumentType>pdf</ie881:SupportingDocumentType>
          </ie881:SupportingDocuments>
          <ie881:SupportingDocuments>
            <ie881:SupportingDocumentDescription language="en">XI8466333B</ie881:SupportingDocumentDescription>
            <ie881:ReferenceOfSupportingDocument language="en">Closure request</ie881:ReferenceOfSupportingDocument>
            <ie881:ImageOfDocument>Y2lyY3Vt</ie881:ImageOfDocument>
            <ie881:SupportingDocumentType>pdf</ie881:SupportingDocumentType>
          </ie881:SupportingDocuments>
          <ie881:BodyManualClosure>
            <ie881:BodyRecordUniqueReference>11</ie881:BodyRecordUniqueReference>
            <ie881:IndicatorOfShortageOrExcess>S</ie881:IndicatorOfShortageOrExcess>
            <ie881:ObservedShortageOrExcess>1000</ie881:ObservedShortageOrExcess>
            <ie881:ExciseProductCode>W200</ie881:ExciseProductCode>
            <ie881:RefusedQuantity>1000</ie881:RefusedQuantity>
            <ie881:ComplementaryInformation language="en">Not supplied goods promised</ie881:ComplementaryInformation>
          </ie881:BodyManualClosure>
        </ie881:ManualClosureResponse>
      </ie881:Body>
    </ie881:IE881>
}

private case object IE905XmlMessageGenerator extends XmlMessageGenerator {
  override def generate(ern: String, params: MessageParams): NodeSeq =
    <ie905:IE905 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23"
                 xmlns:ie905="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE905:V3.23">
      <ie905:Header>
        <tms:MessageSender>NDEA.GB</tms:MessageSender>
        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
        <tms:DateOfPreparation>2023-07-02</tms:DateOfPreparation>
        <tms:TimeOfPreparation>21:23:41</tms:TimeOfPreparation>
        <tms:MessageIdentifier>{params.messageIdentifier}</tms:MessageIdentifier>
        <tms:CorrelationIdentifier>6774741231ff3111f3233</tms:CorrelationIdentifier>
      </ie905:Header>
      <ie905:Body>
        <ie905:StatusResponse>
          <ie905:Attributes>
            <ie905:AdministrativeReferenceCode>{params.administrativeReferenceCode.get}</ie905:AdministrativeReferenceCode>
            <ie905:SequenceNumber>1</ie905:SequenceNumber>
            <ie905:Status>X07</ie905:Status>
            <ie905:LastReceivedMessageType>IE881</ie905:LastReceivedMessageType>
          </ie905:Attributes>
        </ie905:StatusResponse>
      </ie905:Body>
    </ie905:IE905>
}
