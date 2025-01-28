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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptySeq
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.CommonFormats

case class UserDetails(
  gatewayId: String,
  groupIdentifier: String
)

object UserDetails {
  implicit val writes = Json.writes[UserDetails]
}

case class MessageSubmittedDetails(
  messageTypeCode: String,
  messageType: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String],
  movementId: Option[String], //De option if not possible to be unknown
  consignorId: String,
  consigneeId: Option[String],
  submittedToCore: Boolean,
  messageId: String,
  correlationId: Option[String],
  userDetails: UserDetails,
  authExciseNumber: NonEmptySeq[String],
  messageDetails: JsObject
)

object MessageSubmittedDetails extends CommonFormats {

  implicit val writes: OWrites[MessageSubmittedDetails] =
    (
      (__ \ "messageTypeCode").write[String] and
        (__ \ "messageType").write[String] and
        (__ \ "localReferenceNumber").write[Option[String]] and
        (__ \ "administrativeReferenceCode").write[Option[String]] and
        (__ \ "movementId").write[Option[String]] and
        (__ \ "consignorId").write[String] and
        (__ \ "consigneeId").write[Option[String]] and
        (__ \ "submittedToCore").write[Boolean] and
        (__ \ "messageId").write[String] and
        (__ \ "correlationId").write[Option[String]] and
        (__ \ "userDetails").write[UserDetails] and
        (__ \ "authExciseNumber").write[NonEmptySeq[String]](commaWriter) and
        (__ \ "messageDetails").write[JsObject]
    )(unlift(MessageSubmittedDetails.unapply))
}
