/*
 * Copyright 2025 HM Revenue & Customs
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

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.xml.NodeSeq

class TraderMovementXmlGenerator(
  xmlMessageGenerator: XmlMessageGenerator
) {
  def generate(ern: String, messageParams: Seq[MessageParams]): NodeSeq =
    <ns:MovementForTraderDataResponse
    xmlns:ns="http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/MovementForTraderData/3"
    xmlns:ie934="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE934:V3.13"
    xmlns:tms="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13">
      <ie934:IE934 >
        <ie934:Header>
          <tms:MessageSender>NDEA.XI</tms:MessageSender>
          <tms:MessageRecipient>NDEA.XI</tms:MessageRecipient>
          <tms:DateOfPreparation>2023-07-17</tms:DateOfPreparation>
          <tms:TimeOfPreparation>13:09:07.467</tms:TimeOfPreparation>
          <tms:MessageIdentifier>GB100000000302497</tms:MessageIdentifier>
          <tms:CorrelationIdentifier>2017112414108</tms:CorrelationIdentifier>
        </ie934:Header>
        <ie934:Body>
          <ie934:MessagePackage>
            {getMessages(ern, messageParams)}
          </ie934:MessagePackage>
        </ie934:Body>
      </ie934:IE934>
    </ns:MovementForTraderDataResponse>

  private def getMessages(ern: String, messageParams: Seq[MessageParams]): NodeSeq =
    messageParams.flatMap(params => generateMessageBody(ern, params))

  private def generateMessageBody(ern: String, params: MessageParams): NodeSeq =
    <ie934:MessageBody>
      <ie934:TechnicalMessageType>{params.messageType}</ie934:TechnicalMessageType>
      <ie934:MessageData>
        {encodeMessage(xmlMessageGenerator.generate(ern, params).toString())}
      </ie934:MessageData>
    </ie934:MessageBody>

  def encodeMessage(str: String): String =
    Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8))
}
