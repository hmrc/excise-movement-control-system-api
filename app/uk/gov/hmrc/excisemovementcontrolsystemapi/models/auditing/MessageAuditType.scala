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

abstract class MessageAuditType(val name: String)

object MessageAuditType {

  val values: Seq[MessageAuditType] = Seq(
    CancelMovement,
    ChangeOfDestination,
    DraftMovement,
    ReportOfReceipt,
    AlertRejection,
    Delay,
    ShortageOrExcess,
    Refused,
    MovementGenerated,
    Reminder,
    NotificationOfDivertedMovement,
    InterruptionOfMovement,
    NotificationOfAcceptedExport,
    RefusalByCustoms,
    EventReport,
    ManualClosure,
    StatusResponse
  )

  case object CancelMovement extends MessageAuditType("CancelMovement")
  case object ChangeOfDestination extends MessageAuditType("ChangeOfDestination")
  case object DraftMovement extends MessageAuditType("DraftMovement")
  case object ReportOfReceipt extends MessageAuditType("ReportOfReceipt")
  case object AlertRejection extends MessageAuditType("AlertRejection")
  case object Delay extends MessageAuditType("Delay")
  case object ShortageOrExcess extends MessageAuditType("ShortageOrExcess")
  case object Refused extends MessageAuditType("Refused")
  case object MovementGenerated extends MessageAuditType("MovementGenerated")
  case object Reminder extends MessageAuditType("Reminder")
  case object NotificationOfDivertedMovement extends MessageAuditType("NotificationOfDivertedMovement")
  case object InterruptionOfMovement extends MessageAuditType("InterruptionOfMovement")
  case object NotificationOfAcceptedExport extends MessageAuditType("NotificationOfAcceptedExport")
  case object RefusalByCustoms extends MessageAuditType("RefusalByCustoms")
  case object EventReport extends MessageAuditType("EventReport")
  case object ManualClosure extends MessageAuditType("ManualClosure")
  case object StatusResponse extends MessageAuditType("StatusResponse")

}
