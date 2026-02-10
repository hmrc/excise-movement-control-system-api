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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.util.Base64
import scala.Right
import scala.concurrent.{ExecutionContext, Future}

class TransformServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach with ScalaFutures {

  implicit private val hc: HeaderCarrier    = HeaderCarrier()
  implicit private val ex: ExecutionContext = ExecutionContext.global

  private val appConfig = mock[AppConfig]
  private val timestamp = Instant.parse("2024-10-01T12:32:32.12345678Z")
  private val sut       = new TransformService(appConfig)

  // All EMCS namespaces use V3.13
  // ImportSad and ImportSadNumber elements are used
  val inputIE801Message =
    """<ie801:IE801
      |  xmlns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/MovementForTraderData/3"
      |  xmlns:ie801="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.13"
      |  xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13">
      |    <ie801:Header>
      |        <tms:MessageSender>NDEA.XI</tms:MessageSender>
      |        <tms:MessageRecipient>NDEA.AT</tms:MessageRecipient>
      |        <tms:DateOfPreparation>2023-06-22</tms:DateOfPreparation>
      |        <tms:TimeOfPreparation>12:37:08.755</tms:TimeOfPreparation>
      |        <tms:MessageIdentifier>XI000003</tms:MessageIdentifier>
      |        <tms:CorrelationIdentifier>PORTAL6de1b822562c43fb9220d236e487c920</tms:CorrelationIdentifier>
      |    </ie801:Header>
      |    <ie801:Body>
      |        <ie801:EADESADContainer>
      |            <ie801:ConsigneeTrader language="en">
      |                <ie801:Traderid>GBWKQOZ80VLYR</ie801:Traderid>
      |                <ie801:TraderName>AFOR KALE LTD</ie801:TraderName>
      |                <ie801:StreetName>The Street</ie801:StreetName>
      |                <ie801:Postcode>AT123</ie801:Postcode>
      |                <ie801:City>The City</ie801:City>
      |            </ie801:ConsigneeTrader>
      |            <ie801:ExciseMovement>
      |                <ie801:AdministrativeReferenceCode>23XI00000000000000021</ie801:AdministrativeReferenceCode>
      |                <ie801:DateAndTimeOfValidationOfEadEsad>2023-06-22T11:37:10.345739396
      |                </ie801:DateAndTimeOfValidationOfEadEsad>
      |            </ie801:ExciseMovement>
      |            <ie801:ConsignorTrader language="en">
      |                <ie801:TraderExciseNumber>GBWKQOZ99VLYR</ie801:TraderExciseNumber>
      |                <ie801:TraderName>Clarkys Eagles</ie801:TraderName>
      |                <ie801:StreetName>Happy Street</ie801:StreetName>
      |                <ie801:Postcode>BT1 1BG</ie801:Postcode>
      |                <ie801:City>The City</ie801:City>
      |            </ie801:ConsignorTrader>
      |            <ie801:PlaceOfDispatchTrader language="en">
      |                <ie801:ReferenceOfTaxWarehouse>XI00000467014</ie801:ReferenceOfTaxWarehouse>
      |            </ie801:PlaceOfDispatchTrader>
      |            <ie801:DeliveryPlaceTrader language="en">
      |                <ie801:Traderid>GB00000602078</ie801:Traderid>
      |                <ie801:TraderName>METEST BOND STTSTGE</ie801:TraderName>
      |                <ie801:StreetName>WHITETEST ROAD METEST CITY ESTATE</ie801:StreetName>
      |                <ie801:Postcode>BN2 4KX</ie801:Postcode>
      |                <ie801:City>STTEST,KENT</ie801:City>
      |            </ie801:DeliveryPlaceTrader>
      |            <ie801:CompetentAuthorityDispatchOffice>
      |                <ie801:ReferenceNumber>GB004098</ie801:ReferenceNumber>
      |            </ie801:CompetentAuthorityDispatchOffice>
      |            <ie801:EadEsad>
      |                <ie801:LocalReferenceNumber>lrnie8154968571</ie801:LocalReferenceNumber>
      |                <ie801:InvoiceNumber>INVOICE001</ie801:InvoiceNumber>
      |                <ie801:InvoiceDate>2018-04-04</ie801:InvoiceDate>
      |                <ie801:OriginTypeCode>1</ie801:OriginTypeCode>
      |                <ie801:DateOfDispatch>2021-12-02</ie801:DateOfDispatch>
      |                <ie801:TimeOfDispatch>22:37:00</ie801:TimeOfDispatch>
      |                <ie801:ImportSad>
      |                    <ie801:ImportSadNumber>12345678</ie801:ImportSadNumber>
      |                </ie801:ImportSad>
      |            </ie801:EadEsad>
      |            <ie801:HeaderEadEsad>
      |                <ie801:SequenceNumber>1</ie801:SequenceNumber>
      |                <ie801:DateAndTimeOfUpdateValidation>2023-06-22T11:37:10.345801029</ie801:DateAndTimeOfUpdateValidation>
      |                <ie801:DestinationTypeCode>1</ie801:DestinationTypeCode>
      |                <ie801:JourneyTime>D01</ie801:JourneyTime>
      |                <ie801:TransportArrangement>1</ie801:TransportArrangement>
      |            </ie801:HeaderEadEsad>
      |            <ie801:TransportMode>
      |                <ie801:TransportModeCode>1</ie801:TransportModeCode>
      |            </ie801:TransportMode>
      |            <ie801:MovementGuarantee>
      |                <ie801:GuarantorTypeCode>1</ie801:GuarantorTypeCode>
      |            </ie801:MovementGuarantee>
      |            <ie801:BodyEadEsad>
      |                <ie801:BodyRecordUniqueReference>1</ie801:BodyRecordUniqueReference>
      |                <ie801:ExciseProductCode>E410</ie801:ExciseProductCode>
      |                <ie801:CnCode>27101231</ie801:CnCode>
      |                <ie801:Quantity>100.000</ie801:Quantity>
      |                <ie801:GrossMass>100.00</ie801:GrossMass>
      |                <ie801:NetMass>90.00</ie801:NetMass>
      |                <ie801:Density>10.00</ie801:Density>
      |                <ie801:Package>
      |                    <ie801:KindOfPackages>BH</ie801:KindOfPackages>
      |                    <ie801:NumberOfPackages>2</ie801:NumberOfPackages>
      |                    <ie801:ShippingMarks>Shipping comment</ie801:ShippingMarks>
      |                </ie801:Package>
      |                <ie801:Package>
      |                    <ie801:KindOfPackages>BH</ie801:KindOfPackages>
      |                    <ie801:NumberOfPackages>2</ie801:NumberOfPackages>
      |                    <ie801:ShippingMarks>Shipping comment 2</ie801:ShippingMarks>
      |                </ie801:Package>
      |            </ie801:BodyEadEsad>
      |            <ie801:TransportDetails>
      |                <ie801:TransportUnitCode>1</ie801:TransportUnitCode>
      |                <ie801:IdentityOfTransportUnits>Transformers robots in disguise</ie801:IdentityOfTransportUnits>
      |            </ie801:TransportDetails>
      |            <ie801:TransportDetails>
      |                <ie801:TransportUnitCode>2</ie801:TransportUnitCode>
      |                <ie801:IdentityOfTransportUnits>MACHINES</ie801:IdentityOfTransportUnits>
      |            </ie801:TransportDetails>
      |            <ie801:TransportDetails>
      |                <ie801:TransportUnitCode>3</ie801:TransportUnitCode>
      |                <ie801:IdentityOfTransportUnits>MORE MACHINES</ie801:IdentityOfTransportUnits>
      |            </ie801:TransportDetails>
      |        </ie801:EADESADContainer>
      |    </ie801:Body>
      |</ie801:IE801>""".stripMargin

