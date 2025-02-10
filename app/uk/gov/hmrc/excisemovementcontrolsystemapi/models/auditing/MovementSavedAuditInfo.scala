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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

case class KeyMessageDetailsAuditInfo(
  messageId: String,
  correlationId: Option[String],
  messageTypeCode: String,
  messageType: String
) {}

object KeyMessageDetailsAuditInfo {
  implicit val writes = Json.writes[KeyMessageDetailsAuditInfo]
}

case class MovementSavedSuccessAuditInfo(
  saveStatus: String = "Success",
  messagesAdded: Int,
  totalMessages: Int,
  movementId: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: Option[String],
  batchId: Option[String],
  jobId: Option[String],
  keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
  fullMessageDetails: Seq[JsValue]
) {}

object MovementSavedSuccessAuditInfo {
  def apply(
    messagesAdded: Int,
    totalMessages: Int,
    movementId: String,
    localReferenceNumber: Option[String],
    administrativeReferenceCode: Option[String],
    consignorId: String,
    consigneeId: Option[String],
    batchId: Option[String],
    jobId: Option[String],
    keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
    fullMessageDetails: Seq[JsValue]
  ): MovementSavedSuccessAuditInfo =
    MovementSavedSuccessAuditInfo(
      "Success",
      messagesAdded,
      totalMessages,
      movementId,
      localReferenceNumber,
      administrativeReferenceCode,
      consignorId,
      consigneeId,
      batchId,
      jobId,
      keyMessageDetails,
      fullMessageDetails
    )

  implicit val write: OWrites[MovementSavedSuccessAuditInfo] =
    (
      (JsPath \ "saveStatus").write[String] and
        (JsPath \ "messagesAdded").write[Int] and
        (JsPath \ "totalMessages").write[Int] and
        (JsPath \ "movementId").write[String] and
        (JsPath \ "localReferenceNumber").write[Option[String]] and
        (JsPath \ "administrativeReferenceCode").write[Option[String]] and
        (JsPath \ "consignorId").write[String] and
        (JsPath \ "consigneeId").write[Option[String]] and
        (JsPath \ "batchId").write[Option[String]] and
        (JsPath \ "jobId").write[Option[String]] and
        (JsPath \ "keyMessageDetails").write[Seq[KeyMessageDetailsAuditInfo]] and
        (JsPath \ "fullMessageDetails").write[Seq[JsValue]]
    )(unlift(MovementSavedSuccessAuditInfo.unapply))
}

case class MovementSavedFailureAuditInfo(
  saveStatus: String = "Failure",
  failureReason: String,
  messagesToBeAdded: Int,
  totalMessages: Int,
  movementId: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: Option[String],
  batchId: Option[String],
  jobId: Option[String],
  keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
  fullMessageDetails: Seq[JsValue]
) {}

object MovementSavedFailureAuditInfo {
  def apply(
    failureReason: String,
    messagesToBeAdded: Int,
    totalMessages: Int,
    movementId: String,
    localReferenceNumber: Option[String],
    administrativeReferenceCode: Option[String],
    consignorId: String,
    consigneeId: Option[String],
    batchId: Option[String],
    jobId: Option[String],
    keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
    fullMessageDetails: Seq[JsValue]
  ): MovementSavedFailureAuditInfo =
    MovementSavedFailureAuditInfo(
      "Failure",
      failureReason,
      messagesToBeAdded,
      totalMessages,
      movementId,
      localReferenceNumber,
      administrativeReferenceCode,
      consignorId,
      consigneeId,
      batchId,
      jobId,
      keyMessageDetails,
      fullMessageDetails
    )

  implicit val write: OWrites[MovementSavedFailureAuditInfo] =
    ((JsPath \ "saveStatus").write[String] and
      (JsPath \ "failureReason").write[String] and
      (JsPath \ "messagesToBeAdded").write[Int] and
      (JsPath \ "totalMessages").write[Int] and
      (JsPath \ "movementId").write[String] and
      (JsPath \ "localReferenceNumber").write[Option[String]] and
      (JsPath \ "administrativeReferenceCode").write[Option[String]] and
      (JsPath \ "consignorId").write[String] and
      (JsPath \ "consigneeId").write[Option[String]] and
      (JsPath \ "batchId").write[Option[String]] and
      (JsPath \ "jobId").write[Option[String]] and
      (JsPath \ "keyMessageDetails").write[Seq[KeyMessageDetailsAuditInfo]] and
      (JsPath \ "fullMessageDetails").write[Seq[JsValue]])(unlift(MovementSavedFailureAuditInfo.unapply))
}
