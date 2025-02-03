package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, Json}

import java.util.UUID

class MovementSavedAuditInfoSpec extends PlaySpec {

  "MovementSavedSuccessAuditInfo.writes" should {
    "serialise jobId as null when None is provided" in {

      val batchId                           = UUID.randomUUID().toString
      val messageProcessingSuccessAuditInfo =
        MovementSavedSuccessAuditInfo()

      val expectedResult = Json.obj(
        "exciseRegistrationNumber" -> "ern",
        "messagesAvailable"        -> 10,
        "messagesInBatch"          -> 10,
        "messages"                 -> JsArray.empty,
        "processingStatus"         -> "Success",
        "batchId"                  -> batchId,
        "jobId"                    -> JsNull
      )

      val output = Json.toJson(messageProcessingSuccessAuditInfo)

      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull
    }
  }

  "MovementSavedFailureAuditInfo.writes" should {
    "serialise jobId as null when None is provided" in {}
  }
}
