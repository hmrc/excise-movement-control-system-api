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

object IE871TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-08-15\",\"TimeOfPreparation\":\"09:57:17\",\"MessageIdentifier\":\"GB100000000302708\",\"CorrelationIdentifier\":\"PORTAL56f290f317b947c79ee93b806800351b\"},\"Body\":{\"ExplanationOnReasonForShortage\":{\"AttributesValue\":{\"SubmitterType\":\"1\",\"DateAndTimeOfValidationOfExplanationOnShortage\":\"2023-08-15T09:57:19\"},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23GB00000000000377768\",\"SequenceNumber\":\"1\"},\"ConsignorTrader\":{\"TraderExciseNumber\":\"GBWK240176600\",\"TraderName\":\"CHARLES HASWELL AND PARTNERS LTD\",\"StreetName\":\"1\",\"Postcode\":\"AA11AA\",\"City\":\"1\",\"attributes\":{\"@language\":\"en\"}},\"Analysis\":{\"DateOfAnalysis\":\"2023-08-15\",\"GlobalExplanation\":{\"value\":\"Courier drank the wine\",\"attributes\":{\"@language\":\"en\"}}},\"BodyAnalysis\":[]}}}")
}
