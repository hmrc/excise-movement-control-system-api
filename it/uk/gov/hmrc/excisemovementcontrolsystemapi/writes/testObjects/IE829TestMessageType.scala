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

object IE829TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.AT\",\"DateOfPreparation\":\"2023-06-26\",\"TimeOfPreparation\":\"09:15:33\",\"MessageIdentifier\":\"XI004321B\",\"CorrelationIdentifier\":\"6dddas342231ff3a67888bbcedec3435\"},\"Body\":{\"NotificationOfAcceptedExport\":{\"AttributesValue\":{\"DateAndTimeOfIssuance\":\"2024-06-26T09:14:54\"},\"ConsigneeTrader\":{\"Traderid\":\"AT00000612157\",\"TraderName\":\"Whale Oil Lamps Co.\",\"StreetName\":\"The Street\",\"Postcode\":\"MC232\",\"City\":\"Happy Town\",\"EoriNumber\":\"7\",\"attributes\":{\"@language\":\"en\"}},\"ExciseMovementEad\":[{\"AdministrativeReferenceCode\":\"23XI00000000000056339\",\"SequenceNumber\":\"1\"},{\"AdministrativeReferenceCode\":\"23XI00000000000056340\",\"SequenceNumber\":\"1\"}],\"ExportPlaceCustomsOffice\":{\"ReferenceNumber\":\"AT633734\"},\"ExportAcceptance\":{\"ReferenceNumberOfSenderCustomsOffice\":\"AT324234\",\"IdentificationOfSenderCustomsOfficer\":\"84884\",\"DateOfAcceptance\":\"2023-06-26\",\"DocumentReferenceNumber\":\"123123vmnfhsdf3AT\"}}}}")
}
