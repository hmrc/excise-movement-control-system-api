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

 abstract class AuditType(val name: String)

//TODO: Do we need this?
object AuditType {

  val values: Seq[AuditType] = Seq(
    CancelMovement,
    ChangeOfDestination,
    DraftMovement,
    ReportOfReceipt,
    AlertRejection,
    Delay,
    ShortageOrExcess
  )

  case object CancelMovement                  extends AuditType("CancelMovement")
  case object ChangeOfDestination             extends AuditType("ChangeOfDestination")
  case object DraftMovement                   extends AuditType("DraftMovement")
  case object ReportOfReceipt                 extends AuditType("ReportOfReceipt")
  case object AlertRejection                  extends AuditType("AlertRejection")
  case object Delay                           extends AuditType("Delay")
  case object ShortageOrExcess                extends AuditType("ShortageOrExcess")
  case object Refused                         extends AuditType("Refused")
  case object MovementGenerated               extends AuditType("MovementGenerated")
  case object Reminder                        extends AuditType("Reminder")
  case object NotificationOfDivertedMovement  extends AuditType("NotificationOfDivertedMovement")
  case object InterruptionOfMovement          extends AuditType("InterruptionOfMovement")
  case object NotificationOfAcceptedExport    extends AuditType("NotificationOfAcceptedExport")
  case object RefusalByCustoms                extends AuditType("RefusalByCustoms")
  case object EventReport                     extends AuditType("EventReport")
  case object ManualClosure                   extends AuditType("ManualClosure")
  case object StatusResponse                  extends AuditType("StatusResponse")

}
