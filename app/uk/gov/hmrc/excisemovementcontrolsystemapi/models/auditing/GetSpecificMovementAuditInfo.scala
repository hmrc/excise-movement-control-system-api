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
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.CommonFormats

case class GetSpecificMovementRequestAuditInfo(movementId: String)

object GetSpecificMovementRequestAuditInfo {
  implicit val write = Json.writes[GetSpecificMovementRequestAuditInfo]
}

case class GetSpecificMovementAuditInfo(
  requestType: String = "SpecificMovement",
  request: GetSpecificMovementRequestAuditInfo,
  userDetails: UserDetails,
  authExciseNumber: NonEmptySeq[String]
)

object GetSpecificMovementAuditInfo extends CommonFormats {

  implicit val write: OWrites[GetSpecificMovementAuditInfo] =
    (
      (JsPath \ "requestType").write[String] and
        (JsPath \ "request").write[GetSpecificMovementRequestAuditInfo] and
        (JsPath \ "userDetails").write[UserDetails] and
        (JsPath \ "authExciseNumber").write[NonEmptySeq[String]](commaWriter)
    )(unlift(GetSpecificMovementAuditInfo.unapply))

}
