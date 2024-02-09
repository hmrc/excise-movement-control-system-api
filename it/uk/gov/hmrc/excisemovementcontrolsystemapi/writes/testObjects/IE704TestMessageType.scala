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

object IE704TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <ns1:IE704 xmlns:ns1="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3"
               xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
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

  override def json1: JsValue = Json.parse("""{"Header":{"MessageSender":"NDEA.XI","MessageRecipient":"NDEA.XI","DateOfPreparation":"2008-09-29","TimeOfPreparation":"00:18:33","MessageIdentifier":"XI000001","CorrelationIdentifier":"9b8effe4-adca-4431-bfc2-d65bb5f1e15d"},"Body":{"GenericRefusalMessage":{"AttributesValue":{"AdministrativeReferenceCode":"23XI00000000000000012","SequenceNumber":"1","LocalReferenceNumber":"lrnie8158976912"},"FunctionalError":[{"ErrorType":"4401","ErrorReason":"token","ErrorLocation":"token","OriginalAttributeValue":"token"}]}}}""".stripMargin)

}
