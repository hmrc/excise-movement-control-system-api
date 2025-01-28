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
import play.api.libs.json.{JsPath, Json, OWrites}

case class MessageProcessingMessageAuditInfo(
  messageId: String,
  correlationId: Option[String],
  messageTypeCode: String,
  messageType: String,
  localReferenceNumber: Option[String],
  administrativeReferenceCode: Option[String]
)
object MessageProcessingMessageAuditInfo {
  implicit val writes = Json.writes[MessageProcessingMessageAuditInfo]

}

case class MessageProcessingSuccessAuditInfo(
  exciseRegistrationNumber: String,
  messagesAvailable: Int,
  messagesInBatch: Int,
  messages: Seq[MessageProcessingMessageAuditInfo],
  processingStatus: String = "Success",
  batchId: String,
  jobId: Option[String]
)

object MessageProcessingSuccessAuditInfo {

  def apply(
    exciseRegistrationNumber: String,
    messagesAvailable: Int,
    messagesInBatch: Int,
    messages: Seq[MessageProcessingMessageAuditInfo],
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingSuccessAuditInfo =
    MessageProcessingSuccessAuditInfo(
      exciseRegistrationNumber,
      messagesAvailable,
      messagesInBatch,
      messages,
      "Success",
      batchId,
      jobId
    )

  implicit val write: OWrites[MessageProcessingSuccessAuditInfo] =
    (
      (JsPath \ "exciseRegistrationNumber").write[String] and
        (JsPath \ "messagesAvailable").write[Int] and
        (JsPath \ "messagesInBatch").write[Int] and
        (JsPath \ "messages").write[Seq[MessageProcessingMessageAuditInfo]] and
        (JsPath \ "processingStatus").write[String] and
        (JsPath \ "batchId").write[String] and
        (JsPath \ "jobId").write[Option[String]]
    )(unlift(MessageProcessingSuccessAuditInfo.unapply))
}

case class MessageProcessingFailureAuditInfo(
  exciseRegistrationNumber: String,
 processingStatus: String = "Failure",
  failureReason: String,
  batchId: String,
  jobId: Option[String]
)

object MessageProcessingFailureAuditInfo {

  def apply(
    exciseRegistrationNumber: String,
    failureReason: String,
    batchId: String,
    jobId: Option[String]
  ): MessageProcessingFailureAuditInfo                           =
    MessageProcessingFailureAuditInfo(
      exciseRegistrationNumber,
      "Failure",
      failureReason,
      batchId,
      jobId
    )
  implicit val write: OWrites[MessageProcessingFailureAuditInfo] =
    (
      (JsPath \ "exciseRegistrationNumber").write[String] and
        (JsPath \ "processingStatus").write[String] and
        (JsPath \ "failureReason").write[String] and
        (JsPath \ "batchId").write[String] and
        (JsPath \ "jobId").write[Option[String]]
    )(unlift(MessageProcessingFailureAuditInfo.unapply))
}
