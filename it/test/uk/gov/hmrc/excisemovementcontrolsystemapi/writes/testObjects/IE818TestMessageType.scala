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

object IE818TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-08-30\",\"TimeOfPreparation\":\"13:53:53.425279\",\"MessageIdentifier\":\"GB100000000302814\",\"CorrelationIdentifier\":\"73e87e9b-1145-4dbf-8ee3-807ac103ba62\"},\"Body\":{\"AcceptedOrRejectedReportOfReceiptExport\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfReportOfReceiptExport\":\"2023-08-30T14:53:56\"},\"ConsigneeTrader\":{\"Traderid\":\"GBWK002281023\",\"TraderName\":\"Meredith Ent\",\"StreetName\":\"Romanus Crescent\",\"StreetNumber\":\"38\",\"Postcode\":\"SE24 5GY\",\"City\":\"London\",\"attributes\":{\"@language\":\"en\"}},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23GB00000000000378553\",\"SequenceNumber\":\"1\"},\"DeliveryPlaceTrader\":{\"Traderid\":\"GB00002281023\",\"TraderName\":\"Meredith Ent\",\"StreetName\":\"Romanus Crescent\",\"StreetNumber\":\"38\",\"Postcode\":\"SE24 5GY\",\"City\":\"London\",\"attributes\":{\"@language\":\"en\"}},\"DestinationOffice\":{\"ReferenceNumber\":\"GB004098\"},\"ReportOfReceiptExport\":{\"DateOfArrivalOfExciseProducts\":\"2023-08-30\",\"GlobalConclusionOfReceipt\":\"3\"},\"BodyReportOfReceiptExport\":[{\"BodyRecordUniqueReference\":\"1\",\"ExciseProductCode\":\"B000\",\"UnsatisfactoryReason\":[{\"UnsatisfactoryReasonCode\":\"2\",\"ComplementaryInformation\":{\"value\":\"All is good :)\",\"attributes\":{\"@language\":\"en\"}}}]}]}}}")
}
