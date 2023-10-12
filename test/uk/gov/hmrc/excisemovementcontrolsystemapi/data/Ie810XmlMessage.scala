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

object Ie810XmlMessage {
  lazy val IE810 = <IE810 xmlns:IE810="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.01">
    <IE810:Header>
      <MessageSender>token</MessageSender>
      <MessageRecipient>token</MessageRecipient>
      <DateOfPreparation>2008-09-29</DateOfPreparation>
      <TimeOfPreparation>00:18:33</TimeOfPreparation>
      <MessageIdentifier>token</MessageIdentifier>
      <CorrelationIdentifier>token</CorrelationIdentifier>
    </IE810:Header>
    <IE810:Body>
      <IE810:CancellationOfEAD>
        <IE810:Attributes>
          <IE810:DateAndTimeOfValidationOfCancellation>2006-08-19T18:27:14+01:00</IE810:DateAndTimeOfValidationOfCancellation>
        </IE810:Attributes>
        <IE810:ExciseMovementEad>
          <IE810:AdministrativeReferenceCode>tokentokentokentokent</IE810:AdministrativeReferenceCode>
        </IE810:ExciseMovementEad>
        <IE810:Cancellation>
          <IE810:CancellationReasonCode>t</IE810:CancellationReasonCode>
          <IE810:ComplementaryInformation language="to">token</IE810:ComplementaryInformation>
        </IE810:Cancellation>
      </IE810:CancellationOfEAD>
    </IE810:Body>
  </IE810>

}