  // All EMCS namespaces use V3.23
  // ImportCustomsDeclaration and ImportCustomsDeclarationNumber elements are used
  val outputIE801Message =
    """<ie801:IE801 xmlns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/MovementForTraderData/3" xmlns:ie801="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.23" xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23">
      |    <ie801:Header>
      |        <tms:MessageSender>NDEA.XI</tms:MessageSender>
      |        <tms:MessageRecipient>NDEA.AT</tms:MessageRecipient>
      |        <tms:DateOfPreparation>2023-06-22</tms:DateOfPreparation>
      |        <tms:TimeOfPreparation>12:37:08.755</tms:TimeOfPreparation>
      |        <tms:MessageIdentifier>XI000003</tms:MessageIdentifier>
      |        <tms:CorrelationIdentifier>PORTAL6de1b822562c43fb9220d236e487c920</tms:CorrelationIdentifier>
      |    </ie801:Header>
      |    <ie801:Body>
      |        <ie801:EADESADContainer>
      |            <ie801:ConsigneeTrader language="en">
      |                <ie801:Traderid>GBWKQOZ80VLYR</ie801:Traderid>
      |                <ie801:TraderName>AFOR KALE LTD</ie801:TraderName>
      |                <ie801:StreetName>The Street</ie801:StreetName>
      |                <ie801:Postcode>AT123</ie801:Postcode>
      |                <ie801:City>The City</ie801:City>
      |            </ie801:ConsigneeTrader>
      |            <ie801:ExciseMovement>
      |                <ie801:AdministrativeReferenceCode>23XI00000000000000021</ie801:AdministrativeReferenceCode>
      |                <ie801:DateAndTimeOfValidationOfEadEsad>2023-06-22T11:37:10.345739396
      |                </ie801:DateAndTimeOfValidationOfEadEsad>
      |            </ie801:ExciseMovement>
      |            <ie801:ConsignorTrader language="en">
      |                <ie801:TraderExciseNumber>GBWKQOZ99VLYR</ie801:TraderExciseNumber>
      |                <ie801:TraderName>Clarkys Eagles</ie801:TraderName>
      |                <ie801:StreetName>Happy Street</ie801:StreetName>
      |                <ie801:Postcode>BT1 1BG</ie801:Postcode>
      |                <ie801:City>The City</ie801:City>
      |            </ie801:ConsignorTrader>
      |            <ie801:PlaceOfDispatchTrader language="en">
      |                <ie801:ReferenceOfTaxWarehouse>XI00000467014</ie801:ReferenceOfTaxWarehouse>
      |            </ie801:PlaceOfDispatchTrader>
      |            <ie801:DeliveryPlaceTrader language="en">
      |                <ie801:Traderid>GB00000602078</ie801:Traderid>
      |                <ie801:TraderName>METEST BOND STTSTGE</ie801:TraderName>
      |                <ie801:StreetName>WHITETEST ROAD METEST CITY ESTATE</ie801:StreetName>
      |                <ie801:Postcode>BN2 4KX</ie801:Postcode>
      |                <ie801:City>STTEST,KENT</ie801:City>
      |            </ie801:DeliveryPlaceTrader>
      |            <ie801:CompetentAuthorityDispatchOffice>
      |                <ie801:ReferenceNumber>GB004098</ie801:ReferenceNumber>
      |            </ie801:CompetentAuthorityDispatchOffice>
      |            <ie801:EadEsad>
      |                <ie801:LocalReferenceNumber>lrnie8154968571</ie801:LocalReferenceNumber>
      |                <ie801:InvoiceNumber>INVOICE001</ie801:InvoiceNumber>
      |                <ie801:InvoiceDate>2018-04-04</ie801:InvoiceDate>
      |                <ie801:OriginTypeCode>1</ie801:OriginTypeCode>
      |                <ie801:DateOfDispatch>2021-12-02</ie801:DateOfDispatch>
      |                <ie801:TimeOfDispatch>22:37:00</ie801:TimeOfDispatch>
      |                <ie801:ImportCustomsDeclaration>
      |                    <ie801:ImportCustomsDeclarationNumber>12345678</ie801:ImportCustomsDeclarationNumber>
      |                </ie801:ImportCustomsDeclaration>
      |            </ie801:EadEsad>
      |            <ie801:HeaderEadEsad>
      |                <ie801:SequenceNumber>1</ie801:SequenceNumber>
      |                <ie801:DateAndTimeOfUpdateValidation>2023-06-22T11:37:10.345801029</ie801:DateAndTimeOfUpdateValidation>
      |                <ie801:DestinationTypeCode>1</ie801:DestinationTypeCode>
      |                <ie801:JourneyTime>D01</ie801:JourneyTime>
      |                <ie801:TransportArrangement>1</ie801:TransportArrangement>
      |            </ie801:HeaderEadEsad>
      |            <ie801:TransportMode>
      |                <ie801:TransportModeCode>1</ie801:TransportModeCode>
      |            </ie801:TransportMode>
      |            <ie801:MovementGuarantee>
      |                <ie801:GuarantorTypeCode>1</ie801:GuarantorTypeCode>
      |            </ie801:MovementGuarantee>
      |            <ie801:BodyEadEsad>
      |                <ie801:BodyRecordUniqueReference>1</ie801:BodyRecordUniqueReference>
      |                <ie801:ExciseProductCode>E410</ie801:ExciseProductCode>
      |                <ie801:CnCode>27101231</ie801:CnCode>
      |                <ie801:Quantity>100.000</ie801:Quantity>
      |                <ie801:GrossMass>100.00</ie801:GrossMass>
      |                <ie801:NetMass>90.00</ie801:NetMass>
      |                <ie801:Density>10.00</ie801:Density>
      |                <ie801:Package>
      |                    <ie801:KindOfPackages>BH</ie801:KindOfPackages>
      |                    <ie801:NumberOfPackages>2</ie801:NumberOfPackages>
      |                    <ie801:ShippingMarks>Shipping comment</ie801:ShippingMarks>
      |                </ie801:Package>
      |                <ie801:Package>
      |                    <ie801:KindOfPackages>BH</ie801:KindOfPackages>
      |                    <ie801:NumberOfPackages>2</ie801:NumberOfPackages>
      |                    <ie801:ShippingMarks>Shipping comment 2</ie801:ShippingMarks>
      |                </ie801:Package>
      |            </ie801:BodyEadEsad>
      |            <ie801:TransportDetails>
      |                <ie801:TransportUnitCode>1</ie801:TransportUnitCode>
      |                <ie801:IdentityOfTransportUnits>Transformers robots in disguise</ie801:IdentityOfTransportUnits>
      |            </ie801:TransportDetails>
      |            <ie801:TransportDetails>
      |                <ie801:TransportUnitCode>2</ie801:TransportUnitCode>
      |                <ie801:IdentityOfTransportUnits>MACHINES</ie801:IdentityOfTransportUnits>
      |            </ie801:TransportDetails>
      |            <ie801:TransportDetails>
      |                <ie801:TransportUnitCode>3</ie801:TransportUnitCode>
      |                <ie801:IdentityOfTransportUnits>MORE MACHINES</ie801:IdentityOfTransportUnits>
      |            </ie801:TransportDetails>
      |        </ie801:EADESADContainer>
      |    </ie801:Body>
      |</ie801:IE801>""".stripMargin

