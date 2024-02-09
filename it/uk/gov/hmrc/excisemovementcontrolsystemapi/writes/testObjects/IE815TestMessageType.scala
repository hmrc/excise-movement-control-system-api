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

object IE815TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn:IE815 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.01"
                                          xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                                          xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
                                          xmlns="http://www.hmrc.gov.uk/ChRIS/Service/Control"
                                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <urn:Header>
        <urn1:MessageSender>NDEA.GB</urn1:MessageSender>
        <urn1:MessageRecipient>NDEA.GB</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2023-09-09</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>03:22:47</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>ok</urn1:MessageIdentifier>
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
            <urn:TraderExciseNumber>GBWK002281024</urn:TraderExciseNumber>
            <urn:TraderName>Company PLC</urn:TraderName>
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
            <urn:LocalReferenceNumber>lrnie8158976912</urn:LocalReferenceNumber>
            <urn:InvoiceNumber>Test123</urn:InvoiceNumber>
            <urn:InvoiceDate>2023-09-09</urn:InvoiceDate>
            <urn:OriginTypeCode>1</urn:OriginTypeCode>
            <urn:DateOfDispatch>2024-02-05</urn:DateOfDispatch>
            <urn:TimeOfDispatch>12:00:00</urn:TimeOfDispatch>
          </urn:EadEsadDraft>
          <urn:TransportDetails>
            <urn:TransportUnitCode>1</urn:TransportUnitCode>
            <urn:IdentityOfTransportUnits>100</urn:IdentityOfTransportUnits>
          </urn:TransportDetails>
        </urn:SubmittedDraftOfEADESAD>
      </urn:Body>
    </urn:IE815>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-09-09\",\"TimeOfPreparation\":\"03:22:47\",\"MessageIdentifier\":\"ok\",\"CorrelationIdentifier\":\"PORTAL6de1b822562c43fb9220d236e487c920\"},\"Body\":{\"SubmittedDraftOfEADESAD\":{\"AttributesValue\":{\"SubmissionMessageType\":\"1\"},\"ConsigneeTrader\":{\"Traderid\":\"GBWKQOZ8OVLYR\",\"TraderName\":\"WFlgUjfC\",\"StreetName\":\"xoL0NsNyDi\",\"StreetNumber\":\"67\",\"Postcode\":\"A1 1AA\",\"City\":\"l8WSaHS9\",\"attributes\":{\"@language\":\"en\"}},\"ConsignorTrader\":{\"TraderExciseNumber\":\"GBWK002281024\",\"TraderName\":\"Company PLC\",\"StreetName\":\"msfvZUL1Oe\",\"StreetNumber\":\"25\",\"Postcode\":\"A1 1AA\",\"City\":\"QDHwPa61\",\"attributes\":{\"@language\":\"en\"}},\"PlaceOfDispatchTrader\":{\"ReferenceOfTaxWarehouse\":\"GB00DO459DMNX\",\"TraderName\":\"2z0waekA\",\"StreetName\":\"MhO1XtDIVr\",\"StreetNumber\":\"25\",\"Postcode\":\"A1 1AA\",\"City\":\"zPCc6skm\",\"attributes\":{\"@language\":\"en\"}},\"DeliveryPlaceTrader\":{\"Traderid\":\"GB00AIP67RAO3\",\"TraderName\":\"BJpWdv2N\",\"StreetName\":\"C24vvUqCw6\",\"StreetNumber\":\"43\",\"Postcode\":\"A1 1AA\",\"City\":\"A9ZlElxP\",\"attributes\":{\"@language\":\"en\"}},\"CompetentAuthorityDispatchOffice\":{\"ReferenceNumber\":\"GB004098\"},\"FirstTransporterTrader\":{\"VatNumber\":\"123798354\",\"TraderName\":\"Mr Delivery place trader 4\",\"StreetName\":\"Delplace Avenue\",\"StreetNumber\":\"05\",\"Postcode\":\"FR5 4RN\",\"City\":\"Delville\",\"attributes\":{\"@language\":\"en\"}},\"DocumentCertificate\":[{\"DocumentType\":\"9\",\"DocumentReference\":\"DPdQsYktZEJEESpc7b32Ig0U6B34XmHmfZU\"}],\"HeaderEadEsad\":{\"DestinationTypeCode\":\"1\",\"JourneyTime\":\"D07\",\"TransportArrangement\":\"1\"},\"TransportMode\":{\"TransportModeCode\":\"3\"},\"MovementGuarantee\":{\"GuarantorTypeCode\":\"1\",\"GuarantorTrader\":[]},\"BodyEadEsad\":[{\"BodyRecordUniqueReference\":\"1\",\"ExciseProductCode\":\"B000\",\"CnCode\":\"22030001\",\"Quantity\":2000,\"GrossMass\":20000,\"NetMass\":19999,\"AlcoholicStrengthByVolumeInPercentage\":0.5,\"FiscalMarkUsedFlag\":\"0\",\"PackageValue\":[{\"KindOfPackages\":\"BA\",\"NumberOfPackages\":\"2\"}]}],\"EadEsadDraft\":{\"LocalReferenceNumber\":\"lrnie8158976912\",\"InvoiceNumber\":\"Test123\",\"InvoiceDate\":\"2023-09-09\",\"OriginTypeCode\":\"1\",\"DateOfDispatch\":\"2024-02-05\",\"TimeOfDispatch\":\"12:00:00\",\"ImportSad\":[]},\"TransportDetails\":[{\"TransportUnitCode\":\"1\",\"IdentityOfTransportUnits\":\"100\"}]}}}")
}
