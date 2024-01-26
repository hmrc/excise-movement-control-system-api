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

import com.google.inject.ImplementedBy
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.{Notification, NotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationServiceImpl @Inject()
(
  notificationConnector: PushNotificationConnector
)(implicit val ec: ExecutionContext) extends PushNotificationService {

  def sendNotification(ern: String, movement: Movement,  messageId: String)(implicit hc: HeaderCarrier): Future[NotificationResponse] = {

    if(!movement.administrativeReferenceCode.exists(_.trim.nonEmpty))
      throw new RuntimeException(s"[PushNotificationService] - Could not push notification for message: $messageId. Administration Reference code is empty")

    val notification = Notification(
      movement._id,
      messageId,
      movement.consignorId,
      movement.consigneeId,
      movement.administrativeReferenceCode.get,
      ern)

    notificationConnector.postNotification(movement.boxId, notification)

  }
}

@ImplementedBy(classOf[PushNotificationServiceImpl])
trait PushNotificationService {
  def sendNotification
  (
    ern: String,
    movement: Movement,
    messageId: String
  )(implicit hc: HeaderCarrier): Future[NotificationResponse]
}
