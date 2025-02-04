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
import play.api.libs.json.{JsNull, JsObject, JsPath, JsString, JsValue, Json, OWrites}

case class MessageAcknowledgedSuccessAuditInfo(
  acknowledgementStatus: String,
  batchId: String,
  jobId: Option[String],
  exciseRegistrationNumber: String,
  recordsAffected: Int
)

object MessageAcknowledgedSuccessAuditInfo {

  def apply(batchId: String, jobId: Option[String], exciseRegistrationNumber: String, recordsAffected: Int) =
    new MessageAcknowledgedSuccessAuditInfo("Success", batchId, jobId, exciseRegistrationNumber, recordsAffected)

  implicit val writes: OWrites[MessageAcknowledgedSuccessAuditInfo] =
    (
      (JsPath \ "acknowledgementStatus").write[String] and
        (JsPath \ "batchId").write[String] and
        (JsPath \ "jobId").write[Option[String]] and
        (JsPath \ "exciseRegistrationNumber").write[String] and
        (JsPath \ "recordsAffected").write[Int]
    )(unlift(MessageAcknowledgedSuccessAuditInfo.unapply))

}

case class MessageAcknowledgedFailureAuditInfo(
  acknowledgementStatus: String,
  failureReason: String,
  batchId: String,
  jobId: Option[String],
  exciseRegistrationNumber: String
)

object MessageAcknowledgedFailureAuditInfo {

  def apply(failureReason: String, batchId: String, jobId: Option[String], exciseRegistrationNumber: String) =
    new MessageAcknowledgedFailureAuditInfo("Failure", failureReason, batchId, jobId, exciseRegistrationNumber)

  implicit val writes: OWrites[MessageAcknowledgedFailureAuditInfo] =
    (
      (JsPath \ "acknowledgementStatus").write[String] and
        (JsPath \ "failureReason").write[String] and
        (JsPath \ "batchId").write[String] and
        (JsPath \ "jobId").write[Option[String]] and
        (JsPath \ "exciseRegistrationNumber").write[String]
    )(unlift(MessageAcknowledgedFailureAuditInfo.unapply))
}
