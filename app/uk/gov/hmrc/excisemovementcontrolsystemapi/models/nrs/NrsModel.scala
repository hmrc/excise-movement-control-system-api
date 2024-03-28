/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs

import play.api.libs.json._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils

final case class NrsSubmissionId(nrsSubmissionId: String)

object NrsSubmissionId {
  implicit val format: OFormat[NrsSubmissionId] = Json.format[NrsSubmissionId]
}

case class NrsMetadata(
                        businessId: String,
                        notableEvent: String,
                        payloadContentType: String,
                        payloadSha256Checksum: String,
                        userSubmissionTimestamp: String,
                        identityData: IdentityData,
                        userAuthToken: String,
                        headerData: Map[String, String],
                        searchKeys: Map[String, String]
                      )

object NrsMetadata {

  val EmcsCreateMovementNotableEventId = "emcs-create-a-movement-api"
  val EmcsChangeDestinationNotableEventId = "emcs-change-a-destination-api"
  val EmcsReportOfReceiptNotableEvent = "emcs-report-a-receipt-api"
  val EmcsExplainADelayNotableEventId = "emcs-explain-a-delay-api"
  val EmcsExplainAShortageNotableEventId = "emcs-explain-a-shortage-api"
  val EmccCancelMovement = "emcs-cancel-a-movement-api"
  val EmcsSubmitAlertOrRejectionNotableEventId = "emcs-submit-alert-or-rejection-api"

  val BusinessId = "emcs"
  val SearchKey = "ern"

  def create
  (
    payLoad: String,
    emcsUtils: EmcsUtils,
    notableEventId: String,
    identityData: IdentityData,
    submissionTimeStamp: String,
    userAuthToken: String,
    userHeaderData: Map[String, String],
    exciseNumber: String
  ): NrsMetadata = {
    NrsMetadata(
      BusinessId,
      notableEventId,
      payloadContentType = "application/xml",
      payloadSha256Checksum = emcsUtils.sha256Hash(payLoad),
      userSubmissionTimestamp = submissionTimeStamp,
      identityData = identityData,
      userAuthToken,
      headerData = userHeaderData,
      searchKeys = Map(SearchKey -> exciseNumber)
    )
  }
  implicit val format: OFormat[NrsMetadata] = Json.format[NrsMetadata]
}

case class NrsPayload(payload: String, metadata: NrsMetadata) {
  def toJsObject: JsObject = Json.toJson(this).as[JsObject]
}

object NrsPayload {
  implicit val format: OFormat[NrsPayload] = Json.format[NrsPayload]
}
