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

import scala.xml.Elem

object Ie704XmlMessage {
  lazy val IE704: Elem = <IE704 xmlns:IE704="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3">
    <IE704:Header>
      <MessageSender>token</MessageSender>
      <MessageRecipient>token</MessageRecipient>
      <DateOfPreparation>2008-09-29</DateOfPreparation>
      <TimeOfPreparation>00:18:33</TimeOfPreparation>
      <MessageIdentifier>token</MessageIdentifier>
      <CorrelationIdentifier>token</CorrelationIdentifier>
    </IE704:Header>
    <IE704:Body>
      <IE704:GenericRefusalMessage>
        <IE704:Attributes>
          <IE704:AdministrativeReferenceCode>tokentokentokentokent</IE704:AdministrativeReferenceCode>
          <IE704:SequenceNumber>to</IE704:SequenceNumber>
          <IE704:LocalReferenceNumber>token</IE704:LocalReferenceNumber>
        </IE704:Attributes>
        <IE704:FunctionalError>
          <IE704:ErrorType>4518</IE704:ErrorType>
          <IE704:ErrorReason>token</IE704:ErrorReason>
          <IE704:ErrorLocation>token</IE704:ErrorLocation>
          <IE704:OriginalAttributeValue>token</IE704:OriginalAttributeValue>
        </IE704:FunctionalError>
      </IE704:GenericRefusalMessage>
    </IE704:Body>
  </IE704>
}
