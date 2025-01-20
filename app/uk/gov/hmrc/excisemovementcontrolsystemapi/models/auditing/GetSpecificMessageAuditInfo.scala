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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptySeq
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Json, OWrites}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.GetSpecificMovementAuditInfo.commaWriter

//TODO: Re-review with kara
case class GetSpecificMessageRequestAuditInfo(movementId: String, messageId: String)

object GetSpecificMessageRequestAuditInfo {
  implicit val writes = Json.writes[GetSpecificMessageRequestAuditInfo]
}

//[info] auditService.getInformationForGetSpecificMessage(
//[info]     GetSpecificMessageRequestAuditInfo("cfdb20c7-d0b0-4b8b-a071-737d68dede5e", "1123e1aa-cbb0-4edc-a63c-768d1e19703d"),
//[info]     GetSpecificMessageResponseAuditInfo(Some("PORTAL6de1b822562c43fb9220d236e487c920"), "IE801", "MovementGenerated", "lrn", Some("arc"), "ern", Some("consigneeId")),
//[info]     UserDetails("testInternalId", "testGroupId"),
//[info]     NonEmptySeq(testErn, otherErn),
//[info]     <any>

//[info] auditService.getInformationForGetSpecificMessage(
//[info]     GetSpecificMessageRequestAuditInfo(cfdb20c7-d0b0-4b8b-a071-737d68dede5e,1123e1aa-cbb0-4edc-a63c-768d1e19703d),
//[info]     GetSpecificMessageResponseAuditInfo(Some(PORTAL6de1b822562c43fb9220d236e487c920),IE801,MovementGenerated,lrn,Some(arc),testErn,Some(consigneeId)),
//[info]     UserDetails(testInternalId,testGroupId),
//[info]     List(testErn),
//[info]     HeaderCarrier(None,None,None,None,RequestChain(aa4b),235091704135833,List(),None,None,None,None,None,None,List((Accept,application/vnd.hmrc.1.0+xml), (path,/)))

case class GetSpecificMessageResponseAuditInfo(
  correlationId: Option[String],
  messageTypeCode: String,
  messageType: String,
  localReferenceNumber: String,
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: Option[String]
)

object GetSpecificMessageResponseAuditInfo {
  implicit val write: OWrites[GetSpecificMessageResponseAuditInfo] =
    (
      (JsPath \ "correlationId").write[Option[String]] and
        (JsPath \ "messageTypeCode").write[String] and
        (JsPath \ "messageType").write[String] and
        (JsPath \ "localReferenceNumber").write[String] and
        (JsPath \ "administrativeReferenceCode").writeNullable[String] and
        (JsPath \ "consignorId").write[String] and
        (JsPath \ "consigneeId").write[Option[String]]
    )(unlift(GetSpecificMessageResponseAuditInfo.unapply))
}

case class GetSpecificMessageAuditInfo(
  requestType: String = "SpecificMessage",
  request: GetSpecificMessageRequestAuditInfo,
  response: GetSpecificMessageResponseAuditInfo,
  userDetails: UserDetails,
  authExciseNumber: NonEmptySeq[String]
)

object GetSpecificMessageAuditInfo {
  implicit val write: OWrites[GetSpecificMessageAuditInfo] =
    (
      (JsPath \ "requestType").write[String] and
        (JsPath \ "request").write[GetSpecificMessageRequestAuditInfo] and
        (JsPath \ "response").write[GetSpecificMessageResponseAuditInfo] and
        (JsPath \ "userDetails").write[UserDetails] and
        (JsPath \ "authExciseNumber").write[NonEmptySeq[String]](commaWriter)
    )(unlift(GetSpecificMessageAuditInfo.unapply))

}
