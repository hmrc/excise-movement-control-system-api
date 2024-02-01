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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connector

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.SuccessBoxNotificationResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class PushNotificationConnectorSpec
  extends PlaySpec
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global;
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpClient = mock[HttpClient]
  private val appConfig = mock[AppConfig]

  private val sut = new PushNotificationConnector(httpClient, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig)

    when(httpClient.GET[Any](any, any, any)(any,any,any))
      .thenReturn(Future.successful(HttpResponse(200, """{"boxId": "123"}""")))
    when(httpClient.POST[Any,Any](any, any, any)(any, any, any, any))
      .thenReturn(Future.successful(HttpResponse(201, """{"notificationId": "123"}""")))
    when(appConfig.pushPullNotificationHost).thenReturn("/notificationUrl")
    when(appConfig.pushNotificationUri(any)).thenReturn("/pushNotificationUrl")
  }

  "getBoxId" should {
    "return 200 status" in {
      val result = await(sut.getBoxId("clientId"))

      result.status mustBe OK
      result.body mustBe """{"boxId": "123"}"""
    }

    "send the request to the notification service" in {
      val queryParams = Seq(
        "boxName"  -> "customs/excise##1.0##notificationUrl",
        "clientId" -> "clientId"
      )
      await(sut.getBoxId("clientId"))
      verify(httpClient).GET(eqTo("/notificationUrl/box"), eqTo(queryParams), any)(any,any,any)
    }
  }

  "postNotification" should {

    val notification = Notification("mvId", "/url", "messageId", "consignor", Some("consignee"), "arc", "ern123")

    "return an HttpResponse" in {
      val result = await(sut.postNotification("boxId", notification))

      result.status mustBe CREATED
      result.body mustBe """{"notificationId": "123"}"""
    }

    "post the notification" in {
      await(sut.postNotification("boxId", notification))

      verify(httpClient).POST(
        eqTo("/pushNotificationUrl"),
        eqTo(notification),
        eqTo(Seq("Content-Type" -> "application/json"))
      )(any, any, any, any)
    }
  }
}

