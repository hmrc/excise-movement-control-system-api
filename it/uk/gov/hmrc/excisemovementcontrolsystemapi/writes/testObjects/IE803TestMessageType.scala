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

object IE803TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn6:IE803 xmlns:urn6="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE803:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn6:Header>
        <urn:MessageSender>NDEA.GB</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2023-06-27</urn:DateOfPreparation>
        <urn:TimeOfPreparation>00:23:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>GB002312688</urn:MessageIdentifier>
        <urn:CorrelationIdentifier>6dddasfffff3abcb344bbcbcbcbc3435</urn:CorrelationIdentifier>
      </urn6:Header>
      <urn6:Body>
        <urn6:NotificationOfDivertedEADESAD>
          <urn6:ExciseNotification>
            <urn6:NotificationType>1</urn6:NotificationType>
            <urn6:NotificationDateAndTime>2023-06-26T23:56:46</urn6:NotificationDateAndTime>
            <urn6:AdministrativeReferenceCode>23XI00000000000056333</urn6:AdministrativeReferenceCode>
            <urn6:SequenceNumber>1</urn6:SequenceNumber>
          </urn6:ExciseNotification>
        </urn6:NotificationOfDivertedEADESAD>
      </urn6:Body>
    </urn6:IE803>


  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-06-27\",\"TimeOfPreparation\":\"00:23:33\",\"MessageIdentifier\":\"GB002312688\",\"CorrelationIdentifier\":\"6dddasfffff3abcb344bbcbcbcbc3435\"},\"Body\":{\"NotificationOfDivertedEADESAD\":{\"ExciseNotification\":{\"NotificationType\":\"1\",\"NotificationDateAndTime\":\"2023-06-26T23:56:46\",\"AdministrativeReferenceCode\":\"23XI00000000000056333\",\"SequenceNumber\":\"1\"},\"DownstreamArc\":[]}}}")
}
