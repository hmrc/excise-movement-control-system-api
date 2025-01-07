package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import cats.data.NonEmptySeq
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, JsValue, Json}

class MessageSubmittedDetailsSpec extends PlaySpec {

  "messageSubmittedDetails" should {
    "serializes the authExciseNumber sequence into a comma-seperated string" in {
      val testModel = MessageSubmittedDetails(
        "typeCode",
        "IE815",
        "lrn",
        Some("arc"),
        Some("movementId"),
        "consignorId",
        Some("consigneeId"),
        true,
        "messageId",
        Some("correlationId"),
        UserDetails("gatewayId", "groupIdentifier"),
        NonEmptySeq("id1", Seq("id2", "id3")),
        Json.obj("test" -> "data")
      )

      val output = Json.toJson(testModel)

      (output \ "authExciseNumber").get mustBe JsString("id1,id2,id3")

    }
  }

}
