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

object IE810TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn10:IE810 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                 xmlns:urn10="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.01">
      <urn10:Header>
        <urn:MessageSender>NDEA.XI</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
        <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>X000012</urn:MessageIdentifier>
        <!--Optional:-->
        <urn:CorrelationIdentifier>X000013</urn:CorrelationIdentifier>
      </urn10:Header>
      <urn10:Body>
        <urn10:CancellationOfEAD>
          <urn10:Attributes>
            <!--Optional:-->
            <urn10:DateAndTimeOfValidationOfCancellation>2006-08-19T18:27:14</urn10:DateAndTimeOfValidationOfCancellation>
          </urn10:Attributes>
          <urn10:ExciseMovementEad>
            <urn10:AdministrativeReferenceCode>23XI00000000000000090</urn10:AdministrativeReferenceCode>
          </urn10:ExciseMovementEad>
          <urn10:Cancellation>
            <urn10:CancellationReasonCode>1</urn10:CancellationReasonCode>
            <!--Optional:-->
            <urn10:ComplementaryInformation language="to">token</urn10:ComplementaryInformation>
          </urn10:Cancellation>
        </urn10:CancellationOfEAD>
      </urn10:Body>
    </urn10:IE810>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2008-09-29\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"X000012\",\"CorrelationIdentifier\":\"X000013\"},\"Body\":{\"CancellationOfEAD\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfCancellation\":\"2006-08-19T18:27:14\"},\"ExciseMovementEad\":{\"AdministrativeReferenceCode\":\"23XI00000000000000090\"},\"Cancellation\":{\"CancellationReasonCode\":\"1\",\"ComplementaryInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}}}}}}")
}
