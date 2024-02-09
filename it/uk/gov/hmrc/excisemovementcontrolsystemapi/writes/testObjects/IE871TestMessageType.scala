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

object IE871TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn6:IE871 xmlns:urn6="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE871:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn6:Header>
        <urn:MessageSender>NDEA.XI</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2015-08-06</urn:DateOfPreparation>
        <urn:TimeOfPreparation>03:44:00</urn:TimeOfPreparation>
        <urn:MessageIdentifier>1caa434b6c1b46f9aaaeca1ff2273</urn:MessageIdentifier>
        <!--Optional:-->
        <urn:CorrelationIdentifier>054f764f07d24664b0d2351dfd19d09d</urn:CorrelationIdentifier>
      </urn6:Header>
      <urn6:Body>
        <urn6:ExplanationOnReasonForShortage>
          <urn6:Attributes>
            <urn6:SubmitterType>2</urn6:SubmitterType>
            <!--Optional:-->
            <urn6:DateAndTimeOfValidationOfExplanationOnShortage>2006-12-27T09:49:58</urn6:DateAndTimeOfValidationOfExplanationOnShortage>
          </urn6:Attributes>
          <!--Optional:-->
          <urn6:ConsigneeTrader language="to">
            <!--Optional:-->
            <urn6:Traderid>GBWK002281034</urn6:Traderid>
            <urn6:TraderName>token</urn6:TraderName>
            <urn6:StreetName>token</urn6:StreetName>
            <!--Optional:-->
            <urn6:StreetNumber>token</urn6:StreetNumber>
            <urn6:Postcode>token</urn6:Postcode>
            <urn6:City>token</urn6:City>
            <!--Optional:-->
            <urn6:EoriNumber>token</urn6:EoriNumber>
          </urn6:ConsigneeTrader>
          <urn6:ExciseMovement>
            <urn6:AdministrativeReferenceCode>23XI00000000000000016</urn6:AdministrativeReferenceCode>
            <urn6:SequenceNumber>12</urn6:SequenceNumber>
          </urn6:ExciseMovement>
          <!--Optional:-->
          <urn6:ConsignorTrader language="to">
            <urn6:TraderExciseNumber>GBWK002281024</urn6:TraderExciseNumber>
            <urn6:TraderName>token</urn6:TraderName>
            <urn6:StreetName>token</urn6:StreetName>
            <!--Optional:-->
            <urn6:StreetNumber>token</urn6:StreetNumber>
            <urn6:Postcode>token</urn6:Postcode>
            <urn6:City>token</urn6:City>
          </urn6:ConsignorTrader>
          <!--Optional:-->
          <urn6:Analysis>
            <urn6:DateOfAnalysis>2002-02-01</urn6:DateOfAnalysis>
            <urn6:GlobalExplanation language="to">token</urn6:GlobalExplanation>
          </urn6:Analysis>
          <!--Zero or more repetitions:-->
          <urn6:BodyAnalysis>
            <urn6:ExciseProductCode>toke</urn6:ExciseProductCode>
            <urn6:BodyRecordUniqueReference>45</urn6:BodyRecordUniqueReference>
            <urn6:Explanation language="to">token</urn6:Explanation>
            <!--Optional:-->
            <urn6:ActualQuantity>1000.0</urn6:ActualQuantity>
          </urn6:BodyAnalysis>
        </urn6:ExplanationOnReasonForShortage>
      </urn6:Body>
    </urn6:IE871>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2015-08-06\",\"TimeOfPreparation\":\"03:44:00\",\"MessageIdentifier\":\"1caa434b6c1b46f9aaaeca1ff2273\",\"CorrelationIdentifier\":\"054f764f07d24664b0d2351dfd19d09d\"},\"Body\":{\"ExplanationOnReasonForShortage\":{\"AttributesValue\":{\"SubmitterType\":\"2\",\"DateAndTimeOfValidationOfExplanationOnShortage\":\"2006-12-27T09:49:58\"},\"ConsigneeTrader\":{\"Traderid\":\"GBWK002281034\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"EoriNumber\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000016\",\"SequenceNumber\":\"12\"},\"ConsignorTrader\":{\"TraderExciseNumber\":\"GBWK002281024\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"Analysis\":{\"DateOfAnalysis\":\"2002-02-01\",\"GlobalExplanation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}}},\"BodyAnalysis\":[{\"ExciseProductCode\":\"toke\",\"BodyRecordUniqueReference\":\"45\",\"Explanation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"ActualQuantity\":1000}]}}}")
}
