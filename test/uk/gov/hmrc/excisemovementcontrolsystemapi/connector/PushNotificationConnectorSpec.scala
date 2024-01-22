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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{FailedPushNotification, SuccessBoxNotificationResponse, SuccessPushNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.typeOf

class PushNotificationConnectorSpec
  extends PlaySpec
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global;
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpClient = mock[HttpClient]
  private val appConfig = mock[AppConfig]
  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.now

  private val sut = new PushNotificationConnector(httpClient, appConfig, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig)

    when(httpClient.GET[Any](any, any, any)(any,any,any))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(SuccessBoxNotificationResponse("123")).toString())))
    when(appConfig.pushPullNotificationHost).thenReturn("/notificationUrl")
    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(appConfig.pushNotificationUri(any)).thenReturn("/pushNotificationUrl")
    when(httpClient.POST[Any,Any](any, any, any)(any, any, any, any))
      .thenReturn(Future.successful(HttpResponse(201, """{"notificationId": "123"}""")))
  }

  "getBoxId" should {
    "return 200 status" in {
      val result = await(sut.getBoxId("clientId"))
      result mustBe Right(SuccessBoxNotificationResponse("123"))
    }

    "send the request to the notification service" in {
      val queryParams = Seq(
        "boxName"  -> "customs/excise##1.0##notificationUrl",
        "clientId" -> "clientId"
      )
      await(sut.getBoxId("clientId"))
      verify(httpClient).GET(eqTo("/notificationUrl/box"), eqTo(queryParams), any)(any,any,any)
    }

    "return an error" when {
      "Box Id not found" in {
        val debugMessage = "Box does not exist"
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(404, debugMessage)))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe NotFound(buildBoxIdJsonError(debugMessage))
      }

      "is bad request" in {
        val debugMessage = "BAD_REQUEST"
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(400, debugMessage)))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe BadRequest(buildBoxIdJsonError(debugMessage))
      }

      "is unknown error" in {
        val debugMessage = "unknown error"
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(500, debugMessage)))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe InternalServerError(buildBoxIdJsonError(debugMessage))
      }

      "cannot parse json" in {
        val errorJson = Json.obj( "code" -> "UNKNOWN_ERROR", "message" -> "Box does not exist")
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(200, errorJson.toString())))

        val result = await(sut.getBoxId("clientId"))

        val expectedError: JsValue = buildBoxIdJsonError(s"Response body could not be read as type ${typeOf[SuccessBoxNotificationResponse]}")
        result.left.value mustBe InternalServerError(expectedError)
      }
    }
  }

  "postNotification" should {

    val notification = Notification("123", "/messageUli", "messageId", "ern123")

    "return a 200" in {
      val result = await(sut.postNotification("boxId", notification))

      result mustBe SuccessPushNotificationResponse("123")
    }

    "post the notification" in {
      await(sut.postNotification("boxId", notification))

      verify(httpClient).POST(
        eqTo("/pushNotificationUrl"),
        eqTo(notification),
        eqTo(Seq("Content-Type" -> "application/json"))
      )(any, any, any, any)
    }

    "return an error" when {
      "the notification API return an error" in {
        when(httpClient.POST[Any,Any](any, any, any)(any, any, any, any))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "Box ID is not a UUID")))

        val result = await(sut.postNotification("boxId", notification))

       Json.toJson(result.asInstanceOf[FailedPushNotification]) mustBe
         buildPushNotificationJsonError(BAD_REQUEST, "Box ID is not a UUID")
      }

      "invalid json" in {
        when(httpClient.POST[Any,Any](any, any, any)(any, any, any, any))
          .thenReturn(Future.successful(HttpResponse(200, "invalid json")))

        val result = await(sut.postNotification("boxId", notification))

        result mustBe FailedPushNotification(INTERNAL_SERVER_ERROR,
            s"Response body could not be read as type ${typeOf[SuccessPushNotificationResponse]}")
      }
    }
  }

  private def buildBoxIdJsonError(debugMessage: String) = {
    Json.parse(
      s"""{"dateTime":"$timestamp",
         |"message":"Box Id error",
         |"debugMessage":"$debugMessage"}""".stripMargin)
  }

  private def buildPushNotificationJsonError(status: Int, debugMessage: String) = {
    Json.parse(
      s"""{"status":$status,
         |"message":"Push notification error",
         |"debugMessage":"$debugMessage"}""".stripMargin)
  }
}

