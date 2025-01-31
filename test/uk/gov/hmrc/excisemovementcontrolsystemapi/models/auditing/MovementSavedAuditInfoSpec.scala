package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import org.scalatestplus.play.PlaySpec

class MovementSavedAuditInfoSpec extends PlaySpec {

  "MovementSavedSuccessAuditInfo.writes" should {
    "serialise jobId as null when None is provided" in {}
  }

  "MovementSavedFailureAuditInfo.writes" should {
    "serialise jobId as null when None is provided" in {}
  }
}
