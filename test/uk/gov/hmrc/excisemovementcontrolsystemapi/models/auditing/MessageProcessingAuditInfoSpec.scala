package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsNull, Json}

import java.util.UUID

class MessageProcessingAuditInfoSpec extends PlaySpec {

  "MessageProcessingSuccessAuditInfo.writes" should {
    "serialize jobId as a null if None provided" in {
      val batchId                           = UUID.randomUUID().toString
      val messageProcessingSuccessAuditInfo =
        MessageProcessingSuccessAuditInfo("ern", 10, 10, Seq.empty, batchId, None)

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

  "MessageProcessingFailureAuditInfo.writes" should {
    "serialize jobId as a null if None provided" in {
      val batchId                           = UUID.randomUUID().toString
      val messageProcessingSuccessAuditInfo =
        MessageProcessingFailureAuditInfo("ern", 10, 10, "Reason", batchId, None)

      val expectedResult = Json.obj(
        "exciseRegistrationNumber" -> "ern",
        "messagesAvailable"        -> 10,
        "messagesInBatch"          -> 10,
        "processingStatus"         -> "Failure",
        "failureReason"            -> "Reason",
        "batchId"                  -> batchId,
        "jobId"                    -> JsNull
      )

      val output = Json.toJson(messageProcessingSuccessAuditInfo)

      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull

    }
  }
}
