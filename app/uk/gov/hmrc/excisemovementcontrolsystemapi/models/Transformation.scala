package uk.gov.hmrc.excisemovementcontrolsystemapi.models
import scala.xml.NodeSeq
import generated.v1.{IE815Type => IE815TypeV1}
import generated.v2.{IE815Type => IE815TypeV2}
import generated.v2.{IE829Type => IE829TypeV2}

import scala.xml._
import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scalaxb._

import java.util.Base64
import scala.xml.PrettyPrinter
object Transformation {
  val ie815Xml =
    """<IE815 xmlns="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13" xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13">
      |  <Header>
      |    <tms:MessageSender>NDEA.XI</tms:MessageSender>
      |    <tms:MessageRecipient>NDEA.GB</tms:MessageRecipient>
      |    <tms:DateOfPreparation>2025-11-18</tms:DateOfPreparation>
      |    <tms:TimeOfPreparation>09:43:39.687</tms:TimeOfPreparation>
      |    <tms:MessageIdentifier>ZJZLJN8IRPA7E26T4</tms:MessageIdentifier>
      |    <tms:CorrelationIdentifier>cor001</tms:CorrelationIdentifier>
      |  </Header>
      |  <Body xmlns="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13">
      |    <SubmittedDraftOfEADESAD>
      |      <Attributes>
      |        <SubmissionMessageType>1</SubmissionMessageType>
      |      </Attributes>
      |      <ConsigneeTrader language="en">
      |        <Traderid>GBWK924908680</Traderid>
      |        <TraderName>AFOR KALE LTD</TraderName>
      |        <StreetName>The Street</StreetName>
      |        <Postcode>AT123</Postcode>
      |        <City>The City</City>
      |      </ConsigneeTrader>
      |      <ConsignorTrader language="en">
      |        <TraderExciseNumber>XIRC649326241</TraderExciseNumber>
      |        <TraderName>Clarkys Eagles</TraderName>
      |        <StreetName>Happy Street</StreetName>
      |        <Postcode>BT1 1BG</Postcode>
      |        <City>The City</City>
      |      </ConsignorTrader>
      |      <DispatchImportOffice>
      |        <ReferenceNumber>XI004098</ReferenceNumber>
      |      </DispatchImportOffice>
      |      <DeliveryPlaceTrader language="en">
      |        <Traderid>GB00000602078</Traderid>
      |        <TraderName>METEST BOND &amp; STTSTGE</TraderName>
      |        <StreetName>WHITETEST ROAD METEST CITY ESTATE</StreetName>
      |        <Postcode>BN2 4KX</Postcode>
      |        <City>STTEST,KENT</City>
      |      </DeliveryPlaceTrader>
      |      <CompetentAuthorityDispatchOffice>
      |        <ReferenceNumber>XI004098</ReferenceNumber>
      |      </CompetentAuthorityDispatchOffice>
      |      <FirstTransporterTrader language="en">
      |        <VatNumber>AO234111165661</VatNumber>
      |        <TraderName>Clarkys Eagles</TraderName>
      |        <StreetName>Happy Street</StreetName>
      |        <Postcode>BT1 1BG</Postcode>
      |        <City>The City</City>
      |      </FirstTransporterTrader>
      |      <HeaderEadEsad>
      |        <DestinationTypeCode>1</DestinationTypeCode>
      |        <JourneyTime>D01</JourneyTime>
      |        <TransportArrangement>1</TransportArrangement>
      |      </HeaderEadEsad>
      |      <TransportMode>
      |        <TransportModeCode>1</TransportModeCode>
      |      </TransportMode>
      |      <MovementGuarantee>
      |        <GuarantorTypeCode>1</GuarantorTypeCode>
      |      </MovementGuarantee>
      |      <BodyEadEsad>
      |        <BodyRecordUniqueReference>1</BodyRecordUniqueReference>
      |        <ExciseProductCode>E410</ExciseProductCode>
      |        <CnCode>27101231</CnCode>
      |        <Quantity>100.000</Quantity>
      |        <GrossMass>100.00</GrossMass>
      |        <NetMass>90.00</NetMass>
      |        <FiscalMarkUsedFlag>1</FiscalMarkUsedFlag>
      |        <DesignationOfOrigin language="en">AO77711144444</DesignationOfOrigin>
      |        <SizeOfProducer>250740</SizeOfProducer>
      |        <Density>10.00</Density>
      |        <CommercialDescription language="en">AOM123456</CommercialDescription>
      |        <BrandNameOfProducts language="en">AOM75</BrandNameOfProducts>
      |        <Package>
      |          <KindOfPackages>BH</KindOfPackages>
      |          <NumberOfPackages>2</NumberOfPackages>
      |          <ShippingMarks>Subhasis Swain1</ShippingMarks>
      |        </Package>
      |        <Package>
      |          <KindOfPackages>BH</KindOfPackages>
      |          <NumberOfPackages>2</NumberOfPackages>
      |          <ShippingMarks>Subhasis Swain 2</ShippingMarks>
      |          <CommercialSealIdentification>GB4472543A</CommercialSealIdentification>
      |          <SealInformation language="en">AOM123456</SealInformation>
      |        </Package>
      |      </BodyEadEsad>
      |      <BodyEadEsad>
      |        <BodyRecordUniqueReference>2</BodyRecordUniqueReference>
      |        <ExciseProductCode>T300</ExciseProductCode>
      |        <CnCode>24021000</CnCode>
      |        <Quantity>6.00</Quantity>
      |        <GrossMass>10.00</GrossMass>
      |        <NetMass>5.00</NetMass>
      |        <FiscalMarkUsedFlag>0</FiscalMarkUsedFlag>
      |        <Package>
      |          <KindOfPackages>PX</KindOfPackages>
      |          <NumberOfPackages>1</NumberOfPackages>
      |          <ShippingMarks>Subhasis Swain 2</ShippingMarks>
      |          <CommercialSealIdentification>GB4472543A</CommercialSealIdentification>
      |          <SealInformation language="en">AOM123456</SealInformation>
      |        </Package>
      |      </BodyEadEsad>
      |      <BodyEadEsad>
      |        <BodyRecordUniqueReference>3</BodyRecordUniqueReference>
      |        <ExciseProductCode>E920</ExciseProductCode>
      |        <CnCode>38260090</CnCode>
      |        <Quantity>450.00</Quantity>
      |        <GrossMass>600.00</GrossMass>
      |        <NetMass>500.00</NetMass>
      |        <FiscalMarkUsedFlag>0</FiscalMarkUsedFlag>
      |        <Density>12.00</Density>
      |        <Package>
      |          <KindOfPackages>BA</KindOfPackages>
      |          <NumberOfPackages>200</NumberOfPackages>
      |          <ShippingMarks>Subhasis Swain 2</ShippingMarks>
      |          <CommercialSealIdentification>GB4472543A</CommercialSealIdentification>
      |          <SealInformation language="en">AOM123456</SealInformation>
      |        </Package>
      |      </BodyEadEsad>
      |      <BodyEadEsad>
      |        <BodyRecordUniqueReference>4</BodyRecordUniqueReference>
      |        <ExciseProductCode>T400</ExciseProductCode>
      |        <CnCode>24031910</CnCode>
      |        <Quantity>150.000</Quantity>
      |        <GrossMass>165.00</GrossMass>
      |        <NetMass>150.00</NetMass>
      |        <FiscalMarkUsedFlag>0</FiscalMarkUsedFlag>
      |        <Package>
      |          <KindOfPackages>CR</KindOfPackages>
      |          <NumberOfPackages>1</NumberOfPackages>
      |          <ShippingMarks>Subhasis Swain 2</ShippingMarks>
      |          <CommercialSealIdentification>GB4472543A</CommercialSealIdentification>
      |          <SealInformation language="en">AOM123456</SealInformation>
      |        </Package>
      |      </BodyEadEsad>
      |      <EadEsadDraft>
      |        <LocalReferenceNumber>lrnie8155419875</LocalReferenceNumber>
      |        <InvoiceNumber>INVOICE001</InvoiceNumber>
      |        <InvoiceDate>2018-04-04</InvoiceDate>
      |        <OriginTypeCode>2</OriginTypeCode>
      |        <DateOfDispatch>2026-01-26-</DateOfDispatch>
      |        <TimeOfDispatch>22:37:00</TimeOfDispatch>
      |        <ImportSad>
      |          <ImportSadNumber>lrnie8155419875</ImportSadNumber>
      |        </ImportSad>
      |      </EadEsadDraft>
      |      <TransportDetails>
      |        <TransportUnitCode>1</TransportUnitCode>
      |        <IdentityOfTransportUnits>Transformers robots in disguise</IdentityOfTransportUnits>
      |      </TransportDetails>
      |      <TransportDetails>
      |        <TransportUnitCode>2</TransportUnitCode>
      |        <IdentityOfTransportUnits>MACHINES</IdentityOfTransportUnits>
      |      </TransportDetails>
      |      <TransportDetails>
      |        <TransportUnitCode>3</TransportUnitCode>
      |        <IdentityOfTransportUnits>MORE MACHINES</IdentityOfTransportUnits>
      |        <CommercialSealIdentification>GB4472543A</CommercialSealIdentification>
      |        <ComplementaryInformation language="en">BH21</ComplementaryInformation>
      |        <SealInformation language="en">AOM123456</SealInformation>
      |      </TransportDetails>
      |    </SubmittedDraftOfEADESAD>
      |  </Body>
      |</IE815>""".stripMargin

