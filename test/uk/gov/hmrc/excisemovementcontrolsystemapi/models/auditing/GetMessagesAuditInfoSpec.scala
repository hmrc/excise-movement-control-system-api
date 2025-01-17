package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class GetMessagesAuditInfoSpec extends PlaySpec {

  "GetMessagesRequestAuditInfo.writes" should {
    "write json when updatedSince and traderType are present" in {
      val getMessagesRequestAuditInfo = GetMessagesRequestAuditInfo(
        "movementId",
        Some("timestamp"),
        Some("consignor")
      )

      val json = Json.toJson(getMessagesRequestAuditInfo)

      val expectedJson = Json.obj(
        "movementId" -> "movementId",
        "updatedSince" -> "timestamp",
        "traderType" -> "consignor"
      )

      json mustBe expectedJson
    }

    "write json when updatedSince and traderType are not present" in {
      val getMessagesRequestAuditInfo = GetMessagesRequestAuditInfo(
        "movementId",
        None,
        None
      )

      val json = Json.toJson(getMessagesRequestAuditInfo)

      val expectedJson = Json.obj(
        "movementId" -> "movementId"
      )

      json mustBe expectedJson
    }
  }

  "GetMessagesRequestAuditInfo.writes" should {
    "write json when updatedSince and traderType are present" in {
      val getMessagesRequestAuditInfo = GetMessagesRequestAuditInfo(
        "movementId",
        Some("timestamp"),
        Some("consignor")
      )

      val json = Json.toJson(getMessagesRequestAuditInfo)

      val expectedJson = Json.obj(
        "movementId" -> "movementId",
        "updatedSince" -> "timestamp",
        "traderType" -> "consignor"
      )

      json mustBe expectedJson
    }

    "write json when updatedSince and traderType are not present" in {
      val getMessagesRequestAuditInfo = GetMessagesRequestAuditInfo(
        "movementId",
        None,
        None
      )

      val json = Json.toJson(getMessagesRequestAuditInfo)

      val expectedJson = Json.obj(
        "movementId" -> "movementId"
      )

      json mustBe expectedJson
    }
  }
}