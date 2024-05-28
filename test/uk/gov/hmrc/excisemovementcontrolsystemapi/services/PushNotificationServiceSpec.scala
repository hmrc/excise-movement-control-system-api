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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach with ScalaFutures {

  implicit private val hc: HeaderCarrier = HeaderCarrier()
  implicit private val ex: ExecutionContext = ExecutionContext.global

  private val notificationConnector = mock[PushNotificationConnector]
  private val dateTimeService = mock[DateTimeService]
  private val appConfig = mock[AppConfig]
  private val timestamp = Instant.parse("2024-10-01T12:32:32.12345678Z")
  private val sut = new PushNotificationServiceImpl(notificationConnector, dateTimeService)
  private val boxId = "1c5b9365-18a6-55a5-99c9-83a091ac7f26"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(notificationConnector, dateTimeService)

    when(notificationConnector.getDefaultBoxId(any)(any))
      .thenReturn(Future.successful(Right(SuccessBoxNotificationResponse(boxId))))
    when(notificationConnector.postNotification(any, any)(any))
      .thenReturn(Future.successful(SuccessPushNotificationResponse("notificationId")))
    when(dateTimeService.timestamp()).thenReturn(timestamp)

    when(appConfig.pushNotificationsEnabled).thenReturn(true)
  }

  "getBoxId" should {

    "return the default box id" in {
      val result = await(sut.getBoxId("clientId"))
      result mustBe Right(boxId)

      withClue("send the request to the notification service") {
        verify(notificationConnector).getDefaultBoxId(eqTo("clientId"))(any)
      }
    }

    "return the client box id when this is present" in {
      val result = await(sut.getBoxId("clientId", Some(boxId)))

      result mustBe Right(boxId)

      withClue("not request box Id from the push-poll-notification service") {
        verifyZeroInteractions(notificationConnector)
      }
    }

    "return an error" when {

      "default box id return an error" in {
        when(notificationConnector.getDefaultBoxId(any)(any))
          .thenReturn(Future.successful(Left(
            InternalServerError(
              Json.toJson(FailedBoxIdNotificationResponse(timestamp, "error")))))
          )

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe InternalServerError(Json.toJson(FailedBoxIdNotificationResponse(timestamp, "error")))
      }

      "clientBoxId is not a valid UUID" in {
        val result = await(sut.getBoxId("clientId", Some("client-box-id")))

        result.left.value mustBe BadRequest(buildBoxIdJsonError("Client box id should be a valid UUID"))
        verifyZeroInteractions(notificationConnector)

      }
    }
  }

  "sendNotification" should {
    "send a notification" in {
      val result = await(sut.sendNotification("boxId", "ern", "id", "messageId", "IE801", "consignorId", Some("consigneeId"), Some("arc")))

      result mustBe Done

      val messageUri = s"/movements/id/messages/messageId"
      val notification = Notification("id", messageUri, "messageId", "IE801", "consignorId", Some("consigneeId"), Some("arc"), "ern")
      verify(notificationConnector).postNotification("boxId", notification)
    }

    "send a notification with an empty arc" in {
      val result = await(sut.sendNotification("boxId", "ern", "id", "messageId", "IE704", "consignorId", Some("consigneeId"), None))

      result mustBe Done

      val messageUri = s"/movements/id/messages/messageId"
      val notification = Notification("id", messageUri, "messageId", "IE704", "consignorId", Some("consigneeId"), None, "ern")
      verify(notificationConnector).postNotification("boxId", notification)
    }

    "return an error" when {
      "the notification API return an error" in {
        when(notificationConnector.postNotification(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("Error!")))

        sut.sendNotification("boxId", "ern", "id", "messageId", "IE801", "consignorId", Some("consigneeId"), Some("arc")).failed.futureValue
      }
    }
  }

  private def buildBoxIdJsonError(debugMessage: String) = {
    Json.parse(
      s"""{"dateTime":"2024-10-01T12:32:32.123Z",
         |"message":"Box Id error",
         |"debugMessage":"$debugMessage"}""".stripMargin)
  }
}