  val ie815encoded =  new String(Base64.getEncoder.encode(ie815Xml.getBytes("UTF-8")), "UTF-8")
  val ie815decoded = new String(Base64.getDecoder.decode(ie815encoded), "UTF-8")
  val ie829xml =
    """<ie829:IE829 xmlns:ie829="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13">
      |    <body:Header xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13" xmlns:body="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13">
      |        <tms:MessageSender>NDEA.AT</tms:MessageSender>
      |        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
      |        <tms:DateOfPreparation>2025-11-24</tms:DateOfPreparation>
      |        <tms:TimeOfPreparation>12:18:33.460</tms:TimeOfPreparation>
      |        <tms:MessageIdentifier>XI42304</tms:MessageIdentifier>
      |        <tms:CorrelationIdentifier>XXZIPL60FNQXFOVK</tms:CorrelationIdentifier>
      |    </body:Header>
      |    <ie829:Body>
      |        <ie829:NotificationOfAcceptedExport>
      |            <body:Attributes xmlns:body="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13">
      |                <body:DateAndTimeOfIssuance>2023-05-20T13:39:12</body:DateAndTimeOfIssuance>
      |            </body:Attributes>
      |            <body:ConsigneeTrader language="es" xmlns:body="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13">
      |                <body:Traderid>GB123456789</body:Traderid>
      |                <body:TraderName>METEST BOND STTSTGE</body:TraderName>
      |                <body:StreetName>WHITETEST ROAD METEST CITY ESTATE</body:StreetName>
      |                <body:Postcode>BN2 4KX</body:Postcode>
      |                <body:City>STTEST, KENT</body:City>
      |            </body:ConsigneeTrader>
      |            <ie829:ExciseMovementEad>
      |                <ie829:AdministrativeReferenceCode>25XI00000000000051380</ie829:AdministrativeReferenceCode>
      |                <ie829:SequenceNumber>1</ie829:SequenceNumber>
      |                <ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>1</ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>
      |            </ie829:ExciseMovementEad>
      |            <body:ExportDeclarationAcceptanceRelease xmlns:body="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13">
      |                <body:ReferenceNumberOfSenderCustomsOffice>GB000383</body:ReferenceNumberOfSenderCustomsOffice>
      |                <body:DateOfAcceptance>2023-05-20</body:DateOfAcceptance>
      |                <body:DocumentReferenceNumber>LRN20230518133824</body:DocumentReferenceNumber>
      |            </body:ExportDeclarationAcceptanceRelease>
      |        </ie829:NotificationOfAcceptedExport>
      |    </ie829:Body>
      |</ie829:IE829>""".stripMargin


