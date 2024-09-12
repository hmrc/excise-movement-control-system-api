package uk.gov.hmrc.excisemovementcontrolsystemapi

import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.ACCEPTED
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{ErrorResponseSupport, StringSupport}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures._

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class SubscribeErnsControllerItSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with ApplicationBuilderSupport
    with TestXml
    with WireMockServerSpec
    with SubmitMessageTestSupport
    with StringSupport
    with ErrorResponseSupport
    with Eventually
    with IntegrationPatience
    with BeforeAndAfterEach {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val ern                             = "ern"
  private val url                     = s"http://localhost:$port/$ern/subscription"

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    applicationBuilder(configureEisService).build()
  }

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val consignorId = "GBWK002281023"

  "subscribeErn" should {
    "return Accepted when correct ern is passed and feature flag is on" in {
      withAuthorizedTrader(consignorId)

      val result = postRequest(ern)

      result.status mustBe ACCEPTED

    }
  }

  private def postRequest(ern: String, contentType: String = """application/vnd.hmrc.1.0+xml""") =
    await(
      wsClient
        .url(url)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN",
          HeaderNames.CONTENT_TYPE  -> contentType,
          "X-Client-Id"             -> "clientId"
        )
        .post(ern)
    )

}
