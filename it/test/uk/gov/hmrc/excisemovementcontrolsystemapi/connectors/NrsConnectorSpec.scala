package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.Done
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.{NrsCircuitBreaker, UnexpectedResponseException}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NrsMetadata, NrsPayload}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.ZonedDateTime
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt

class NrsConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerTest
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with NrsTestData {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.nrs.port" -> wireMockPort,
        "microservice.services.nrs.api-key" -> "some-bearer",
        "nrs.max-failures" -> 1,
        "nrs.reset-timeout" -> "1 second",
        "nrs.call-timeout" -> "30 seconds"
      )
      .build()

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
        val connector = app.injector.instanceOf[NrsConnector]
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
      "when the call to NRS fails with a 4xx error" - {
        "with circuit breaker present" in {
          val connector = app.injector.instanceOf[NrsConnector]
          wireMockServer.stubFor(
            post(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withBody("body")
                  .withStatus(BAD_REQUEST)
              )
          )

          val result = connector.sendToNrs(nrsPayLoad, correlationId)(hc).futureValue

          result mustBe Done
        }
        "and NOT trip the circuit breaker" in {
          val connector = app.injector.instanceOf[NrsConnector]
          val circuitBreaker = app.injector.instanceOf[NrsCircuitBreaker].breaker

          circuitBreaker.resetTimeout mustEqual 1.second

          wireMockServer.stubFor(
            post(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withBody("body")
                  .withStatus(BAD_REQUEST)
              )
          )

          circuitBreaker.isOpen mustBe false
          connector.sendToNrs(nrsPayLoad, correlationId)(hc).futureValue
          circuitBreaker.isOpen mustBe false
          connector.sendToNrs(nrsPayLoad, correlationId)(hc).futureValue

          wireMockServer.verify(2, postRequestedFor(urlMatching(url)))
        }
      }
    }
    "must return a failed future" - {
      "when the call to NRS fails with a 5xx error" - {
        "with circuit breaker present" in {
          val connector = app.injector.instanceOf[NrsConnector]
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
        "and trip the circuit breaker to fail fast" in {
          val connector = app.injector.instanceOf[NrsConnector]
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

          val onOpen = Promise[Unit]
          circuitBreaker.onOpen(onOpen.success(()))

          circuitBreaker.isOpen mustBe false
          connector.sendToNrs(nrsPayLoad, correlationId)(hc).failed.futureValue
          onOpen.future.futureValue
          circuitBreaker.isOpen mustBe true
          connector.sendToNrs(nrsPayLoad, correlationId)(hc).failed.futureValue

          wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
        }
      }
    }
  }
}
