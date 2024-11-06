/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, postRequestedFor, urlEqualTo, urlMatching}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialStrength, User}
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ItmpAddress, ItmpName, MdtpInformation, Name}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.NrsCircuitBreaker
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{IdentityData, NonRepudiationSubmissionAccepted, NonRepudiationSubmissionFailed, NrsMetadata, NrsPayload}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.lang.String.format
import java.math.BigInteger
import java.security.MessageDigest.getInstance
import java.time.{LocalDate, ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.Promise
import scala.concurrent.duration._

class NrsConnectorItSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerSuite
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach
    with NewMessagesXml {

  override lazy val app: Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.nrs.port"          -> wireMockPort,
        "microservice.services.nrs.max-failures"  -> 10,
        "microservice.services.nrs.call-timeout"  -> "30 seconds",
        "microservice.services.nrs.reset-timeout" -> "1 seconds",
        "microservice.services.nrs.api-key"       -> "api-key"
      )
      .build()
  private lazy val connector         = app.injector.instanceOf[NrsConnector]

  private val timeStamp                    = ZonedDateTime.now()
  val testAffinityGroup: AffinityGroup     = AffinityGroup.Organisation
  val testProviderId: String               = "testProviderID"
  val testProviderType: String             = "GovernmentGateway"
  val testCredentials: Credentials         = Credentials(testProviderId, testProviderType)
  val testInternalid                       = "INT-123-456-789"
  val testExternalId                       = "testExternalId"
  val testAgentCode                        = "testAgentCode"
  val testConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L200
  val testSautr                            = "testSautr"
  val testNino                             = "NB686868C"
  val testDate: LocalDate                  = LocalDate.of(2017, 1, 1)
  val testDateTime: ZonedDateTime          = ZonedDateTime.of(2022, 7, 1, 12, 34, 37, 0, ZoneId.of("Z"))
  val testDateTimeString: String           = "2022-07-01T12:34:37Z" // <-- this is an actual date string taken from production
  val testAuthName: Name                   =
    uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))

  val testEmail: String                    = "testEmail"
  val testErn: String                      = "GB123456789"
  val testAuthToken: String                = "testAuthToken"
  val testUserHeaders: Map[String, String] = Map("testKey" -> "testValue")
  val testSearchKeys: Map[String, String]  = Map("ern" -> testErn)

  val testAgentInformation: AgentInformation =
    AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))
  val testGroupIdentifier                    = "testGroupIdentifier"
  val testCredentialRole: User.type          = User
  val testMdtpInformation: MdtpInformation   = MdtpInformation("testDeviceId", "testSessionId")
  val testCredentialStrength: String         = CredentialStrength.strong
  val testItmpName: ItmpName                 =
    ItmpName(Some("testGivenName"), Some("testMiddleName"), Some("testFamilyName"))

  val testItmpAddress: ItmpAddress      =
    ItmpAddress(Some("testLine1"), None, None, None, None, Some("testPostcode"), None, None)
  val testNrsIdentityData: IdentityData = IdentityData(
    Some(testInternalid),
    Some(testExternalId),
    Some(testAgentCode),
    Some(testCredentials),
    testConfidenceLevel,
    Some(testNino),
    Some(testSautr),
    Some(testAuthName),
    Some(testEmail),
    testAgentInformation,
    Some(testGroupIdentifier),
    Some(testCredentialRole),
    Some(testMdtpInformation),
    Some(testItmpName),
    Some(testItmpAddress),
    Some(testAffinityGroup),
    Some(testCredentialStrength)
  )
  private val nrsMetadata               = NrsMetadata(
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
  "sendToNrs" - {
    val url           = "/submission"
    val correlationId = UUID.randomUUID().toString
    val hc            = HeaderCarrier()
    val request       = NrsPayload("encodepayload", nrsMetadata)

    "returns successful submission" in {
      val response = NonRepudiationSubmissionAccepted("submissionId")

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader(NrsConnector.XApiKeyHeaderKey, equalTo("api-key"))
          .willReturn(
            aResponse()
              .withBody(Json.stringify(Json.toJson(response)))
              .withStatus(ACCEPTED)
          )
      )
      val result = connector.sendToNrs(request, correlationId)(hc).futureValue

      result mustBe response
    }

    "returns failed submission for 5xx code" in {
      val response = NonRepudiationSubmissionFailed(INTERNAL_SERVER_ERROR, "error")

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader(NrsConnector.XApiKeyHeaderKey, equalTo("api-key"))
          .willReturn(
            aResponse()
              .withBody("error")
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      val result = connector.sendToNrs(request, correlationId)(hc).futureValue

      result mustBe response
    }

    "returns failed submission for any other unsupported code" in {
      val response = NonRepudiationSubmissionFailed(BAD_REQUEST, "error")

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader(NrsConnector.XApiKeyHeaderKey, equalTo("api-key"))
          .willReturn(
            aResponse()
              .withBody("error")
              .withStatus(BAD_REQUEST)
          )
      )
      val result = connector.sendToNrs(request, correlationId)(hc).futureValue

      result mustBe response
    }

    "must fail fast when the circuit breaker is open" in {
      val circuitBreaker = app.injector.instanceOf[NrsCircuitBreaker].breaker
      circuitBreaker.resetTimeout mustEqual 1.second

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader(NrsConnector.XApiKeyHeaderKey, equalTo("api-key"))
          .willReturn(
            aResponse()
              .withBody("error")
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val onOpen = Promise[Unit]
      circuitBreaker.onOpen(onOpen.success(System.currentTimeMillis()))

      circuitBreaker.isOpen mustBe false
      connector.sendToNrs(request, correlationId)(hc).failed.futureValue
      onOpen.future.futureValue
      circuitBreaker.isOpen mustBe true
      connector.sendToNrs(request, correlationId)(hc).failed.futureValue

      wireMockServer.verify(1, postRequestedFor(urlMatching(url)))
    }
  }

  def sha256Hash(text: String): String =
    format(
      "%064x",
      new BigInteger(
        1,
        getInstance("SHA-256")
          .digest(text.getBytes("UTF-8"))
      )
    )
}
