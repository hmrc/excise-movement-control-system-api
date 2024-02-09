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

object IE837TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn:IE837
  xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.01"
  xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn:Header>
        <urn1:MessageSender>token</urn1:MessageSender>
        <urn1:MessageRecipient>token</urn1:MessageRecipient>
        <urn1:DateOfPreparation>2008-09-29</urn1:DateOfPreparation>
        <urn1:TimeOfPreparation>00:18:33</urn1:TimeOfPreparation>
        <urn1:MessageIdentifier>XI004321B</urn1:MessageIdentifier>
        <!--Optional:-->
        <urn1:CorrelationIdentifier>token</urn1:CorrelationIdentifier>
      </urn:Header>
      <urn:Body>
        <urn:ExplanationOnDelayForDelivery>
          <urn:Attributes>
            <urn:SubmitterIdentification>tokentokentok</urn:SubmitterIdentification>
            <urn:SubmitterType>1</urn:SubmitterType>
            <urn:ExplanationCode>to</urn:ExplanationCode>
            <!--Optional:-->
            <urn:ComplementaryInformation language="to">token</urn:ComplementaryInformation>
            <urn:MessageRole>2</urn:MessageRole>
            <!--Optional:-->
            <urn:DateAndTimeOfValidationOfExplanationOnDelay>2008-11-15T16:52:58</urn:DateAndTimeOfValidationOfExplanationOnDelay>
          </urn:Attributes>
          <urn:ExciseMovement>
            <urn:AdministrativeReferenceCode>tokentokentokentokent</urn:AdministrativeReferenceCode>
            <urn:SequenceNumber>to</urn:SequenceNumber>
          </urn:ExciseMovement>
        </urn:ExplanationOnDelayForDelivery>
      </urn:Body>
    </urn:IE837>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"token\",\"MessageRecipient\":\"token\",\"DateOfPreparation\":\"2008-09-29\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"XI004321B\",\"CorrelationIdentifier\":\"token\"},\"Body\":{\"ExplanationOnDelayForDelivery\":{\"AttributesValue\":{\"SubmitterIdentification\":\"tokentokentok\",\"SubmitterType\":\"1\",\"ExplanationCode\":\"to\",\"ComplementaryInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"MessageRole\":\"2\",\"DateAndTimeOfValidationOfExplanationOnDelay\":\"2008-11-15T16:52:58\"},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"tokentokentokentokent\",\"SequenceNumber\":\"to\"}}}}")
}
