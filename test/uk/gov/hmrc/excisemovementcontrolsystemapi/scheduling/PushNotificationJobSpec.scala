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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository.MessageNotification
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PushNotificationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class PushNotificationJobSpec
    extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val movementRepository       = mock[MovementRepository]
  private val pushNotificationService  = mock[PushNotificationService]
  private lazy val pushNotificationJob = app.injector.instanceOf[PushNotificationJob]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[PushNotificationService].toInstance(pushNotificationService)
    )
    .configure(
      "scheduler.pushNotificationJob.initialDelay" -> "2 minutes",
      "scheduler.pushNotificationJob.interval"     -> "1 minute"
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      movementRepository,
      pushNotificationService
    )
  }

  "push notifications job" when {
    "initialised" should {
      "have the correct name" in {
        pushNotificationJob.name mustBe "push-notification-job"
      }
      "use initial delay from configuration" in {
        pushNotificationJob.initialDelay mustBe FiniteDuration(2, "minutes")
      }
      "use interval from configuration" in {
        pushNotificationJob.interval mustBe FiniteDuration(1, "minute")
      }
    }
  }

  "executing" when {
    "there are no messages pending to be notified" should {
      "not send any notifications" in {
        when(movementRepository.getPendingMessageNotifications).thenReturn(Future.successful(Seq.empty))

        pushNotificationJob.execute.futureValue

        verify(pushNotificationService, never()).sendNotification(any, any, any, any, any, any, any, any)(any)
      }
    }
    "there are messages pending to be notified" when {
      "notifications are successfully sent for each message" should {
        "send a notification for each message" in {
          val notification1 = MessageNotification(
            "movement1",
            "message1",
            "IE801",
            "consignor",
            Some("consignee"),
            Some("arc1"),
            "consignor",
            "box1"
          )
          val notification2 = MessageNotification(
            "movement1",
            "message2",
            "IE818",
            "consignor",
            Some("consignee"),
            Some("arc2"),
            "consignee",
            "box2"
          )
          when(movementRepository.getPendingMessageNotifications).thenReturn(
            Future.successful(Seq(notification1, notification2))
          )
          when(movementRepository.confirmNotification(any, any, any)).thenReturn(Future.successful(Done))
          when(pushNotificationService.sendNotification(any, any, any, any, any, any, any, any)(any)).thenReturn(
            Future.successful(Done)
          )

          pushNotificationJob.execute.futureValue

          verify(pushNotificationService).sendNotification(
            eqTo("box1"),
            eqTo("consignor"),
            eqTo("movement1"),
            eqTo("message1"),
            eqTo("IE801"),
            eqTo("consignor"),
            eqTo(Some("consignee")),
            eqTo(Some("arc1"))
          )(any)
          verify(pushNotificationService).sendNotification(
            eqTo("box2"),
            eqTo("consignee"),
            eqTo("movement1"),
            eqTo("message2"),
            eqTo("IE818"),
            eqTo("consignor"),
            eqTo(Some("consignee")),
            eqTo(Some("arc2"))
          )(any)
        }
        "confirm each notification" in {
          val notification1 = MessageNotification(
            "movement1",
            "message1",
            "IE801",
            "consignor",
            Some("consignee"),
            Some("arc1"),
            "consignor",
            "box1"
          )
          val notification2 = MessageNotification(
            "movement1",
            "message2",
            "IE818",
            "consignor",
            Some("consignee"),
            Some("arc2"),
            "consignee",
            "box2"
          )
          when(movementRepository.getPendingMessageNotifications).thenReturn(
            Future.successful(Seq(notification1, notification2))
          )
          when(movementRepository.confirmNotification(any, any, any)).thenReturn(Future.successful(Done))
          when(pushNotificationService.sendNotification(any, any, any, any, any, any, any, any)(any)).thenReturn(
            Future.successful(Done)
          )

          pushNotificationJob.execute.futureValue

          verify(movementRepository).confirmNotification("movement1", "message1", "box1")
          verify(movementRepository).confirmNotification("movement1", "message2", "box2")
        }
      }

      "a notification fails for a message" should {
        "not confirm a notification but process further notifications" in {
          val notification1 = MessageNotification(
            "movement1",
            "message1",
            "IE801",
            "consignor",
            Some("consignee"),
            Some("arc1"),
            "consignor",
            "box1"
          )
          val notification2 = MessageNotification(
            "movement1",
            "message2",
            "IE818",
            "consignor",
            Some("consignee"),
            Some("arc2"),
            "consignee",
            "box2"
          )
          when(movementRepository.getPendingMessageNotifications).thenReturn(
            Future.successful(Seq(notification1, notification2))
          )
          when(movementRepository.confirmNotification(any, any, any)).thenReturn(Future.successful(Done))
          when(pushNotificationService.sendNotification(any, any, any, any, any, any, any, any)(any)).thenReturn(
            Future.failed(new RuntimeException("Error")),
            Future.successful(Done)
          )

          pushNotificationJob.execute.futureValue

          verify(movementRepository, never()).confirmNotification("movement1", "message1", "box1")
          verify(movementRepository).confirmNotification("movement1", "message2", "box2")
        }
      }

      "a confirmation of a notification fails for a message" should {
        "not fail the job and process further notifications" in {
          val notification1 = MessageNotification(
            "movement1",
            "message1",
            "IE801",
            "consignor",
            Some("consignee"),
            Some("arc1"),
            "consignor",
            "box1"
          )
          val notification2 = MessageNotification(
            "movement1",
            "message2",
            "IE818",
            "consignor",
            Some("consignee"),
            Some("arc2"),
            "consignee",
            "box2"
          )
          when(movementRepository.getPendingMessageNotifications).thenReturn(
            Future.successful(Seq(notification1, notification2))
          )
          when(movementRepository.confirmNotification(any, any, any)).thenReturn(
            Future.failed(new RuntimeException("Error")),
            Future.successful(Done)
          )
          when(pushNotificationService.sendNotification(any, any, any, any, any, any, any, any)(any)).thenReturn(
            Future.successful(Done)
          )

          pushNotificationJob.execute.futureValue

          verify(movementRepository).confirmNotification("movement1", "message2", "box2")
        }
      }
    }
  }
}
