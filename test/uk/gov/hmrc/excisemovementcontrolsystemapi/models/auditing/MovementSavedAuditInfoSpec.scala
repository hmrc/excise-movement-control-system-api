package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsNull, Json}

import java.util.UUID

class MovementSavedAuditInfoSpec extends PlaySpec {

  "MovementSavedSuccessAuditInfo.writes" should {
    "serialise jobId as null when None is provided" in {

      val batchId                           = UUID.randomUUID().toString
//      messagesAdded: Int,
//      totalMessages: Int,
//      movementId: String,
//      localReferenceNumber: Option[String],
//      administrativeReferenceCode: Option[String],
//      consignorId: String,
//      consigneeId: String,
//      batchId: String,
//      jobId: Option[String],
//      keyMessageDetails: Seq[KeyMessageDetailsAuditInfo],
//      fullMessageDetails: Seq[IEMessage]
      val messageProcessingSuccessAuditInfo =
        MovementSavedSuccessAuditInfo(
          1, 1, "movementId", Some("lrn"), Some("arc"), "consignorId", "consigneeId", "batchId", None,
          Seq.empty,
          Seq.empty
        )

      val expectedResult = Json.obj(
        "messagesAdded" -> 1,
        "totalMessages" -> 1,
        "movementId" -> "movementId",
        "localReferenceNumber" -> "lrn",
        "administrativeReferenceCode" -> "arc",
        "consignorId" -> "consignorId",
        "consigneeId" -> "consigneeId",
        "batchId" -> "batchId",
        "jobId" -> JsNull,
        "keyMessageDetails" -> JsArray.empty,
        "fullMessageDetails" -> JsArray.empty
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
