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

object IE819TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn2:IE819 xmlns:urn2="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE819:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn2:Header>
        <urn:MessageSender>NDEA.XI</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
        <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>X00008</urn:MessageIdentifier>
        <!--Optional:-->
        <urn:CorrelationIdentifier>X000008</urn:CorrelationIdentifier>
      </urn2:Header>
      <urn2:Body>
        <urn2:AlertOrRejectionOfEADESAD>
          <urn2:Attributes>
            <!--Optional:-->
            <urn2:DateAndTimeOfValidationOfAlertRejection>2006-08-19T18:27:14</urn2:DateAndTimeOfValidationOfAlertRejection>
          </urn2:Attributes>
          <!--Optional:-->
          <urn2:ConsigneeTrader language="to">
            <!--Optional:-->
            <urn2:Traderid>token</urn2:Traderid>
            <urn2:TraderName>token</urn2:TraderName>
            <urn2:StreetName>token</urn2:StreetName>
            <!--Optional:-->
            <urn2:StreetNumber>token</urn2:StreetNumber>
            <urn2:Postcode>token</urn2:Postcode>
            <urn2:City>token</urn2:City>
            <!--Optional:-->
            <urn2:EoriNumber>token</urn2:EoriNumber>
          </urn2:ConsigneeTrader>
          <urn2:ExciseMovement>
            <urn2:AdministrativeReferenceCode>23XI00000000000000090</urn2:AdministrativeReferenceCode>
            <urn2:SequenceNumber>12</urn2:SequenceNumber>
          </urn2:ExciseMovement>
          <urn2:DestinationOffice>
            <urn2:ReferenceNumber>GB004022</urn2:ReferenceNumber>
          </urn2:DestinationOffice>
          <urn2:AlertOrRejection>
            <urn2:DateOfAlertOrRejection>2009-05-16</urn2:DateOfAlertOrRejection>
            <urn2:EadEsadRejectedFlag>1</urn2:EadEsadRejectedFlag>
          </urn2:AlertOrRejection>
          <!--0 to 9 repetitions:-->
          <urn2:AlertOrRejectionOfEadEsadReason>
            <urn2:AlertOrRejectionOfMovementReasonCode>3</urn2:AlertOrRejectionOfMovementReasonCode>
            <!--Optional:-->
            <urn2:ComplementaryInformation language="to">token</urn2:ComplementaryInformation>
          </urn2:AlertOrRejectionOfEadEsadReason>
        </urn2:AlertOrRejectionOfEADESAD>
      </urn2:Body>
    </urn2:IE819>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2008-09-29\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"X00008\",\"CorrelationIdentifier\":\"X000008\"},\"Body\":{\"AlertOrRejectionOfEADESAD\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfAlertRejection\":\"2006-08-19T18:27:14\"},\"ConsigneeTrader\":{\"Traderid\":\"token\",\"TraderName\":\"token\",\"StreetName\":\"token\",\"StreetNumber\":\"token\",\"Postcode\":\"token\",\"City\":\"token\",\"EoriNumber\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000090\",\"SequenceNumber\":\"12\"},\"DestinationOffice\":{\"ReferenceNumber\":\"GB004022\"},\"AlertOrRejection\":{\"DateOfAlertOrRejection\":\"2009-05-16\",\"EadEsadRejectedFlag\":\"1\"},\"AlertOrRejectionOfEadEsadReason\":[{\"AlertOrRejectionOfMovementReasonCode\":\"3\",\"ComplementaryInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}}}]}}}")
}