  val ie818xml =
    """<ie818:IE818 xmlns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/MovementForTraderData/3" xmlns:doc="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:DOC:V3.13" xmlns:emcs="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:EMCS:V3.13" xmlns:euc="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/EmcsUkCodes/3" xmlns:ie0="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE880:V3.13" xmlns:ie1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE825:V3.13" xmlns:ie2="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE717:V3.13" xmlns:ie3="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13" xmlns:ie="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE934:V3.13" xmlns:ie704uk="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3" xmlns:ie801="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.13" xmlns:ie802="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.13" xmlns:ie803="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE803:V3.13" xmlns:ie807="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE807:V3.13" xmlns:ie810="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.13" xmlns:ie813="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE813:V3.13" xmlns:ie818="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.13" xmlns:ie819="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE819:V3.13" xmlns:ie829="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13" xmlns:ie837="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.13" xmlns:ie839="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.13" xmlns:ie840="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE840:V3.13" xmlns:ie871="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE871:V3.13" xmlns:ie881="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE881:V3.13" xmlns:ie905="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE905:V3.13" xmlns:tcl="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TCL:V3.13" xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13" xmlns:tns4="http://www.govtalk.gov.uk/taxation/InternationalTrade/Common/ControlDocument" xmlns:tns5="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/MovementForTraderData/3" xmlns:tns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/NewMessagesData/3" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |    <ie818:Header>
      |        <tms:MessageSender>NDEA.XI</tms:MessageSender>
      |        <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
      |        <tms:DateOfPreparation>2006-08-04</tms:DateOfPreparation>
      |        <tms:TimeOfPreparation>09:43:40</tms:TimeOfPreparation>
      |        <tms:MessageIdentifier>XI0000016</tms:MessageIdentifier>
      |        <tms:CorrelationIdentifier>cor001</tms:CorrelationIdentifier>
      |    </ie818:Header>
      |    <ie818:Body>
      |        <ie818:AcceptedOrRejectedReportOfReceiptExport>
      |            <ie818:Attributes>
      |                <ie818:DateAndTimeOfValidationOfReportOfReceiptExport>2001-01-03T11:25:01</ie818:DateAndTimeOfValidationOfReportOfReceiptExport>
      |            </ie818:Attributes>
      |            <ie818:ConsigneeTrader language="to">
      |                <ie818:Traderid>GBWK924908680</ie818:Traderid>
      |                <ie818:TraderName>token</ie818:TraderName>
      |                <ie818:StreetName>token</ie818:StreetName>
      |                <ie818:StreetNumber>token</ie818:StreetNumber>
      |                <ie818:Postcode>token</ie818:Postcode>
      |                <ie818:City>token</ie818:City>
      |                <ie818:EoriNumber>token</ie818:EoriNumber>
      |            </ie818:ConsigneeTrader>
      |            <ie818:ExciseMovement>
      |                <ie818:AdministrativeReferenceCode>23XI00000000000000016</ie818:AdministrativeReferenceCode>
      |                <ie818:SequenceNumber>1</ie818:SequenceNumber>
      |            </ie818:ExciseMovement>
      |            <ie818:DeliveryPlaceTrader language="to">
      |                <ie818:Traderid>token</ie818:Traderid>
      |                <ie818:TraderName>token</ie818:TraderName>
      |                <ie818:StreetName>token</ie818:StreetName>
      |                <ie818:StreetNumber>token</ie818:StreetNumber>
      |                <ie818:Postcode>token</ie818:Postcode>
      |                <ie818:City>token</ie818:City>
      |            </ie818:DeliveryPlaceTrader>
      |            <ie818:DestinationOffice>
      |                <ie818:ReferenceNumber>GB005045</ie818:ReferenceNumber>
      |            </ie818:DestinationOffice>
      |            <ie818:ReportOfReceiptExport>
      |                <ie818:DateOfArrivalOfExciseProducts>2014-01-10</ie818:DateOfArrivalOfExciseProducts>
      |                <ie818:GlobalConclusionOfReceipt>22</ie818:GlobalConclusionOfReceipt>
      |                <ie818:ComplementaryInformation language="to">token</ie818:ComplementaryInformation>
      |            </ie818:ReportOfReceiptExport>
      |            <ie818:BodyReportOfReceiptExport>
      |                <ie818:BodyRecordUniqueReference>123</ie818:BodyRecordUniqueReference>
      |                <ie818:IndicatorOfShortageOrExcess>S</ie818:IndicatorOfShortageOrExcess>
      |                <ie818:ObservedShortageOrExcess>1000.0</ie818:ObservedShortageOrExcess>
      |                <ie818:ExciseProductCode>toke</ie818:ExciseProductCode>
      |                <ie818:RefusedQuantity>1000.0</ie818:RefusedQuantity>
      |                <ie818:UnsatisfactoryReason>
      |                    <ie818:UnsatisfactoryReasonCode>12</ie818:UnsatisfactoryReasonCode>
      |                    <ie818:ComplementaryInformation language="to">token</ie818:ComplementaryInformation>
      |                </ie818:UnsatisfactoryReason>
      |            </ie818:BodyReportOfReceiptExport>
      |        </ie818:AcceptedOrRejectedReportOfReceiptExport>
      |    </ie818:Body>
      |</ie818:IE818>""".stripMargin

