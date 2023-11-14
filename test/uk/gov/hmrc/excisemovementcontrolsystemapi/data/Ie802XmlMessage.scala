/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.data

object Ie802XmlMessage {
  lazy val IE802 =
    <IE802 xmlns:IE802="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.01">
      <IE802:Header>
        <MessageSender>token</MessageSender>
        <MessageRecipient>token</MessageRecipient>
        <DateOfPreparation>2015-08-24</DateOfPreparation>
        <TimeOfPreparation>23:07:00+01:00</TimeOfPreparation>
        <MessageIdentifier>token</MessageIdentifier>
        <CorrelationIdentifier>token</CorrelationIdentifier>
      </IE802:Header>
      <IE802:Body>
        <IE802:ReminderMessageForExciseMovement>
          <IE802:Attributes>
            <IE802:DateAndTimeOfIssuanceOfReminder>2000-04-21T01:36:55+01:00</IE802:DateAndTimeOfIssuanceOfReminder>
            <IE802:ReminderInformation language="to">token</IE802:ReminderInformation>
            <IE802:LimitDateAndTime>2017-04-19T15:38:57+01:00</IE802:LimitDateAndTime>
            <IE802:ReminderMessageType>1</IE802:ReminderMessageType>
          </IE802:Attributes>
          <IE802:ExciseMovement>
            <IE802:AdministrativeReferenceCode>tokentokentokentokent</IE802:AdministrativeReferenceCode>
            <IE802:SequenceNumber>to</IE802:SequenceNumber>
          </IE802:ExciseMovement>
        </IE802:ReminderMessageForExciseMovement>
      </IE802:Body>
    </IE802>
}
