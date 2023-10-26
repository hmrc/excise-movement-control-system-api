package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}

import scala.concurrent.ExecutionContext

class GetMovementsControllerSpec extends PlaySpec {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()

  "getMovements" should {
    "return 200 when successful" in {
      val controller = new GetMovementsController(cc)

      val result = controller.getMovements

      status(result) mustBe OK

      val expectedJson = Json.parse(
        """[
          |  {
          |    "consignorId": "MRlY1BIyYA1nd",
          |    "localReferenceNumber": "1v$%wqd",
          |    "consigneeId": "dUC\"v",
          |    "administrativeReferenceCode": "73CUHCY7XA4BIDE35CBR6",
          |    "status": "Accepted"
          |  }
          |]""".stripMargin
      )

      contentAsJson(result) mustBe expectedJson
    }
  }

}