  val ie801Encoded = "PGllODAxOklFODAxIHhtbG5zPSJodHRwOi8vd3d3LmdvdnRhbGsuZ292LnVrL3RheGF0aW9uL0ludGVybmF0aW9uYWxUcmFkZS9FeGNpc2UvTW92ZW1lbnRGb3JUcmFkZXJEYXRhLzMiIHhtbG5zOmRvYz0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpET0M6VjMuMTMiIHhtbG5zOmVtY3M9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6RU1DUzpWMy4xMyIgeG1sbnM6ZXVjPSJodHRwOi8vd3d3LmdvdnRhbGsuZ292LnVrL3RheGF0aW9uL0ludGVybmF0aW9uYWxUcmFkZS9FeGNpc2UvRW1jc1VrQ29kZXMvMyIgeG1sbnM6aWUwPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODgwOlYzLjEzIiB4bWxuczppZTE9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MjU6VjMuMTMiIHhtbG5zOmllMj0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTcxNzpWMy4xMyIgeG1sbnM6aWUzPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODE1OlYzLjEzIiB4bWxuczppZT0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTkzNDpWMy4xMyIgeG1sbnM6aWU3MDR1az0iaHR0cDovL3d3dy5nb3Z0YWxrLmdvdi51ay90YXhhdGlvbi9JbnRlcm5hdGlvbmFsVHJhZGUvRXhjaXNlL2llNzA0dWsvMyIgeG1sbnM6aWU4MDE9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MDE6VjMuMTMiIHhtbG5zOmllODAyPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODAyOlYzLjEzIiB4bWxuczppZTgwMz0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTgwMzpWMy4xMyIgeG1sbnM6aWU4MDc9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MDc6VjMuMTMiIHhtbG5zOmllODEwPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODEwOlYzLjEzIiB4bWxuczppZTgxMz0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTgxMzpWMy4xMyIgeG1sbnM6aWU4MTg9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MTg6VjMuMTMiIHhtbG5zOmllODE5PSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODE5OlYzLjEzIiB4bWxuczppZTgyOT0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTgyOTpWMy4xMyIgeG1sbnM6aWU4Mzc9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4Mzc6VjMuMTMiIHhtbG5zOmllODM5PSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODM5OlYzLjEzIiB4bWxuczppZTg0MD0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTg0MDpWMy4xMyIgeG1sbnM6aWU4NzE9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4NzE6VjMuMTMiIHhtbG5zOmllODgxPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODgxOlYzLjEzIiB4bWxuczppZTkwNT0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTkwNTpWMy4xMyIgeG1sbnM6dGNsPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OlRDTDpWMy4xMyIgeG1sbnM6dG1zPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OlRNUzpWMy4xMyIgeG1sbnM6dG5zND0iaHR0cDovL3d3dy5nb3Z0YWxrLmdvdi51ay90YXhhdGlvbi9JbnRlcm5hdGlvbmFsVHJhZGUvQ29tbW9uL0NvbnRyb2xEb2N1bWVudCIgeG1sbnM6dG5zNT0iaHR0cDovL3d3dy5nb3Z0YWxrLmdvdi51ay90YXhhdGlvbi9JbnRlcm5hdGlvbmFsVHJhZGUvRXhjaXNlL01vdmVtZW50Rm9yVHJhZGVyRGF0YS8zIiB4bWxuczp0bnM9Imh0dHA6Ly93d3cuZ292dGFsay5nb3YudWsvdGF4YXRpb24vSW50ZXJuYXRpb25hbFRyYWRlL0V4Y2lzZS9OZXdNZXNzYWdlc0RhdGEvMyIgeG1sbnM6eHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hIiB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIj48aWU4MDE6SGVhZGVyPjx0bXM6TWVzc2FnZVNlbmRlcj5OREVBLlhJPC90bXM6TWVzc2FnZVNlbmRlcj48dG1zOk1lc3NhZ2VSZWNpcGllbnQ+TkRFQS5BVDwvdG1zOk1lc3NhZ2VSZWNpcGllbnQ+PHRtczpEYXRlT2ZQcmVwYXJhdGlvbj4yMDIzLTA2LTIyPC90bXM6RGF0ZU9mUHJlcGFyYXRpb24+PHRtczpUaW1lT2ZQcmVwYXJhdGlvbj4xMjozNzowOC43NTU8L3RtczpUaW1lT2ZQcmVwYXJhdGlvbj48dG1zOk1lc3NhZ2VJZGVudGlmaWVyPlhJMDAwMDAyPC90bXM6TWVzc2FnZUlkZW50aWZpZXI+PHRtczpDb3JyZWxhdGlvbklkZW50aWZpZXI+UE9SVEFMNmRlMWI4MjI1NjJjNDNmYjkyMjBkMjM2ZTQ4N2M5MjA8L3RtczpDb3JyZWxhdGlvbklkZW50aWZpZXI+PC9pZTgwMTpIZWFkZXI+PGllODAxOkJvZHk+PGllODAxOkVBREVTQURDb250YWluZXI+PGllODAxOkNvbnNpZ25lZVRyYWRlciBsYW5ndWFnZT0iZW4iPjxpZTgwMTpUcmFkZXJpZD5HQldLUU9aODBWTFlSPC9pZTgwMTpUcmFkZXJpZD48aWU4MDE6VHJhZGVyTmFtZT5BRk9SIEtBTEUgTFREPC9pZTgwMTpUcmFkZXJOYW1lPjxpZTgwMTpTdHJlZXROYW1lPlRoZSBTdHJlZXQ8L2llODAxOlN0cmVldE5hbWU+PGllODAxOlBvc3Rjb2RlPkFUMTIzPC9pZTgwMTpQb3N0Y29kZT48aWU4MDE6Q2l0eT5UaGUgQ2l0eTwvaWU4MDE6Q2l0eT48L2llODAxOkNvbnNpZ25lZVRyYWRlcj48aWU4MDE6RXhjaXNlTW92ZW1lbnQ+PGllODAxOkFkbWluaXN0cmF0aXZlUmVmZXJlbmNlQ29kZT4yM1hJMDAwMDAwMDAwMDAwMDAwMTM8L2llODAxOkFkbWluaXN0cmF0aXZlUmVmZXJlbmNlQ29kZT48aWU4MDE6RGF0ZUFuZFRpbWVPZlZhbGlkYXRpb25PZkVhZEVzYWQ+MjAyMy0wNi0yMlQxMTozNzoxMC4zNDU3MzkzOTY8L2llODAxOkRhdGVBbmRUaW1lT2ZWYWxpZGF0aW9uT2ZFYWRFc2FkPjwvaWU4MDE6RXhjaXNlTW92ZW1lbnQ+PGllODAxOkNvbnNpZ25vclRyYWRlciBsYW5ndWFnZT0iZW4iPjxpZTgwMTpUcmFkZXJFeGNpc2VOdW1iZXI+R0JXS1FPWjk5VkxZUjwvaWU4MDE6VHJhZGVyRXhjaXNlTnVtYmVyPjxpZTgwMTpUcmFkZXJOYW1lPkNsYXJreXMgRWFnbGVzPC9pZTgwMTpUcmFkZXJOYW1lPjxpZTgwMTpTdHJlZXROYW1lPkhhcHB5IFN0cmVldDwvaWU4MDE6U3RyZWV0TmFtZT48aWU4MDE6UG9zdGNvZGU+QlQxIDFCRzwvaWU4MDE6UG9zdGNvZGU+PGllODAxOkNpdHk+VGhlIENpdHk8L2llODAxOkNpdHk+PC9pZTgwMTpDb25zaWdub3JUcmFkZXI+PGllODAxOlBsYWNlT2ZEaXNwYXRjaFRyYWRlciBsYW5ndWFnZT0iZW4iPjxpZTgwMTpSZWZlcmVuY2VPZlRheFdhcmVob3VzZT5YSTAwMDAwNDY3MDE0PC9pZTgwMTpSZWZlcmVuY2VPZlRheFdhcmVob3VzZT48L2llODAxOlBsYWNlT2ZEaXNwYXRjaFRyYWRlcj48aWU4MDE6RGVsaXZlcnlQbGFjZVRyYWRlciBsYW5ndWFnZT0iZW4iPjxpZTgwMTpUcmFkZXJpZD5HQjAwMDAwNjAyMDc4PC9pZTgwMTpUcmFkZXJpZD48aWU4MDE6VHJhZGVyTmFtZT5NRVRFU1QgQk9ORCBTVFRTVEdFPC9pZTgwMTpUcmFkZXJOYW1lPjxpZTgwMTpTdHJlZXROYW1lPldISVRFVEVTVCBST0FEIE1FVEVTVCBDSVRZIEVTVEFURTwvaWU4MDE6U3RyZWV0TmFtZT48aWU4MDE6UG9zdGNvZGU+Qk4yIDRLWDwvaWU4MDE6UG9zdGNvZGU+PGllODAxOkNpdHk+U1RURVNULEtFTlQ8L2llODAxOkNpdHk+PC9pZTgwMTpEZWxpdmVyeVBsYWNlVHJhZGVyPjxpZTgwMTpDb21wZXRlbnRBdXRob3JpdHlEaXNwYXRjaE9mZmljZT48aWU4MDE6UmVmZXJlbmNlTnVtYmVyPkdCMDA0MDk4PC9pZTgwMTpSZWZlcmVuY2VOdW1iZXI+PC9pZTgwMTpDb21wZXRlbnRBdXRob3JpdHlEaXNwYXRjaE9mZmljZT48aWU4MDE6RWFkRXNhZD48aWU4MDE6TG9jYWxSZWZlcmVuY2VOdW1iZXI+bHJuaWU4MTU2NTQwODU2PC9pZTgwMTpMb2NhbFJlZmVyZW5jZU51bWJlcj48aWU4MDE6SW52b2ljZU51bWJlcj5JTlZPSUNFMDAxPC9pZTgwMTpJbnZvaWNlTnVtYmVyPjxpZTgwMTpJbnZvaWNlRGF0ZT4yMDE4LTA0LTA0PC9pZTgwMTpJbnZvaWNlRGF0ZT48aWU4MDE6T3JpZ2luVHlwZUNvZGU+MTwvaWU4MDE6T3JpZ2luVHlwZUNvZGU+PGllODAxOkRhdGVPZkRpc3BhdGNoPjIwMjEtMTItMDI8L2llODAxOkRhdGVPZkRpc3BhdGNoPjxpZTgwMTpUaW1lT2ZEaXNwYXRjaD4yMjozNzowMDwvaWU4MDE6VGltZU9mRGlzcGF0Y2g+PC9pZTgwMTpFYWRFc2FkPjxpZTgwMTpIZWFkZXJFYWRFc2FkPjxpZTgwMTpTZXF1ZW5jZU51bWJlcj4xPC9pZTgwMTpTZXF1ZW5jZU51bWJlcj48aWU4MDE6RGF0ZUFuZFRpbWVPZlVwZGF0ZVZhbGlkYXRpb24+MjAyMy0wNi0yMlQxMTozNzoxMC4zNDU4MDEwMjk8L2llODAxOkRhdGVBbmRUaW1lT2ZVcGRhdGVWYWxpZGF0aW9uPjxpZTgwMTpEZXN0aW5hdGlvblR5cGVDb2RlPjE8L2llODAxOkRlc3RpbmF0aW9uVHlwZUNvZGU+PGllODAxOkpvdXJuZXlUaW1lPkQwMTwvaWU4MDE6Sm91cm5leVRpbWU+PGllODAxOlRyYW5zcG9ydEFycmFuZ2VtZW50PjE8L2llODAxOlRyYW5zcG9ydEFycmFuZ2VtZW50PjwvaWU4MDE6SGVhZGVyRWFkRXNhZD48aWU4MDE6VHJhbnNwb3J0TW9kZT48aWU4MDE6VHJhbnNwb3J0TW9kZUNvZGU+MTwvaWU4MDE6VHJhbnNwb3J0TW9kZUNvZGU+PC9pZTgwMTpUcmFuc3BvcnRNb2RlPjxpZTgwMTpNb3ZlbWVudEd1YXJhbnRlZT48aWU4MDE6R3VhcmFudG9yVHlwZUNvZGU+MTwvaWU4MDE6R3VhcmFudG9yVHlwZUNvZGU+PC9pZTgwMTpNb3ZlbWVudEd1YXJhbnRlZT48aWU4MDE6Qm9keUVhZEVzYWQ+PGllODAxOkJvZHlSZWNvcmRVbmlxdWVSZWZlcmVuY2U+MTwvaWU4MDE6Qm9keVJlY29yZFVuaXF1ZVJlZmVyZW5jZT48aWU4MDE6RXhjaXNlUHJvZHVjdENvZGU+RTQxMDwvaWU4MDE6RXhjaXNlUHJvZHVjdENvZGU+PGllODAxOkNuQ29kZT4yNzEwMTIzMTwvaWU4MDE6Q25Db2RlPjxpZTgwMTpRdWFudGl0eT4xMDAuMDAwPC9pZTgwMTpRdWFudGl0eT48aWU4MDE6R3Jvc3NNYXNzPjEwMC4wMDwvaWU4MDE6R3Jvc3NNYXNzPjxpZTgwMTpOZXRNYXNzPjkwLjAwPC9pZTgwMTpOZXRNYXNzPjxpZTgwMTpEZW5zaXR5PjEwLjAwPC9pZTgwMTpEZW5zaXR5PjxpZTgwMTpQYWNrYWdlPjxpZTgwMTpLaW5kT2ZQYWNrYWdlcz5CSDwvaWU4MDE6S2luZE9mUGFja2FnZXM+PGllODAxOk51bWJlck9mUGFja2FnZXM+MjwvaWU4MDE6TnVtYmVyT2ZQYWNrYWdlcz48aWU4MDE6U2hpcHBpbmdNYXJrcz5TaGlwcGluZyBjb21tZW50PC9pZTgwMTpTaGlwcGluZ01hcmtzPjwvaWU4MDE6UGFja2FnZT48aWU4MDE6UGFja2FnZT48aWU4MDE6S2luZE9mUGFja2FnZXM+Qkg8L2llODAxOktpbmRPZlBhY2thZ2VzPjxpZTgwMTpOdW1iZXJPZlBhY2thZ2VzPjI8L2llODAxOk51bWJlck9mUGFja2FnZXM+PGllODAxOlNoaXBwaW5nTWFya3M+U2hpcHBpbmcgY29tbWVudCAyPC9pZTgwMTpTaGlwcGluZ01hcmtzPjwvaWU4MDE6UGFja2FnZT48L2llODAxOkJvZHlFYWRFc2FkPjxpZTgwMTpUcmFuc3BvcnREZXRhaWxzPjxpZTgwMTpUcmFuc3BvcnRVbml0Q29kZT4xPC9pZTgwMTpUcmFuc3BvcnRVbml0Q29kZT48aWU4MDE6SWRlbnRpdHlPZlRyYW5zcG9ydFVuaXRzPlRyYW5zZm9ybWVycyByb2JvdHMgaW4gZGlzZ3Vpc2U8L2llODAxOklkZW50aXR5T2ZUcmFuc3BvcnRVbml0cz48L2llODAxOlRyYW5zcG9ydERldGFpbHM+PGllODAxOlRyYW5zcG9ydERldGFpbHM+PGllODAxOlRyYW5zcG9ydFVuaXRDb2RlPjI8L2llODAxOlRyYW5zcG9ydFVuaXRDb2RlPjxpZTgwMTpJZGVudGl0eU9mVHJhbnNwb3J0VW5pdHM+TUFDSElORVM8L2llODAxOklkZW50aXR5T2ZUcmFuc3BvcnRVbml0cz48L2llODAxOlRyYW5zcG9ydERldGFpbHM+PGllODAxOlRyYW5zcG9ydERldGFpbHM+PGllODAxOlRyYW5zcG9ydFVuaXRDb2RlPjM8L2llODAxOlRyYW5zcG9ydFVuaXRDb2RlPjxpZTgwMTpJZGVudGl0eU9mVHJhbnNwb3J0VW5pdHM+TU9SRSBNQUNISU5FUzwvaWU4MDE6SWRlbnRpdHlPZlRyYW5zcG9ydFVuaXRzPjwvaWU4MDE6VHJhbnNwb3J0RGV0YWlscz48L2llODAxOkVBREVTQURDb250YWluZXI+PC9pZTgwMTpCb2R5PjwvaWU4MDE6SUU4MDE+"