  // All EMCS namespaces use V3.13
  // ExportDeclarationAcceptanceOrGoodsReleasedForExport appears within ExciseMovementEad
  val inputIE829Message =
    """<ie829:IE829 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13" xmlns:ie829="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13">
      |    <ie829:Header>
      |        <tms:MessageSender>NDEA.XI</tms:MessageSender>
      |        <tms:MessageRecipient>NDEA.AT</tms:MessageRecipient>
      |        <tms:DateOfPreparation>2023-06-26</tms:DateOfPreparation>
      |        <tms:TimeOfPreparation>09:15:33</tms:TimeOfPreparation>
      |        <tms:MessageIdentifier>XI000003</tms:MessageIdentifier>
      |        <tms:CorrelationIdentifier>6dddas342231ff3a67888bbcedec3435</tms:CorrelationIdentifier>
      |    </ie829:Header>
      |    <ie829:Body>
      |        <ie829:NotificationOfAcceptedExport>
      |            <ie829:Attributes>
      |                <ie829:DateAndTimeOfIssuance>2024-06-26T09:14:54</ie829:DateAndTimeOfIssuance>
      |            </ie829:Attributes>
      |            <ie829:ConsigneeTrader language="en">
      |                <ie829:Traderid>GBWKQOZ80VLYR</ie829:Traderid>
      |                <ie829:TraderName>Whale Oil Lamps Co.</ie829:TraderName>
      |                <ie829:StreetName>The Street</ie829:StreetName>
      |                <ie829:Postcode>MC232</ie829:Postcode>
      |                <ie829:City>Happy Town</ie829:City>
      |                <ie829:EoriNumber>7</ie829:EoriNumber>
      |            </ie829:ConsigneeTrader>
      |            <ie829:ExciseMovementEad>
      |                <ie829:AdministrativeReferenceCode>23XI00000000000056339</ie829:AdministrativeReferenceCode>
      |                <ie829:SequenceNumber>1</ie829:SequenceNumber>
      |                <ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>0</ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>
      |            </ie829:ExciseMovementEad>
      |            <ie829:ExciseMovementEad>
      |                <ie829:AdministrativeReferenceCode>23XI00000000000056340</ie829:AdministrativeReferenceCode>
      |                <ie829:SequenceNumber>1</ie829:SequenceNumber>
      |                <ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>0</ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>
      |            </ie829:ExciseMovementEad>
      |            <ie829:ExportPlaceCustomsOffice>
      |                <ie829:ReferenceNumber>AT633734</ie829:ReferenceNumber>
      |            </ie829:ExportPlaceCustomsOffice>
      |            <ie829:ExportDeclarationAcceptanceRelease>
      |                <ie829:ReferenceNumberOfSenderCustomsOffice>AT324234</ie829:ReferenceNumberOfSenderCustomsOffice>
      |                <ie829:IdentificationOfSenderCustomsOfficer>84884</ie829:IdentificationOfSenderCustomsOfficer>
      |                <ie829:DateOfAcceptance>2023-06-26</ie829:DateOfAcceptance>
      |                <ie829:DocumentReferenceNumber>123123vmnfhsdf3AT</ie829:DocumentReferenceNumber>
      |            </ie829:ExportDeclarationAcceptanceRelease>
      |        </ie829:NotificationOfAcceptedExport>
      |    </ie829:Body>
      |</ie829:IE829>""".stripMargin

