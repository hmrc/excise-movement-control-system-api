package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

abstract class EventAuditType(val name: String)

object EventAuditType {

  val values: Seq[EventAuditType] = Seq(
    MessageSubmitted
  )

  case object MessageSubmitted extends EventAuditType("MessageSubmitted")
}