  def createXMLFromString(xmlStr: String) = {


    val xmlFromStringV1 = xml.XML.loadString(xmlStr)


    val updatedHeaderNameSpaceXML: Elem = XmlNamespaceTransformer.updateXMLNamespace(xmlFromStringV1)
    //val res = SchemaTransformer.exportDeclarationTransformation(updatedHeaderNameSpaceXML)

    val pp = new PrettyPrinter(300, 2)

    println(pp.format(updatedHeaderNameSpaceXML))


  }

  object SchemaTransformer {

    def convertImportSadToCustomDeclaration(n: scala.xml.Node): scala.xml.Node = {
      n match {

        case e: scala.xml.Elem =>
          val newLabel = e.label match {
            case "ImportSad" => "ImportCustomsDeclaration"
            case "ImportSadNumber" => "ImportCustomsDeclarationNumber"
            case label => label
          }

          e.copy(label = newLabel, child = e.child.map(convertImportSadToCustomDeclaration))

        case other => other
      }
    }

    def exportDeclarationTransformation(xml: scala.xml.Node): Either[ExportDeclaractionTransformError, NodeSeq] = {
      // add error handling
      (xml \\ "ExportDeclarationAcceptanceOrGoodsReleasedForExport").headOption.map {
        exportDeclaration =>


          def removeExportDeclaration(n: scala.xml.Node): NodeSeq = {
            //get export save it append it to
            val res = n match {
              case t: scala.xml.Text if t.text.trim.isEmpty => NodeSeq.Empty
              case e: scala.xml.Elem if e.label == "ExportDeclarationAcceptanceOrGoodsReleasedForExport" => NodeSeq.Empty
              case e: scala.xml.Elem => e.copy(child = e.child.flatMap(removeExportDeclaration))
              case other => other

            }

            res
          }


          def appendToExportDeclarationAcceptanceRelease(xml: scala.xml.Node): NodeSeq = {
            xml match {
              case e: scala.xml.Elem if e.label == "ExportDeclarationAcceptanceRelease" => e.copy(child = e.child ++ exportDeclaration)
              case e: Elem =>
                e.copy(child = e.child.flatMap(appendToExportDeclarationAcceptanceRelease))
              case other => other

            }

          }

          Right(appendToExportDeclarationAcceptanceRelease(removeExportDeclaration(xml).head))
      }.getOrElse(Left(ExportDeclaractionTransformError("Could not locate ExportDeclarationAcceptanceOrGoodsReleasedForExport in xml")))
    }
  }

