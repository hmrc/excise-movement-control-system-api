package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.http.Status.{ACCEPTED, OK}
import play.api.libs.json.Json
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.SubscribeErnsAdminController.SubscribeErnsRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication

import scala.concurrent.ExecutionContext

class SubscribeErnsControllerSpec extends PlaySpec
  with FakeAuthentication  with GuiceOneAppPerSuite with BeforeAndAfterEach  {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "subscribeErns" should {
    "respond with 200" in {

      val request = FakeRequest()
        .withHeaders(FakeHeaders(Seq(
          HeaderNames.CONTENT_TYPE -> "application/xml",
          "X-Client-Id" -> "clientId",
          "X-Callback-Box-Id" -> "clientBoxId"
        )))

      val result = createWithSuccessfulAuth.subscribeErns(request)

      status(result) mustBe OK

    }

    "respond with 403" in {

      val request = FakeRequest()
        .withHeaders(FakeHeaders(Seq(
          HeaderNames.CONTENT_TYPE -> "application/xml",
          "X-Client-Id" -> "clientId",
          "X-Callback-Box-Id" -> "clientBoxId"
        )))

      val result = createWithFailingAuth.subscribeErns(request)

      status(result) mustBe FORBIDDEN
    }
  }

  private def createWithSuccessfulAuth =
    new SubscribeErnsController(
      FakeSuccessAuthentication(Set(ern)),
      stubControllerComponents()
    )

  private def createWithFailingAuth =
    new SubscribeErnsController(
      FakeFailingAuthentication,
      stubControllerComponents()
    )

}
