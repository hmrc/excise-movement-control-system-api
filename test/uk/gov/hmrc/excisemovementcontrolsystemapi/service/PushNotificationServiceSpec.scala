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

package uk.gov.hmrc.excisemovementcontrolsystemapi.service

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{FailedPushNotification, SuccessBoxNotificationResponse, SuccessPushNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PushNotificationServiceImpl
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  implicit private val hc: HeaderCarrier = HeaderCarrier()
  implicit private val ex: ExecutionContext = ExecutionContext.global

  private val notificationConnector = mock[PushNotificationConnector]
  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.now
  private val sut = new PushNotificationServiceImpl(notificationConnector, dateTimeService)
  private val message = Message("this is a test", "IE801", "messageId", Instant.now)
  private val movement = Movement("id", "boxId", "lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
  private val boxIdSuccessResponse = Json.parse("""
  |{
  | "boxId": "1c5b9365-18a6-55a5-99c9-83a091ac7f26",
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
    reset(notificationConnector, dateTimeService)

    when(notificationConnector.getBoxId(any)(any))
      .thenReturn(Future.successful(HttpResponse(200, boxIdSuccessResponse.toString())))
    when(notificationConnector.postNotification(any, any)(any))
      .thenReturn(Future.successful(HttpResponse(200, Json.parse("""{"notificationId": "notificationId"}"""").toString())))
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  "getBoxId" should {
    "return 200 status" in {
      val result = await(sut.getBoxId("clientId"))
      result mustBe Right(SuccessBoxNotificationResponse("1c5b9365-18a6-55a5-99c9-83a091ac7f26"))
    }

    "send the request to the notification service" in {
      await(sut.getBoxId("clientId"))
      verify(notificationConnector).getBoxId(eqTo("clientId"))(any)
    }

    "return an error" when {
      "Box Id not found" in {
        when(notificationConnector.getBoxId(any)(any))
          .thenReturn(Future.successful(HttpResponse(404, "Box does not exist")))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe NotFound(buildBoxIdJsonError("Box does not exist"))
      }

      "is bad request" in {
        val debugMessage = "BAD_REQUEST"
        when(notificationConnector.getBoxId(any)(any))
          .thenReturn(Future.successful(HttpResponse(400, debugMessage)))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe BadRequest(buildBoxIdJsonError(debugMessage))
      }

      "is unknown error" in {
        val debugMessage = "unknown error"
        when(notificationConnector.getBoxId(any)(any))
          .thenReturn(Future.successful(HttpResponse(500, debugMessage)))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe InternalServerError(buildBoxIdJsonError(debugMessage))
      }

      "cannot parse json" in {
        val errorJson = Json.obj( "code" -> "UNKNOWN_ERROR", "message" -> "Box does not exist")
        when(notificationConnector.getBoxId(any)(any))
          .thenReturn(Future.successful(HttpResponse(200, errorJson.toString())))

        val result = await(sut.getBoxId("clientId"))

        val expectedError: JsValue = buildBoxIdJsonError("Exception occurred when getting boxId for clientId: clientId")
        result.left.value mustBe InternalServerError(expectedError)
      }
    }
  }
  "sendNotification" should {
    "send a notification" in {
      val result = await(sut.sendNotification("ern", movement, "messageId"))

      result mustBe SuccessPushNotificationResponse("notificationId")

      val messageUri = s"/movements/id/message/messageId"
      val notification = Notification("id", messageUri, "messageId", "consignorId", Some("consigneeId"), "arc", "ern")
      verify(notificationConnector).postNotification("boxId", notification)
    }

    "return an error" when {

      "the notification API return an error" in {
        when(notificationConnector.postNotification(any, any)(any))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "Box ID is not a UUID")))

        val result = await(sut.sendNotification("ern", movement, "messageId"))

        Json.toJson(result.asInstanceOf[FailedPushNotification]) mustBe
          buildPushNotificationJsonError(BAD_REQUEST, "Box ID is not a UUID")
      }

      "invalid json" in {
        when(notificationConnector.postNotification(any, any)(any))
          .thenReturn(Future.successful(HttpResponse(200, "invalid json")))

        val result = await(sut.sendNotification("ern", movement, "messageId"))

        result mustBe FailedPushNotification(INTERNAL_SERVER_ERROR,
          "An exception occurred when sending a notification with excise number: ern, for message: messageId")
      }

      "Administration reference code (ARC) is missing" in {
        the[RuntimeException] thrownBy
          await(sut.sendNotification("ern", movement.copy(administrativeReferenceCode = None), "messageId")) must
          have message "[PushNotificationService] - Could not push notification for message: messageId. Administration Reference code is empty"

        verifyZeroInteractions(notificationConnector)
      }

      "Administration reference code (ARC) is empty" in {
        the[RuntimeException] thrownBy
          await(sut.sendNotification("ern", movement.copy(administrativeReferenceCode = Some("")), "messageId")) must
          have message "[PushNotificationService] - Could not push notification for message: messageId. Administration Reference code is empty"

        verifyZeroInteractions(notificationConnector)
      }
    }
  }

  private def buildBoxIdJsonError(debugMessage: String) = {
    Json.parse(
      s"""{"dateTime":"$timestamp",
         |"message":"Box Id error",
         |"debugMessage":"$debugMessage"}""".stripMargin)
  }

   def buildPushNotificationJsonError(status: Int, debugMessage: String) = {
    Json.parse(
      s"""{"status":$status,
         |"message":"Push notification error",
         |"debugMessage":"$debugMessage"}""".stripMargin)
  }
}
