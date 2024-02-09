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

object IE881TestMessageType extends TestMessageType {
  override def xml1: NodeSeq =
    <urn5:IE881 xmlns:urn5="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE881:V3.01"
                xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
      <urn5:Header>
        <urn:MessageSender>NDEA.GB</urn:MessageSender>
        <urn:MessageRecipient>NDEA.XI</urn:MessageRecipient>
        <urn:DateOfPreparation>2023-07-01</urn:DateOfPreparation>
        <urn:TimeOfPreparation>03:18:33</urn:TimeOfPreparation>
        <urn:MessageIdentifier>XI00432M</urn:MessageIdentifier>
        <urn:CorrelationIdentifier>6dddas1231ff3111f3233</urn:CorrelationIdentifier>
      </urn5:Header>
      <urn5:Body>
        <urn5:ManualClosureResponse>
          <urn5:Attributes>
            <urn5:AdministrativeReferenceCode>23XI00000000000056349</urn5:AdministrativeReferenceCode>
            <urn5:SequenceNumber>1</urn5:SequenceNumber>
            <urn5:DateOfArrivalOfExciseProducts>2023-06-30</urn5:DateOfArrivalOfExciseProducts>
            <urn5:GlobalConclusionOfReceipt>3</urn5:GlobalConclusionOfReceipt>
            <urn5:ComplementaryInformation language="en">Manual closure request recieved</urn5:ComplementaryInformation>
            <urn5:ManualClosureRequestReasonCode>1</urn5:ManualClosureRequestReasonCode>
            <urn5:ManualClosureRequestReasonCodeComplement language="en">Nice try</urn5:ManualClosureRequestReasonCodeComplement>
            <urn5:ManualClosureRequestAccepted>1</urn5:ManualClosureRequestAccepted>
          </urn5:Attributes>
          <urn5:SupportingDocuments>
            <urn5:SupportingDocumentDescription language="en">XI8466333A</urn5:SupportingDocumentDescription>
            <urn5:ReferenceOfSupportingDocument language="en">Closure request</urn5:ReferenceOfSupportingDocument>
            <urn5:ImageOfDocument>Y2lyY3Vt</urn5:ImageOfDocument>
            <urn5:SupportingDocumentType>pdf</urn5:SupportingDocumentType>
          </urn5:SupportingDocuments>
          <urn5:SupportingDocuments>
            <urn5:SupportingDocumentDescription language="en">XI8466333B</urn5:SupportingDocumentDescription>
            <urn5:ReferenceOfSupportingDocument language="en">Closure request</urn5:ReferenceOfSupportingDocument>
            <urn5:ImageOfDocument>Y2lyY3Vt</urn5:ImageOfDocument>
            <urn5:SupportingDocumentType>pdf</urn5:SupportingDocumentType>
          </urn5:SupportingDocuments>
          <urn5:BodyManualClosure>
            <urn5:BodyRecordUniqueReference>11</urn5:BodyRecordUniqueReference>
            <urn5:IndicatorOfShortageOrExcess>S</urn5:IndicatorOfShortageOrExcess>
            <urn5:ObservedShortageOrExcess>1000</urn5:ObservedShortageOrExcess>
            <urn5:ExciseProductCode>W200</urn5:ExciseProductCode>
            <urn5:RefusedQuantity>1000</urn5:RefusedQuantity>
            <urn5:ComplementaryInformation language="en">Not supplied goods promised</urn5:ComplementaryInformation>
          </urn5:BodyManualClosure>
        </urn5:ManualClosureResponse>
      </urn5:Body>
    </urn5:IE881>

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-07-01\",\"TimeOfPreparation\":\"03:18:33\",\"MessageIdentifier\":\"XI00432M\",\"CorrelationIdentifier\":\"6dddas1231ff3111f3233\"},\"Body\":{\"ManualClosureResponse\":{\"AttributesValue\":{\"AdministrativeReferenceCode\":\"23XI00000000000056349\",\"SequenceNumber\":\"1\",\"DateOfArrivalOfExciseProducts\":\"2023-06-30\",\"GlobalConclusionOfReceipt\":\"3\",\"ComplementaryInformation\":{\"value\":\"Manual closure request recieved\",\"attributes\":{\"@language\":\"en\"}},\"ManualClosureRequestReasonCode\":\"1\",\"ManualClosureRequestReasonCodeComplement\":{\"value\":\"Nice try\",\"attributes\":{\"@language\":\"en\"}},\"ManualClosureRequestAccepted\":\"1\"},\"SupportingDocuments\":[{\"SupportingDocumentDescription\":{\"value\":\"XI8466333A\",\"attributes\":{\"@language\":\"en\"}},\"ReferenceOfSupportingDocument\":{\"value\":\"Closure request\",\"attributes\":{\"@language\":\"en\"}},\"ImageOfDocument\":[99,105,114,99,117,109],\"SupportingDocumentType\":\"pdf\"},{\"SupportingDocumentDescription\":{\"value\":\"XI8466333B\",\"attributes\":{\"@language\":\"en\"}},\"ReferenceOfSupportingDocument\":{\"value\":\"Closure request\",\"attributes\":{\"@language\":\"en\"}},\"ImageOfDocument\":[99,105,114,99,117,109],\"SupportingDocumentType\":\"pdf\"}],\"BodyManualClosure\":[{\"BodyRecordUniqueReference\":\"11\",\"IndicatorOfShortageOrExcess\":\"S\",\"ObservedShortageOrExcess\":1000,\"ExciseProductCode\":\"W200\",\"RefusedQuantity\":1000,\"ComplementaryInformation\":{\"value\":\"Not supplied goods promised\",\"attributes\":{\"@language\":\"en\"}}}]}}}")
}
