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

package uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects
import play.api.libs.json.{JsValue, Json}

object IE819TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse(
    "{\"Header\":{\"MessageSender\":\"NDEA.GB\",\"MessageRecipient\":\"NDEA.GB\",\"DateOfPreparation\":\"2023-08-31\",\"TimeOfPreparation\":\"11:32:45\",\"MessageIdentifier\":\"GB100000000302820\",\"CorrelationIdentifier\":\"PORTAL6de1b822562c43fb9220d236e487c920\"},\"Body\":{\"AlertOrRejectionOfEADESAD\":{\"AttributesValue\":{\"DateAndTimeOfValidationOfAlertRejection\":\"2023-08-31T11:32:47\"},\"ConsigneeTrader\":{\"Traderid\":\"GBWK002281023\",\"TraderName\":\"Roms PLC\",\"StreetName\":\"Bellhouston Road\",\"StreetNumber\":\"420\",\"Postcode\":\"G41 5BS\",\"City\":\"Glasgow\",\"attributes\":{\"@language\":\"en\"}},\"ExciseMovement\":{\"AdministrativeReferenceCode\":\"23GB00000000000378574\",\"SequenceNumber\":\"1\"},\"DestinationOffice\":{\"ReferenceNumber\":\"GB004098\"},\"AlertOrRejection\":{\"DateOfAlertOrRejection\":\"2023-08-31\",\"EadEsadRejectedFlag\":\"1\"},\"AlertOrRejectionOfEadEsadReason\":[{\"AlertOrRejectionOfMovementReasonCode\":\"2\",\"ComplementaryInformation\":{\"value\":\"test\",\"attributes\":{\"@language\":\"en\"}}}]}}}"
  )

  override def auditEvent: JsValue = Json.parse(
    """{"messageCode":"IE819","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-08-31","TimeOfPreparation":"11:32:45","MessageIdentifier":"GB100000000302820","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"AlertOrRejectionOfEADESAD":{"AttributesValue":{"DateAndTimeOfValidationOfAlertRejection":"2023-08-31T11:32:47"},"ConsigneeTrader":{"Traderid":"GBWK002281023","TraderName":"Roms PLC","StreetName":"Bellhouston Road","StreetNumber":"420","Postcode":"G41 5BS","City":"Glasgow","attributes":{"@language":"en"}},"ExciseMovement":{"AdministrativeReferenceCode":"23GB00000000000378574","SequenceNumber":"1"},"DestinationOffice":{"ReferenceNumber":"GB004098"},"AlertOrRejection":{"DateOfAlertOrRejection":"2023-08-31","EadEsadRejectedFlag":"1"},"AlertOrRejectionOfEadEsadReason":[{"AlertOrRejectionOfMovementReasonCode":"2","ComplementaryInformation":{"value":"test","attributes":{"@language":"en"}}}]}}},"outcome":{"status":"SUCCESS"}}"""
  )

  override def auditFailure(failureReason: String): JsValue = Json.parse(
    s"""{"messageCode":"IE819","content":{"Header":{"MessageSender":"NDEA.GB","MessageRecipient":"NDEA.GB","DateOfPreparation":"2023-08-31","TimeOfPreparation":"11:32:45","MessageIdentifier":"GB100000000302820","CorrelationIdentifier":"PORTAL6de1b822562c43fb9220d236e487c920"},"Body":{"AlertOrRejectionOfEADESAD":{"AttributesValue":{"DateAndTimeOfValidationOfAlertRejection":"2023-08-31T11:32:47"},"ConsigneeTrader":{"Traderid":"GBWK002281023","TraderName":"Roms PLC","StreetName":"Bellhouston Road","StreetNumber":"420","Postcode":"G41 5BS","City":"Glasgow","attributes":{"@language":"en"}},"ExciseMovement":{"AdministrativeReferenceCode":"23GB00000000000378574","SequenceNumber":"1"},"DestinationOffice":{"ReferenceNumber":"GB004098"},"AlertOrRejection":{"DateOfAlertOrRejection":"2023-08-31","EadEsadRejectedFlag":"1"},"AlertOrRejectionOfEadEsadReason":[{"AlertOrRejectionOfMovementReasonCode":"2","ComplementaryInformation":{"value":"test","attributes":{"@language":"en"}}}]}}},"outcome":{"status":"FAILURE","failureReason":"$failureReason"}}"""
  )
}
