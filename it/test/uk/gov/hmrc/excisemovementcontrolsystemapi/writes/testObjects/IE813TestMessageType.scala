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

object IE813TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-08-15\",\"TimeOfPreparation\":\"11:54:25\",\"MessageIdentifier\":\"GB100000000302715\",\"CorrelationIdentifier\":\"PORTAL906384fb126d43e787a802683c03b44c\"},\"Body\":{\"ChangeOfDestination\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfChangeOfDestination\":\"2023-08-15T11:54:32\"},\"UpdateEadEsad\":{\"AdministrativeReferenceCode\":\"23GB00000000000378126\",\"JourneyTime\":\"D02\",\"ChangedTransportArrangement\":\"1\",\"SequenceNumber\":\"3\",\"InvoiceNumber\":\"5678\",\"TransportModeCode\":\"4\"},\"DestinationChanged\":{\"DestinationTypeCode\":\"1\",\"NewConsigneeTrader\":{\"Traderid\":\"GBWK240176600\",\"TraderName\":\"pqr\",\"StreetName\":\"Tattenhoe Park\",\"StreetNumber\":\"18 Priestly\",\"Postcode\":\"MK4 4NW\",\"City\":\"Milton Keynes\",\"attributes\":{\"@language\":\"en\"}},\"DeliveryPlaceTrader\":{\"Traderid\":\"GB00240176601\",\"TraderName\":\"lmn\",\"StreetName\":\"Tattenhoe Park\",\"StreetNumber\":\"18 Priestl\",\"Postcode\":\"MK4 4NW\",\"City\":\"Milton Keynes\",\"attributes\":{\"@language\":\"en\"}},\"MovementGuarantee\":{\"GuarantorTypeCode\":\"1\",\"GuarantorTrader\":[]}},\"NewTransporterTrader\":{\"TraderName\":\"pqr\",\"StreetName\":\"Tattenhoe Park\",\"StreetNumber\":\"18 Priestly\",\"Postcode\":\"MK4 4NW\",\"City\":\"Milton Keynes\",\"attributes\":{\"@language\":\"en\"}},\"TransportDetails\":[{\"TransportUnitCode\":\"1\",\"IdentityOfTransportUnits\":\"1\"}]}}}")

  override def auditEvent: JsValue = Json.parse("""{"messageCode":"IE813","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-08-15","TimeOfPreparation":"11:54:25","MessageIdentifier":"GB100000000302715","CorrelationIdentifier":"PORTAL906384fb126d43e787a802683c03b44c"},"Body":{"ChangeOfDestination":{"AttributesValue":{"DateAndTimeOfValidationOfChangeOfDestination":"2023-08-15T11:54:32"},"UpdateEadEsad":{"AdministrativeReferenceCode":"23GB00000000000378126","JourneyTime":"D02","ChangedTransportArrangement":"1","SequenceNumber":"3","InvoiceNumber":"5678","TransportModeCode":"4"},"DestinationChanged":{"DestinationTypeCode":"1","NewConsigneeTrader":{"Traderid":"GBWK240176600","TraderName":"pqr","StreetName":"Tattenhoe Park","StreetNumber":"18 Priestly","Postcode":"MK4 4NW","City":"Milton Keynes","attributes":{"@language":"en"}},"DeliveryPlaceTrader":{"Traderid":"GB00240176601","TraderName":"lmn","StreetName":"Tattenhoe Park","StreetNumber":"18 Priestl","Postcode":"MK4 4NW","City":"Milton Keynes","attributes":{"@language":"en"}},"MovementGuarantee":{"GuarantorTypeCode":"1","GuarantorTrader":[]}},"NewTransporterTrader":{"TraderName":"pqr","StreetName":"Tattenhoe Park","StreetNumber":"18 Priestly","Postcode":"MK4 4NW","City":"Milton Keynes","attributes":{"@language":"en"}},"TransportDetails":[{"TransportUnitCode":"1","IdentityOfTransportUnits":"1"}]}}},"outcome":{"status":"SUCCESS"}}""")

  override def auditFailure(failureReason: String): JsValue = Json.parse(s"""{"messageCode":"IE813","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-08-15","TimeOfPreparation":"11:54:25","MessageIdentifier":"GB100000000302715","CorrelationIdentifier":"PORTAL906384fb126d43e787a802683c03b44c"},"Body":{"ChangeOfDestination":{"AttributesValue":{"DateAndTimeOfValidationOfChangeOfDestination":"2023-08-15T11:54:32"},"UpdateEadEsad":{"AdministrativeReferenceCode":"23GB00000000000378126","JourneyTime":"D02","ChangedTransportArrangement":"1","SequenceNumber":"3","InvoiceNumber":"5678","TransportModeCode":"4"},"DestinationChanged":{"DestinationTypeCode":"1","NewConsigneeTrader":{"Traderid":"GBWK240176600","TraderName":"pqr","StreetName":"Tattenhoe Park","StreetNumber":"18 Priestly","Postcode":"MK4 4NW","City":"Milton Keynes","attributes":{"@language":"en"}},"DeliveryPlaceTrader":{"Traderid":"GB00240176601","TraderName":"lmn","StreetName":"Tattenhoe Park","StreetNumber":"18 Priestl","Postcode":"MK4 4NW","City":"Milton Keynes","attributes":{"@language":"en"}},"MovementGuarantee":{"GuarantorTypeCode":"1","GuarantorTrader":[]}},"NewTransporterTrader":{"TraderName":"pqr","StreetName":"Tattenhoe Park","StreetNumber":"18 Priestly","Postcode":"MK4 4NW","City":"Milton Keynes","attributes":{"@language":"en"}},"TransportDetails":[{"TransportUnitCode":"1","IdentityOfTransportUnits":"1"}]}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}""")
}