  object XmlNamespaceTransformer {


    private def rewriteScope(scope: NamespaceBinding): NamespaceBinding = {
      val updateNamespace = (str: String) => if (str != null && str.nonEmpty && str.startsWith("urn:") && str.endsWith("V3.13")) str.dropRight(5) + "V3.23" else str
      if (scope == null) null
      else {
        val rewrittenParent = rewriteScope(scope.parent)

        val newUri = updateNamespace(scope.uri)
        NamespaceBinding(
          scope.prefix,
          newUri,
          rewrittenParent
        )
      }
    }

    def updateXMLNamespace(root: Elem): Elem = {
      val newScope = rewriteScope(root.scope)
      updateDataElementNamespace(root, newScope).asInstanceOf[Elem]
    }

    private def updateDataElementNamespace(node: Node, parentScope: NamespaceBinding): Node =
      node match {
        case e: Elem =>
          val newScope = if (e.scope != null && e.scope != parentScope) {


            println(
              s"${e.label}\n" +
                s"element scope : ${e.scope}\n" +
                s"parent scope  : $parentScope\n"
            )
            rewriteScope(e.scope)

          } else {
            e.scope
          }
          e.copy(
            scope = newScope,
            child = e.child.map(c => updateDataElementNamespace(c, newScope))
          )


        case other => other
      }


    def rewriteNamespace(bade64EncodedMessage: String): Either[TransformationError, String] = {
      try {
        val updateNamespace = (str: String) => if (str != null && str.nonEmpty && str.startsWith("urn:") && str.endsWith("V3.13")) str.dropRight(5) + "V3.23" else str
        val namespaceRegex =
          """xmlns(?:\s*:\s*([A-Za-z0-9_.-]+))?\s*=\s*"([^"]+)"""".r

        val tagRegex = """<\s*[^/!?][^>]*>""".r

        val xmlStr = new String(Base64.getDecoder.decode(bade64EncodedMessage), "UTF-8")
        Right(tagRegex.replaceAllIn(xmlStr, { elements =>

          val updatedTag = namespaceRegex.replaceAllIn(elements.matched, {
            namespace =>
              // val ns = Option(namespace.group(2))
              namespace.matched.replace(namespace.group(2), updateNamespace(namespace.group(2)))
            // updateNamespace(namespace.group(2))
            // namespace.matched

          })

          updatedTag

        }))

      } catch {
        case e: Exception => Left(RewriteNamespaceError(e.toString))
      }

    }
  }
}