  // All EMCS namespaces use V3.23
  // ExportDeclarationAcceptanceOrGoodsReleasedForExport is moved into ExportDeclarationAcceptanceRelease
  val outputIE829Message =
    """<ie829:IE829 xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.23" xmlns:ie829="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.23">
      |    <ie829:Header>
      |        <tms:MessageSender>NDEA.XI</tms:MessageSender>
      |        <tms:MessageRecipient>NDEA.AT</tms:MessageRecipient>
      |        <tms:DateOfPreparation>2023-06-26</tms:DateOfPreparation>
      |        <tms:TimeOfPreparation>09:15:33</tms:TimeOfPreparation>
      |        <tms:MessageIdentifier>XI000003</tms:MessageIdentifier>
      |        <tms:CorrelationIdentifier>6dddas342231ff3a67888bbcedec3435</tms:CorrelationIdentifier>
      |    </ie829:Header>
      |    <ie829:Body>
      |        <ie829:NotificationOfAcceptedExport>
      |            <ie829:Attributes>
      |                <ie829:DateAndTimeOfIssuance>2024-06-26T09:14:54</ie829:DateAndTimeOfIssuance>
      |            </ie829:Attributes>
      |            <ie829:ConsigneeTrader language="en">
      |                <ie829:Traderid>GBWKQOZ80VLYR</ie829:Traderid>
      |                <ie829:TraderName>Whale Oil Lamps Co.</ie829:TraderName>
      |                <ie829:StreetName>The Street</ie829:StreetName>
      |                <ie829:Postcode>MC232</ie829:Postcode>
      |                <ie829:City>Happy Town</ie829:City>
      |                <ie829:EoriNumber>7</ie829:EoriNumber>
      |            </ie829:ConsigneeTrader>
      |            <ie829:ExciseMovementEad>
      |                <ie829:AdministrativeReferenceCode>23XI00000000000056339</ie829:AdministrativeReferenceCode>
      |                <ie829:SequenceNumber>1</ie829:SequenceNumber>
      |                
      |            </ie829:ExciseMovementEad>
      |            <ie829:ExciseMovementEad>
      |                <ie829:AdministrativeReferenceCode>23XI00000000000056340</ie829:AdministrativeReferenceCode>
      |                <ie829:SequenceNumber>1</ie829:SequenceNumber>
      |                
      |            </ie829:ExciseMovementEad>
      |            <ie829:ExportPlaceCustomsOffice>
      |                <ie829:ReferenceNumber>AT633734</ie829:ReferenceNumber>
      |            </ie829:ExportPlaceCustomsOffice>
      |            <ie829:ExportDeclarationAcceptanceRelease>
      |                <ie829:ReferenceNumberOfSenderCustomsOffice>AT324234</ie829:ReferenceNumberOfSenderCustomsOffice>
      |                <ie829:IdentificationOfSenderCustomsOfficer>84884</ie829:IdentificationOfSenderCustomsOfficer>
      |                <ie829:DateOfAcceptance>2023-06-26</ie829:DateOfAcceptance>
      |                <ie829:DocumentReferenceNumber>123123vmnfhsdf3AT</ie829:DocumentReferenceNumber>
      |            <ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport>0</ie829:ExportDeclarationAcceptanceOrGoodsReleasedForExport></ie829:ExportDeclarationAcceptanceRelease>
      |        </ie829:NotificationOfAcceptedExport>
      |    </ie829:Body>
      |</ie829:IE829>""".stripMargin

  "transformation" should {

    "return no errors when transforming IE801" in {
      val messageType = "IE801"

      val encodedMessage = Base64.getEncoder.encodeToString(inputIE801Message.getBytes("UTF-8"))
      val result         = await(sut.transform(messageType, encodedMessage))

      new String(Base64.getDecoder.decode(result.value), "UTF-8") mustBe outputIE801Message
    }

    "return no errors when transforming IE829" in {
      val messageType = "IE829"

      val encodedMessage = Base64.getEncoder.encodeToString(inputIE829Message.getBytes("UTF-8"))
      val result         = await(sut.transform(messageType, encodedMessage))

      new String(Base64.getDecoder.decode(result.value), "UTF-8") mustBe outputIE829Message
    }
  }

}
