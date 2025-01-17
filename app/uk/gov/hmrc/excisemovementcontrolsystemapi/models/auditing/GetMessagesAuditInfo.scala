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

import java.time.Instant

case class GetMessagesRequestAuditInfo(
  movementId: String,
  updatedSince: Option[String],
  traderType: Option[String]
)

object GetMessagesRequestAuditInfo {


}

case class GetMessagesResponseAuditInfo(
  numberOfMessages: Int,
  message: Seq[MessageAuditInfo],
  localReferenceNumber: String,
  administrativeReferenceCode: Option[String],
  consignorId: String,
  consigneeId: Option[String] // TODO: Discuss with Kara a change to Option
)

object GetMessagesResponseAuditInfo {}

case class MessageAuditInfo(
  messageId: String,
  correlationId: Option[String], // TODO: Discuss with Kara a change to Option
  messageTypeCode: String,
  messageType: String,
  recipient: String,
  createdOn: Instant
)

object MessageAuditInfo {}

case class GetMessagesAuditInfo(
  requestType: String = "MovementMessages",
  request: GetMessagesRequestAuditInfo,
  response: GetMessagesResponseAuditInfo,
  userDetails: UserDetails,
  authExciseNumber: NonEmptySeq[String]
)

object GetMessagesAuditInfo {}
