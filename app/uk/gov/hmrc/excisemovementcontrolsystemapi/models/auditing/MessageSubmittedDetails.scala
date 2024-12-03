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
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.CommonFormats._

trait EventAuditEvent {} //Should this be here vs using the abstract class?

case class UserDetails(
  gatewayId: String,
  name: String,
  email: String,
  affinityGroup: String,
  credentialRole: String
) {}

object UserDetails {
  implicit val format = Json.format[UserDetails]
}

case class ThirdPartyApplication(
  id: String,
  clientId: String,
  gatewayId: String,
  nameResponsibleIndividual: String,
  emailResponsibleIndividual: String,
  organisationUrl: Option[String]
) {}

object ThirdPartyApplication {
  implicit val format = Json.format[ThirdPartyApplication]
}

case class MessageSubmittedDetails(
  messageTypeCode: String,
  messageType: String,
  localReferenceNumber: String,
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
) extends EventAuditEvent

object MessageSubmittedDetails {
  implicit val format = Json.format[MessageSubmittedDetails]
}
