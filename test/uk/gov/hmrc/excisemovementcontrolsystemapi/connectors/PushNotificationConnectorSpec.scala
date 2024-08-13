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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationConnectorSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val httpClient         = mock[HttpClientV2]
  private val appConfig          = mock[AppConfig]
  private val dateTimeService    = mock[DateTimeService]
  private val mockRequestBuilder = mock[RequestBuilder]

  private val timestamp = Instant.parse("2024-09-09T18:30:01.12345678Z")
  private val boxId     = "1c5b9365-18a6-55a5-99c9-83a091ac7f26"

  private val sut = new PushNotificationConnector(httpClient, appConfig, dateTimeService)

  private val boxIdSuccessResponse = Json.parse(s"""
  |{
  | "boxId": "$boxId",
  |    "boxName":"BOX 2",
  |    "boxCreator":{
  |        "clientId": "X5ZasuQLH0xqKooV_IEw6yjQNfEa"
  |    },
  |    "subscriber": {
  |        "subscribedDateTime": "2020-06-01T10:27:33.613+0000",
  |        "callBackUrl": "https://www.example.com/callback",
  |        "subscriptionType": "API_PUSH_SUBSCRIBER"
  |    }
  |}""".stripMargin)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig)
    when(appConfig.pushPullNotificationsHost).thenReturn("http://localhost:8888")
    when(appConfig.pushPullNotificationsUri(any)).thenReturn("http://localhost:8888/pushNotificationUrl")
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  "getDefaultBoxId" should {
    "return 200 status" in {
      when(httpClient.get(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, boxIdSuccessResponse, Map())))

      val result = await(sut.getDefaultBoxId("clientId"))

      result mustBe Right(SuccessBoxNotificationResponse(boxId))
    }

    "send the request to the notification service" in {
      when(httpClient.get(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, boxIdSuccessResponse, Map())))

      await(sut.getDefaultBoxId("clientId"))

      verify(httpClient).get(
        eqTo(
          url"http://localhost:8888/box?boxName=customs/excise%23%231.0%23%23notificationUrl&clientId=clientId"
        )
      )(any)
    }

    "return an error" when {
      "Box Id not found" in {
        when(httpClient.get(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(404, "Box does not exist")))

        val result = await(sut.getDefaultBoxId("clientId"))

        result.left.value mustBe NotFound(buildBoxIdJsonError("Box does not exist"))
      }

      "is bad request" in {
        val debugMessage = "BAD_REQUEST"
        when(httpClient.get(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(400, "BAD_REQUEST")))

        val result = await(sut.getDefaultBoxId("clientId"))

        result.left.value mustBe BadRequest(buildBoxIdJsonError(debugMessage))
      }

      "is unknown error" in {
        val debugMessage = "unknown error"
        when(httpClient.get(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(500, debugMessage)))

        val result = await(sut.getDefaultBoxId("clientId"))

        result.left.value mustBe InternalServerError(buildBoxIdJsonError(debugMessage))
      }

      "cannot parse json" in {
        val errorJson = Json.obj("code" -> "UNKNOWN_ERROR", "message" -> "Box does not exist")
        when(httpClient.get(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, errorJson, Map())))

        val result = await(sut.getDefaultBoxId("clientId"))

        val expectedError: JsValue = buildBoxIdJsonError("Exception occurred when getting boxId for clientId: clientId")
        result.left.value mustBe InternalServerError(expectedError)
      }
    }
  }

  "postNotification" should {

    val messageId    = "messageId"
    val ern          = "ern123"
    val notification =
      Notification("mvId", "/url", messageId, "IE801", "consignor", Some("consignee"), Some("arc"), ern)

    "return a success response" in {
      when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(201, """{"notificationId": "123"}""")))
      val result = await(sut.postNotification(boxId, notification))

      result mustBe SuccessPushNotificationResponse("123")
    }

    "post the notification" in {
      when(httpClient.post(any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(201, """{"notificationId": "123"}""")))

      await(sut.postNotification(boxId, notification))

      verify(mockRequestBuilder, times(1)).execute[HttpResponse]
    }

    "return an error" when {

      "the notification API return an error" in {
        when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "Box ID is not a UUID")))

        val result = await(sut.postNotification(boxId, notification))

        Json.toJson(result.asInstanceOf[FailedPushNotification]) mustBe
          buildPushNotificationJsonError(BAD_REQUEST, "Box ID is not a UUID")
      }

      "invalid json" in {
        when(httpClient.post(any)(any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, "invalid json")))

        val result = await(sut.postNotification(boxId, notification))

        Json.toJson(result.asInstanceOf[FailedPushNotification]) mustBe
          buildPushNotificationJsonError(
            INTERNAL_SERVER_ERROR,
            s"An exception occurred when sending a notification with excise number: $ern, boxId: $boxId, messageId: $messageId"
          )
      }
    }
  }

  private def buildBoxIdJsonError(debugMessage: String) =
    Json.parse(s"""{"dateTime":"2024-09-09T18:30:01.123Z",
         |"message":"Box Id error",
         |"debugMessage":"$debugMessage"}""".stripMargin)

  private def buildPushNotificationJsonError(status: Int, debugMessage: String) =
    Json.parse(s"""{"status":$status,
         |"message":"Push notification error",
         |"debugMessage":"$debugMessage"}""".stripMargin)
}
