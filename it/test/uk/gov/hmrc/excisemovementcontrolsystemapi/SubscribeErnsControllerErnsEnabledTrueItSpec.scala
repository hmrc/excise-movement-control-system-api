/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{ACCEPTED, FORBIDDEN}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{ErrorResponseSupport, StringSupport}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.SuccessBoxNotificationResponse

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class SubscribeErnsControllerErnsEnabledTrueItSpec
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
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  val ern                   = "GBWK002281023"
  private val url           = s"http://localhost:$port/erns/$ern/subscription"
  private val timestamp     = Instant.parse("2024-05-06T15:30:15.12345612Z")
  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    applicationBuilder(configureServicesLocally()).build()
  }

  private def configureServicesLocally(): Map[String, Any] =
    configureServices ++ Map("featureFlags.subscribeErnsEnabled" -> true)

  protected implicit val ec: ExecutionContext              = ExecutionContext.Implicits.global

  private val consignorId = "GBWK002281023"

  "subscribeErn" should {
    "return Accepted when correct ern is passed and feature flag is on" in {
      withAuthorizedTrader(consignorId)
      setupRepositories()
      stubNotificationResponse(UUID.randomUUID().toString)
      val result = postRequestSubscribe(consignorId)

      result.status mustBe ACCEPTED

    }

    "return Forbidden when ern passed in doesn't match ern from request" in {
      withAuthorizedTrader()
      setupRepositories()
      stubNotificationResponse(UUID.randomUUID().toString)
      val result = postRequestSubscribe("123")

      result.status mustBe FORBIDDEN

    }
  }

  "unsubscribeErn" should {
    "return Accepted when correct ern is passed and feature flag is on" in {
      withAuthorizedTrader(consignorId)
      setupRepositories()
      stubNotificationResponse(UUID.randomUUID().toString)
      val result = postRequestUnsubscribe()

      result.status mustBe ACCEPTED
    }
  }

  private def setupRepositories() = {
    when(boxIdRepository.save(any(), any())).thenReturn(Future.successful(Done))
    when(movementRepository.addBoxIdToMessages(any(), any())).thenReturn(Future.successful(Done))
    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(boxIdRepository.delete(any(), any())).thenReturn(Future.successful(Done))
    when(movementRepository.removeBoxIdFromMessages(any(), any())).thenReturn(Future.successful(Done))
  }
  private def postRequestSubscribe(ern: String) =
    await(
      client
        .url(url)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN",
          "X-Client-Id"             -> "clientId"
        )
        .post(ern)
    )

  private def postRequestUnsubscribe() =
    await(
      client
        .url(url)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN",
          "X-Client-Id"             -> "clientId"
        )
        .delete()
    )

  private def stubNotificationResponse(boxId: String): StubMapping =
    wireMock.stubFor(
      get(urlPathEqualTo(s"""/box"""))
        .withQueryParam("boxName", equalTo(Constants.BoxName))
        .withQueryParam("clientId", equalTo("clientId"))
        .willReturn(
          aResponse()
            .withStatus(ACCEPTED)
            .withBody(Json.toJson(SuccessBoxNotificationResponse(boxId)).toString())
        )
    )
}
