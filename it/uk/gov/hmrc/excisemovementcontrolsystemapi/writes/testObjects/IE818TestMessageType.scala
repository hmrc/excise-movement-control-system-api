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

object IE818TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn1:IE818 xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn1:Header>
        <urn:MessageSender>NDEA.XI</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2006-08-04</urn:DateOfPreparation>
        <urn:TimeOfPreparation>09:43:40</urn:TimeOfPreparation>
        <urn:MessageIdentifier>X00006</urn:MessageIdentifier>
        <!--Optional:-->
        <urn:CorrelationIdentifier>X00006</urn:CorrelationIdentifier>
      </urn1:Header>
      <urn1:Body>
        <urn1:AcceptedOrRejectedReportOfReceiptExport>
          <urn1:Attributes>
            <!--Optional:-->
            <urn1:DateAndTimeOfValidationOfReportOfReceiptExport>2001-01-03T11:25:01</urn1:DateAndTimeOfValidationOfReportOfReceiptExport>
          </urn1:Attributes>
          <!--Optional:-->
          <urn1:ConsigneeTrader language="to">
            <!--Optional:-->
            <urn1:Traderid>GBWK002901025</urn1:Traderid>
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
            <urn1:AdministrativeReferenceCode>23XI00000000000000090</urn1:AdministrativeReferenceCode>
            <urn1:SequenceNumber>2</urn1:SequenceNumber>
          </urn1:ExciseMovement>
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
          <urn1:DestinationOffice>
            <urn1:ReferenceNumber>GB005045</urn1:ReferenceNumber>
          </urn1:DestinationOffice>
          <urn1:ReportOfReceiptExport>
            <urn1:DateOfArrivalOfExciseProducts>2014-01-10</urn1:DateOfArrivalOfExciseProducts>
            <urn1:GlobalConclusionOfReceipt>22</urn1:GlobalConclusionOfReceipt>
            <!--Optional:-->
            <urn1:ComplementaryInformation language="to">token</urn1:ComplementaryInformation>
          </urn1:ReportOfReceiptExport>
          <!--Zero or more repetitions:-->
          <urn1:BodyReportOfReceiptExport>
            <urn1:BodyRecordUniqueReference>123</urn1:BodyRecordUniqueReference>
            <!--Optional:-->
            <urn1:IndicatorOfShortageOrExcess>S</urn1:IndicatorOfShortageOrExcess>
            <!--Optional:-->
            <urn1:ObservedShortageOrExcess>1000.0</urn1:ObservedShortageOrExcess>
            <urn1:ExciseProductCode>toke</urn1:ExciseProductCode>
            <!--Optional:-->
            <urn1:RefusedQuantity>1000.0</urn1:RefusedQuantity>
            <!--0 to 9 repetitions:-->
            <urn1:UnsatisfactoryReason>
              <urn1:UnsatisfactoryReasonCode>12</urn1:UnsatisfactoryReasonCode>
              <!--Optional:-->
              <urn1:ComplementaryInformation language="to">token</urn1:ComplementaryInformation>
            </urn1:UnsatisfactoryReason>
          </urn1:BodyReportOfReceiptExport>
        </urn1:AcceptedOrRejectedReportOfReceiptExport>
      </urn1:Body>
    </urn1:IE818>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2006-08-04\",\"TimeOfPreparation\":\"09:43:40\",\"MessageIdentifier\":\"X00006\",\"CorrelationIdentifier\":\"X00006\"},\"Body\":{\"AcceptedOrRejectedReportOfReceiptExport\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfReportOfReceiptExport\":\"2001-01-03T11:25:01\"},\"ConsigneeTrader\":{\"Traderid\":\"GBWK002901025\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"EoriNumber\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000090\",\"SequenceNumber\":\"2\"},\"DeliveryPlaceTrader\":{\"Traderid\":\"token\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"DestinationOffice\":{\"ReferenceNumber\":\"GB005045\"},\"ReportOfReceiptExport\":{\"DateOfArrivalOfExciseProducts\":\"2014-01-10\",\"GlobalConclusionOfReceipt\":\"22\",\"ComplementaryInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}}},\"BodyReportOfReceiptExport\":[{\"BodyRecordUniqueReference\":\"123\",\"IndicatorOfShortageOrExcess\":\"S\",\"ObservedShortageOrExcess\":1000,\"ExciseProductCode\":\"toke\",\"RefusedQuantity\":1000,\"UnsatisfactoryReason\":[{\"UnsatisfactoryReasonCode\":\"12\",\"ComplementaryInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}}}]}]}}}")
}
