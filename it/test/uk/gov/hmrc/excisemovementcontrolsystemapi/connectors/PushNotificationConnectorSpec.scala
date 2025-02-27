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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{FailedPushNotification, SuccessBoxNotificationResponse, SuccessPushNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.http.HeaderCarrierConverter

class PushNotificationConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerTest
    with MockitoSugar
    with ScalaFutures
    with TestXml
    with IntegrationPatience {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("microservice.services.push-pull-notifications.port" -> wireMockPort)
      .build()

  "getDefaultBoxId" - {
    val response = SuccessBoxNotificationResponse("boxId")

    "forward the correlation id if it exists" in {
      val correlationId = "abcdefg"
      val hc            = HeaderCarrierConverter.fromRequest(FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correlationId))
      val ern           = "ern"
      val url           = "/box?boxName=customs/excise%23%231.0%23%23notificationUrl&clientId=clientId"
      val connector     = app.injector.instanceOf[PushNotificationConnector]

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //If this succeeds the wiremock has acted as a matcher
      connector.getDefaultBoxId("clientId")(hc).futureValue.isRight mustBe true
    }
    "forward a new correlation id if not exists" in {
      val correlationId = "abcdefg"
      val ern           = "ern"
      val url           = "/box?boxName=customs/excise%23%231.0%23%23notificationUrl&clientId=clientId"
      val connector     = app.injector.instanceOf[PushNotificationConnector]
      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //This should fail as the headers don't match
      val result = connector.getDefaultBoxId("clientId")(HeaderCarrier()).futureValue.isLeft mustBe true

      wireMockServer.stubFor(
        get(urlEqualTo(url))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //This should succeed as the headers now don't match correlationId
      val result2 = connector.getDefaultBoxId("clientId")(HeaderCarrier()).futureValue.isRight mustBe true

    }

  }

  "postNotification" - {
    val response     = SuccessPushNotificationResponse("notificationId")
    val notification = Notification("mid", "muri", "mid2", "mtype", "ern", None, None, "ern")
    "forward the correlation id if it exists" in {
      val correlationId = "abcdefg"
      val hc            = HeaderCarrierConverter.fromRequest(FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correlationId))
      val url           = "/box/boxId/notifications"
      val connector     = app.injector.instanceOf[PushNotificationConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //If this succeeds the wiremock has acted as a matcher
      connector.postNotification("boxId", notification)(hc).futureValue
    }

    "forward a new correlation id if not exists" in {
      val correlationId = "abcdefg"
      val url           = "/box/boxId/notifications"
      val connector     = app.injector.instanceOf[PushNotificationConnector]
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //This should fail as the headers don't match
      val result =
        connector.postNotification("boxId", notification)(HeaderCarrier()).futureValue mustBe a[FailedPushNotification]

      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader("X-Correlation-Id", notMatching(correlationId))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(response).toString())
              .withStatus(OK)
          )
      )

      //This should succeed as the headers now don't match correlationId
      val result2 = connector
        .postNotification("boxId", notification)(HeaderCarrier())
        .futureValue mustBe a[SuccessPushNotificationResponse]

    }

  }
}
