package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, post, urlEqualTo}
import org.apache.pekko.Done
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.await
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.UnexpectedResponseException
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NrsMetadata, NrsPayload}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.ZonedDateTime
import scala.concurrent.Future

class NrsConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerSuite
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach
    with NewMessagesXml
    with NrsTestData {


  override lazy val app: Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.nrs.port" -> wireMockPort,
        "microservice.services.nrs.api-key" -> "some-bearer"
      )
      .overrides(
      )
      .build()
  }

  private lazy val connector = app.injector.instanceOf[NrsConnector]

  "sendToNrs" - {

    val hc = HeaderCarrier()
    val correlationId = "correlationId"
    val url = "/submission"
    val timeStamp                  = ZonedDateTime.now()
    val nrsMetadata                = NrsMetadata(
      businessId = "emcs",
      notableEvent = "excise-movement-control-system",
      payloadContentType = "application/json",
      payloadSha256Checksum = sha256Hash("payload for NRS"),
      userSubmissionTimestamp = timeStamp.toString,
      identityData = testNrsIdentityData,
      userAuthToken = testAuthToken,
      headerData = Map(),
      searchKeys = Map("ern" -> "123")
    )
    val nrsPayLoad                 = NrsPayload("encodepayload", nrsMetadata)

    "must return Done when the call to NRS succeeds" in {
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
      )

      val result = connector.sendToNrs(nrsPayLoad, correlationId)(hc).futureValue

      result mustBe Done
    }

    "must return a failed future when the call to NRS fails with a 5xx error" in {
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withBody("body")
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val exception = connector.sendToNrs(nrsPayLoad, correlationId)(hc).failed.futureValue

      exception mustBe UnexpectedResponseException(INTERNAL_SERVER_ERROR, "body")
    }
  }
}
