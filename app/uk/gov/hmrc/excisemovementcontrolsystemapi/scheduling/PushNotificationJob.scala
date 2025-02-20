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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import cats.syntax.all._
import org.apache.pekko.Done
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository.MessageNotification
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PushNotificationService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

@Singleton
class PushNotificationJob @Inject() (
  configuration: Configuration,
  movementRepository: MovementRepository,
  pushNotificationService: PushNotificationService
) extends ScheduledJob
    with Logging {

  override def name: String = "push-notification-job"

  override def execute(implicit ec: ExecutionContext): Future[ScheduledJob.Result] =
    movementRepository.getPendingMessageNotifications
      .flatMap { notifications =>
        notifications.traverse { notification =>
          processNotification(notification).recover { case NonFatal(_) =>
            logger.info(
              s"Failed to notify ${notification.recipient} for message ${notification.messageId}. Will try again later."
            )
            Done
          }
        }
      }
      .as(ScheduledJob.Result.Completed)

  private def processNotification(notification: MessageNotification)(implicit ec: ExecutionContext): Future[Done] =
    for {
      _ <- sendNotification(notification)
      _ <- confirmNotification(notification)
    } yield Done

  private def sendNotification(notification: MessageNotification): Future[Done] =
    pushNotificationService.sendNotification(
      notification.boxId,
      notification.recipient,
      notification.movementId,
      notification.messageId,
      notification.messageType,
      notification.consignor,
      notification.consignee,
      notification.arc
    )

  private def confirmNotification(notification: MessageNotification): Future[Done] =
    movementRepository.confirmNotification(notification.movementId, notification.messageId, notification.boxId)

  override val enabled: Boolean             = configuration.get[Boolean]("featureFlags.pushNotificationsEnabled")
  override def initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.pushNotificationJob.initialDelay")
  override def interval: FiniteDuration     = configuration.get[FiniteDuration]("scheduler.pushNotificationJob.interval")
  implicit val hc: HeaderCarrier            = HeaderCarrier()
}
