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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.SuccessPushNotificationResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PushNotificationServiceImpl
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit private val hc: HeaderCarrier = HeaderCarrier()
  implicit private val ex: ExecutionContext = ExecutionContext.global

  private val notificationConnector = mock[PushNotificationConnector]
  private val sut = new PushNotificationServiceImpl(notificationConnector)
  private val message = Message("this is a test", "IE801", Instant.now)
  private val movement = Movement("id", "boxId", "lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(notificationConnector)

    when(notificationConnector.postNotification(any, any)(any))
      .thenReturn(Future.successful(SuccessPushNotificationResponse("notificationId")))
  }

  "sendNotification" should {
    "send a notification" in {
      val result = await(sut.sendNotification("ern", movement, "messageId"))

      result mustBe SuccessPushNotificationResponse("notificationId")

      val notification = Notification("id", "messageId", "consignorId", Some("consigneeId"), "arc", "ern")
      verify(notificationConnector).postNotification("boxId", notification)
    }

    "return an error" when {
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
}
