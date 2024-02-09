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

object IE802TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn7:IE802 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01"
                xmlns:urn7="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.01">
      <urn7:Header>
        <urn:MessageSender>CSMISE.EC</urn:MessageSender>
        <urn:MessageRecipient>CSMISE.EC</urn:MessageRecipient>
        <urn:DateOfPreparation>2008-09-29</urn:DateOfPreparation>
        <urn:TimeOfPreparation>00:18:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>X00004</urn:MessageIdentifier>
        <!--Optional:-->
        <urn:CorrelationIdentifier>X00004</urn:CorrelationIdentifier>
      </urn7:Header>
      <urn7:Body>
        <urn7:ReminderMessageForExciseMovement>
          <urn7:Attributes>
            <urn7:DateAndTimeOfIssuanceOfReminder>2006-08-19T18:27:14</urn7:DateAndTimeOfIssuanceOfReminder>
            <!--Optional:-->
            <urn7:ReminderInformation language="to">token</urn7:ReminderInformation>
            <urn7:LimitDateAndTime>2009-05-16T13:42:28</urn7:LimitDateAndTime>
            <urn7:ReminderMessageType>2</urn7:ReminderMessageType>
          </urn7:Attributes>
          <urn7:ExciseMovement>
            <urn7:AdministrativeReferenceCode>23XI00000000000000090</urn7:AdministrativeReferenceCode>
            <urn7:SequenceNumber>10</urn7:SequenceNumber>
          </urn7:ExciseMovement>
        </urn7:ReminderMessageForExciseMovement>
      </urn7:Body>
    </urn7:IE802>


  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"CSMISE.EC\",\"MessageRecipient\":\"CSMISE.EC\",\"DateOfPreparation\":\"2008-09-29\",\"TimeOfPreparation\":\"00:18:33\",\"MessageIdentifier\":\"X00004\",\"CorrelationIdentifier\":\"X00004\"},\"Body\":{\"ReminderMessageForExciseMovement\":{\"AttributesValue\":{\"DateAndTimeOfIssuanceOfReminder\":\"2006-08-19T18:27:14\",\"ReminderInformation\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"LimitDateAndTime\":\"2009-05-16T13:42:28\",\"ReminderMessageType\":\"2\"},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000090\",\"SequenceNumber\":\"10\"}}}}")
}
