package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptySeq
import play.api.libs.json.JsObject

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
)

case class UserDetails(
  gatewayId: String,
  name: String,
  email: String,
  affinityGroup: String,
  credentialRole: String
) {}

case class ThirdPartyApplication(
  id: String,
  clientId: String,
  gatewayId: String,
  nameResponsibleIndividual: String,
  emailResponsibleIndividual: String,
  organisationUrl: Option[String]
) {}
