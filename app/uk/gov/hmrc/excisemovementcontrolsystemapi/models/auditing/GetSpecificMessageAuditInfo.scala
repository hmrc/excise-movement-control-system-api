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

case class GetSpecificMessageRequestAuditInfo(movementId: String, messageId: String)

object GetSpecificMessageRequestAuditInfo {
  implicit val writes = Json.writes[GetSpecificMessageRequestAuditInfo]
}

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
