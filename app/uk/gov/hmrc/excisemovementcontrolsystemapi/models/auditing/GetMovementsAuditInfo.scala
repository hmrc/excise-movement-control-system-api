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
import play.api.libs.json._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.CommonFormats

case class GetMovementsParametersAuditInfo(
  exciseRegistrationNumber: Option[String],
  administrativeReferenceCode: Option[String],
  localReferenceNumber: Option[String],
  updatedSince: Option[String],
  traderType: Option[String]
)
object GetMovementsParametersAuditInfo {
  implicit val write: OWrites[GetMovementsParametersAuditInfo] =
    (
      (JsPath \ "exciseRegistrationNumber").writeNullable[String] and
        (JsPath \ "administrativeReferenceCode").writeNullable[String] and
        (JsPath \ "localReferenceNumber").writeNullable[String] and
        (JsPath \ "updatedSince").writeNullable[String] and
        (JsPath \ "traderType").writeNullable[String]
    )(unlift(GetMovementsParametersAuditInfo.unapply))
}

case class GetMovementsResponseAuditInfo(
  numberOfMovements: Int
)
object GetMovementsResponseAuditInfo {
  implicit val writes: OWrites[GetMovementsResponseAuditInfo] = Json.writes[GetMovementsResponseAuditInfo]
}

case class GetMovementsAuditInfo(
  requestType: String = "AllMovements",
  request: GetMovementsParametersAuditInfo,
  response: GetMovementsResponseAuditInfo,
  userDetails: UserDetails,
  authExciseNumber: NonEmptySeq[String]
)
object GetMovementsAuditInfo extends CommonFormats {
  implicit val write: OWrites[GetMovementsAuditInfo] =
    (
      (JsPath \ "requestType").write[String] and
        (JsPath \ "request").write[GetMovementsParametersAuditInfo] and
        (JsPath \ "response").write[GetMovementsResponseAuditInfo] and
        (JsPath \ "userDetails").write[UserDetails] and
        (JsPath \ "authExciseNumber").write[NonEmptySeq[String]](commaWriter)
    )(unlift(GetMovementsAuditInfo.unapply))

}
