package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class GetMovementsDetailsSpec extends PlaySpec {

  "GetMovementsRequest.writes" should {
    "create an json only with fields that are populated" in {
      val request = GetMovementsRequest(Some("ern"), None, None, None, None)

      val json = Json.toJson(request)

      val expectedJson = Json.obj("exciseRegistrationNumber" -> "ern")

      json mustBe expectedJson
    }
  }
}
