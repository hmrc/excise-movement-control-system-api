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

object IE881TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.XI\",\"DateOfPreparation\":\"2023-07-01\",\"TimeOfPreparation\":\"03:18:33\",\"MessageIdentifier\":\"XI00432M\",\"CorrelationIdentifier\":\"6dddas1231ff3111f3233\"},\"Body\":{\"ManualClosureResponse\":{\"AttributesValue\":{\"AdministrativeReferenceCode\":\"23XI00000000000056349\",\"SequenceNumber\":\"1\",\"DateOfArrivalOfExciseProducts\":\"2023-06-30\",\"GlobalConclusionOfReceipt\":\"3\",\"ComplementaryInformation\":{\"value\":\"Manual closure request recieved\",\"attributes\":{\"@language\":\"en\"}},\"ManualClosureRequestReasonCode\":\"1\",\"ManualClosureRequestReasonCodeComplement\":{\"value\":\"Nice try\",\"attributes\":{\"@language\":\"en\"}},\"ManualClosureRequestAccepted\":\"1\"},\"SupportingDocuments\":[{\"SupportingDocumentDescription\":{\"value\":\"XI8466333A\",\"attributes\":{\"@language\":\"en\"}},\"ReferenceOfSupportingDocument\":{\"value\":\"Closure request\",\"attributes\":{\"@language\":\"en\"}},\"ImageOfDocument\":[99,105,114,99,117,109],\"SupportingDocumentType\":\"pdf\"},{\"SupportingDocumentDescription\":{\"value\":\"XI8466333B\",\"attributes\":{\"@language\":\"en\"}},\"ReferenceOfSupportingDocument\":{\"value\":\"Closure request\",\"attributes\":{\"@language\":\"en\"}},\"ImageOfDocument\":[99,105,114,99,117,109],\"SupportingDocumentType\":\"pdf\"}],\"BodyManualClosure\":[{\"BodyRecordUniqueReference\":\"11\",\"IndicatorOfShortageOrExcess\":\"S\",\"ObservedShortageOrExcess\":1000,\"ExciseProductCode\":\"W200\",\"RefusedQuantity\":1000,\"ComplementaryInformation\":{\"value\":\"Not supplied goods promised\",\"attributes\":{\"@language\":\"en\"}}}]}}}")
}
