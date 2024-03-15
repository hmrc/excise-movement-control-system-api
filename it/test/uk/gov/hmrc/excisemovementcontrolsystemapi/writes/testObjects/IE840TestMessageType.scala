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

object IE840TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("{\"Header\":{\"MessageSender\":\"NDEA.XI\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-06-28\",\"TimeOfPreparation\":\"00:17:33\",\"MessageIdentifier\":\"XI0003265\",\"CorrelationIdentifier\":\"6de24ff423abcb344bbcbcbcbc3499\"},\"Body\":{\"EventReportEnvelope\":{\"AttributesValue\":{\"EventReportMessageType\":\"1\",\"DateAndTimeOfValidationOfEventReport\":\"2023-06-28T00:17:33\"},\"HeaderEventReport\":{\"EventReportNumber\":\"GBAA2C3F4244ADB3\",\"MsOfSubmissionEventReportReference\":\"GB\",\"ReferenceNumberOfExciseOffice\":\"GB848884\",\"MemberStateOfEvent\":\"GB\"},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23XI00000000000000333\",\"SequenceNumber\":\"1\"},\"OtherAccompanyingDocument\":{\"OtherAccompanyingDocumentType\":\"0\",\"ShortDescriptionOfOtherAccompanyingDocument\":{\"value\":\"token\",\"attributes\":{\"@language\":\"to\"}},\"OtherAccompanyingDocumentNumber\":\"jfj3423jfsjf\",\"OtherAccompanyingDocumentDate\":\"2023-06-15\",\"ImageOfOtherAccompanyingDocument\":[102,114,101,109,117,110,116],\"MemberStateOfDispatch\":\"to\",\"MemberStateOfDestination\":\"to\",\"PersonInvolvedInMovementTrader\":[{\"TraderExciseNumber\":\"IT34HD7413733\",\"Traderid\":\"jj4123\",\"TraderName\":\"Roman Parties Ltd\",\"TraderPersonType\":\"5\",\"MemberStateCode\":\"IT\",\"StreetName\":\"Circus Maximus\",\"StreetNumber\":\"3\",\"Postcode\":\"RM32\",\"City\":\"Rome\",\"PhoneNumber\":\"992743614324\",\"FaxNumber\":\"848848484\",\"EmailAddress\":\"test@example.com\",\"attributes\":{\"@language\":\"to\"}}],\"GoodsItem\":[{\"DescriptionOfTheGoods\":\"Booze\",\"CnCode\":\"84447744\",\"CommercialDescriptionOfTheGoods\":\"Alcoholic beverages\",\"AdditionalCode\":\"token\",\"Quantity\":1000,\"UnitOfMeasureCode\":\"12\",\"GrossMass\":1000,\"NetMass\":1000}],\"MeansOfTransport\":{\"TraderName\":\"Big Lorries Ltd\",\"StreetName\":\"Magic Road\",\"StreetNumber\":\"42\",\"TransporterCountry\":\"FR\",\"Postcode\":\"DRA231\",\"City\":\"Paris\",\"TransportModeCode\":\"12\",\"AcoComplementaryInformation\":{\"value\":\"The lorry is taking the goods\",\"attributes\":{\"@language\":\"en\"}},\"Registration\":\"tj32343\",\"CountryOfRegistration\":\"FR\"}},\"EventReport\":{\"DateOfEvent\":\"2023-06-28\",\"PlaceOfEvent\":{\"value\":\"Dover\",\"attributes\":{\"@language\":\"en\"}},\"ExciseOfficerIdentification\":\"NG28324234\",\"SubmittingPerson\":\"NG2j23432\",\"SubmittingPersonCode\":\"12\",\"SubmittingPersonComplement\":{\"value\":\"Looking good\",\"attributes\":{\"@language\":\"en\"}},\"ChangedTransportArrangement\":\"2\",\"Comments\":{\"value\":\"words\",\"attributes\":{\"@language\":\"en\"}}},\"EvidenceOfEvent\":[{\"IssuingAuthority\":{\"value\":\"CSN123\",\"attributes\":{\"@language\":\"en\"}},\"EvidenceTypeCode\":\"7\",\"ReferenceOfEvidence\":{\"value\":\"token\",\"attributes\":{\"@language\":\"en\"}},\"ImageOfEvidence\":[116,117,114,98,105,110,101],\"EvidenceTypeComplement\":{\"value\":\"Nice job\",\"attributes\":{\"@language\":\"en\"}}}],\"NewTransportArrangerTrader\":{\"VatNumber\":\"GB823482\",\"TraderName\":\"New Transport Arrangers Inc.\",\"StreetName\":\"Main Street\",\"StreetNumber\":\"7655\",\"Postcode\":\"ZL23 XD1\",\"City\":\"Atlantis\",\"attributes\":{\"@language\":\"en\"}},\"NewTransporterTrader\":{\"VatNumber\":\"GDM23423\",\"TraderName\":\"New Transporters Co.\",\"StreetName\":\"Main Street\",\"StreetNumber\":\"7654\",\"Postcode\":\"ZL23 XD1\",\"City\":\"Atlantis\",\"attributes\":{\"@language\":\"en\"}},\"TransportDetails\":[{\"TransportUnitCode\":\"12\",\"IdentityOfTransportUnits\":\"Boxes\",\"CommercialSealIdentification\":\"Sealed\",\"ComplementaryInformation\":{\"value\":\"Boxes are sealed\",\"attributes\":{\"@language\":\"en\"}},\"SealInformation\":{\"value\":\"There are 33 species of seal\",\"attributes\":{\"@language\":\"en\"}}}],\"BodyEventReport\":[{\"EventTypeCode\":\"5\",\"AssociatedInformation\":{\"value\":\"Customs aren't happy\",\"attributes\":{\"@language\":\"en\"}},\"BodyRecordUniqueReference\":\"244\",\"DescriptionOfTheGoods\":\"some are missing\",\"CnCode\":\"43478962\",\"AdditionalCode\":\"NFH412\",\"IndicatorOfShortageOrExcess\":\"S\",\"ObservedShortageOrExcess\":112}]}}}")

  override def auditEvent: JsValue = Json.parse("""{"messageCode":"IE840","content":{"Header":{"MessageSender":"NDEA.XI","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-06-28","TimeOfPreparation":"00:17:33","MessageIdentifier":"XI0003265","CorrelationIdentifier":"6de24ff423abcb344bbcbcbcbc3499"},"Body":{"EventReportEnvelope":{"AttributesValue":{"EventReportMessageType":"1","DateAndTimeOfValidationOfEventReport":"2023-06-28T00:17:33"},"HeaderEventReport":{"EventReportNumber":"GBAA2C3F4244ADB3","MsOfSubmissionEventReportReference":"GB","ReferenceNumberOfExciseOffice":"GB848884","MemberStateOfEvent":"GB"},"ExciseMovement":{"AdministrativeReferenceCode":"23XI00000000000000333","SequenceNumber":"1"},"OtherAccompanyingDocument":{"OtherAccompanyingDocumentType":"0","ShortDescriptionOfOtherAccompanyingDocument":{"value":"token","attributes":{"@language":"to"}},"OtherAccompanyingDocumentNumber":"jfj3423jfsjf","OtherAccompanyingDocumentDate":"2023-06-15","ImageOfOtherAccompanyingDocument":[102,114,101,109,117,110,116],"MemberStateOfDispatch":"to","MemberStateOfDestination":"to","PersonInvolvedInMovementTrader":[{"TraderExciseNumber":"IT34HD7413733","Traderid":"jj4123","TraderName":"Roman Parties Ltd","TraderPersonType":"5","MemberStateCode":"IT","StreetName":"Circus Maximus","StreetNumber":"3","Postcode":"RM32","City":"Rome","PhoneNumber":"992743614324","FaxNumber":"848848484","EmailAddress":"test@example.com","attributes":{"@language":"to"}}],"GoodsItem":[{"DescriptionOfTheGoods":"Booze","CnCode":"84447744","CommercialDescriptionOfTheGoods":"Alcoholic beverages","AdditionalCode":"token","Quantity":1000,"UnitOfMeasureCode":"12","GrossMass":1000,"NetMass":1000}],"MeansOfTransport":{"TraderName":"Big Lorries Ltd","StreetName":"Magic Road","StreetNumber":"42","TransporterCountry":"FR","Postcode":"DRA231","City":"Paris","TransportModeCode":"12","AcoComplementaryInformation":{"value":"The lorry is taking the goods","attributes":{"@language":"en"}},"Registration":"tj32343","CountryOfRegistration":"FR"}},"EventReport":{"DateOfEvent":"2023-06-28","PlaceOfEvent":{"value":"Dover","attributes":{"@language":"en"}},"ExciseOfficerIdentification":"NG28324234","SubmittingPerson":"NG2j23432","SubmittingPersonCode":"12","SubmittingPersonComplement":{"value":"Looking good","attributes":{"@language":"en"}},"ChangedTransportArrangement":"2","Comments":{"value":"words","attributes":{"@language":"en"}}},"EvidenceOfEvent":[{"IssuingAuthority":{"value":"CSN123","attributes":{"@language":"en"}},"EvidenceTypeCode":"7","ReferenceOfEvidence":{"value":"token","attributes":{"@language":"en"}},"ImageOfEvidence":[116,117,114,98,105,110,101],"EvidenceTypeComplement":{"value":"Nice job","attributes":{"@language":"en"}}}],"NewTransportArrangerTrader":{"VatNumber":"GB823482","TraderName":"New Transport Arrangers Inc.","StreetName":"Main Street","StreetNumber":"7655","Postcode":"ZL23 XD1","City":"Atlantis","attributes":{"@language":"en"}},"NewTransporterTrader":{"VatNumber":"GDM23423","TraderName":"New Transporters Co.","StreetName":"Main Street","StreetNumber":"7654","Postcode":"ZL23 XD1","City":"Atlantis","attributes":{"@language":"en"}},"TransportDetails":[{"TransportUnitCode":"12","IdentityOfTransportUnits":"Boxes","CommercialSealIdentification":"Sealed","ComplementaryInformation":{"value":"Boxes are sealed","attributes":{"@language":"en"}},"SealInformation":{"value":"There are 33 species of seal","attributes":{"@language":"en"}}}],"BodyEventReport":[{"EventTypeCode":"5","AssociatedInformation":{"value":"Customs aren't happy","attributes":{"@language":"en"}},"BodyRecordUniqueReference":"244","DescriptionOfTheGoods":"some are missing","CnCode":"43478962","AdditionalCode":"NFH412","IndicatorOfShortageOrExcess":"S","ObservedShortageOrExcess":112}]}}},"outcome":{"status":"SUCCESS"}}""")

  override def auditFailure(failureReason: String): JsValue = Json.parse(s"""{"messageCode":"IE840","content":{"Header":{"MessageSender":"NDEA.XI","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-06-28","TimeOfPreparation":"00:17:33","MessageIdentifier":"XI0003265","CorrelationIdentifier":"6de24ff423abcb344bbcbcbcbc3499"},"Body":{"EventReportEnvelope":{"AttributesValue":{"EventReportMessageType":"1","DateAndTimeOfValidationOfEventReport":"2023-06-28T00:17:33"},"HeaderEventReport":{"EventReportNumber":"GBAA2C3F4244ADB3","MsOfSubmissionEventReportReference":"GB","ReferenceNumberOfExciseOffice":"GB848884","MemberStateOfEvent":"GB"},"ExciseMovement":{"AdministrativeReferenceCode":"23XI00000000000000333","SequenceNumber":"1"},"OtherAccompanyingDocument":{"OtherAccompanyingDocumentType":"0","ShortDescriptionOfOtherAccompanyingDocument":{"value":"token","attributes":{"@language":"to"}},"OtherAccompanyingDocumentNumber":"jfj3423jfsjf","OtherAccompanyingDocumentDate":"2023-06-15","ImageOfOtherAccompanyingDocument":[102,114,101,109,117,110,116],"MemberStateOfDispatch":"to","MemberStateOfDestination":"to","PersonInvolvedInMovementTrader":[{"TraderExciseNumber":"IT34HD7413733","Traderid":"jj4123","TraderName":"Roman Parties Ltd","TraderPersonType":"5","MemberStateCode":"IT","StreetName":"Circus Maximus","StreetNumber":"3","Postcode":"RM32","City":"Rome","PhoneNumber":"992743614324","FaxNumber":"848848484","EmailAddress":"test@example.com","attributes":{"@language":"to"}}],"GoodsItem":[{"DescriptionOfTheGoods":"Booze","CnCode":"84447744","CommercialDescriptionOfTheGoods":"Alcoholic beverages","AdditionalCode":"token","Quantity":1000,"UnitOfMeasureCode":"12","GrossMass":1000,"NetMass":1000}],"MeansOfTransport":{"TraderName":"Big Lorries Ltd","StreetName":"Magic Road","StreetNumber":"42","TransporterCountry":"FR","Postcode":"DRA231","City":"Paris","TransportModeCode":"12","AcoComplementaryInformation":{"value":"The lorry is taking the goods","attributes":{"@language":"en"}},"Registration":"tj32343","CountryOfRegistration":"FR"}},"EventReport":{"DateOfEvent":"2023-06-28","PlaceOfEvent":{"value":"Dover","attributes":{"@language":"en"}},"ExciseOfficerIdentification":"NG28324234","SubmittingPerson":"NG2j23432","SubmittingPersonCode":"12","SubmittingPersonComplement":{"value":"Looking good","attributes":{"@language":"en"}},"ChangedTransportArrangement":"2","Comments":{"value":"words","attributes":{"@language":"en"}}},"EvidenceOfEvent":[{"IssuingAuthority":{"value":"CSN123","attributes":{"@language":"en"}},"EvidenceTypeCode":"7","ReferenceOfEvidence":{"value":"token","attributes":{"@language":"en"}},"ImageOfEvidence":[116,117,114,98,105,110,101],"EvidenceTypeComplement":{"value":"Nice job","attributes":{"@language":"en"}}}],"NewTransportArrangerTrader":{"VatNumber":"GB823482","TraderName":"New Transport Arrangers Inc.","StreetName":"Main Street","StreetNumber":"7655","Postcode":"ZL23 XD1","City":"Atlantis","attributes":{"@language":"en"}},"NewTransporterTrader":{"VatNumber":"GDM23423","TraderName":"New Transporters Co.","StreetName":"Main Street","StreetNumber":"7654","Postcode":"ZL23 XD1","City":"Atlantis","attributes":{"@language":"en"}},"TransportDetails":[{"TransportUnitCode":"12","IdentityOfTransportUnits":"Boxes","CommercialSealIdentification":"Sealed","ComplementaryInformation":{"value":"Boxes are sealed","attributes":{"@language":"en"}},"SealInformation":{"value":"There are 33 species of seal","attributes":{"@language":"en"}}}],"BodyEventReport":[{"EventTypeCode":"5","AssociatedInformation":{"value":"Customs aren't happy","attributes":{"@language":"en"}},"BodyRecordUniqueReference":"244","DescriptionOfTheGoods":"some are missing","CnCode":"43478962","AdditionalCode":"NFH412","IndicatorOfShortageOrExcess":"S","ObservedShortageOrExcess":112}]}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}""")

}
