package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import org.apache.pekko.Done
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.{NrsCircuitBreaker, UnexpectedResponseException}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NrsMetadata, NrsPayload}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.ZonedDateTime
import scala.concurrent.duration.DurationInt

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

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.nrs.port" -> wireMockPort,
        "microservice.services.nrs.api-key" -> "some-bearer"
      )
      .build()

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

    "must return a Future.successful" - {
      "when the call to NRS succeeds" in {
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
    }
    "must return a failed future" - {
      "and trip the circuit breaker" - {

//        val connector = app.injector.instanceOf[SdesConnector]
//        val circuitBreaker = app.injector.instanceOf[SdesCircuitBreaker].breaker
//
//        circuitBreaker.resetTimeout mustEqual 1.second
//
//        wireMockServer.stubFor(
//          post(urlMatching(url))
//            .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
//            .withHeader("x-client-id", equalTo("client-id"))
//            .willReturn(aResponse().withBody("body").withStatus(INTERNAL_SERVER_ERROR))
//        )
//
//        val onOpen = Promise[Unit]
//        circuitBreaker.onOpen(onOpen.success(System.currentTimeMillis()))
//
//        circuitBreaker.isOpen mustBe false
//        connector.notify(request)(using hc).failed.futureValue
//        onOpen.future.futureValue
//        circuitBreaker.isOpen mustBe true
//        connector.notify(request)(using hc).failed.futureValue
//
//        wireMockServer.verify(1, postRequestedFor(urlMatching(url)))

        "when the call to NRS fails with a 5xx error" in {
          val circuitBreaker = app.injector.instanceOf[NrsCircuitBreaker].breaker

          circuitBreaker.resetTimeout mustEqual 1.second
          
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
      "and NOT trip the circuit breaker" - {
        "when the call to NRS fails with a 4xx error" in {
          wireMockServer.stubFor(
            post(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withBody("body")
                  .withStatus(BAD_REQUEST)
              )
          )

          val exception = connector.sendToNrs(nrsPayLoad, correlationId)(hc).failed.futureValue

          exception mustBe UnexpectedResponseException(BAD_REQUEST, "body")
        }
      }
    }
  }
}
