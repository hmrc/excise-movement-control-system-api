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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification

import play.api.libs.json._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.GenericErrorResponse

import java.time.Instant

sealed trait NotificationResponse

object NotificationResponse {

  final case class SuccessPushNotificationResponse(notificationId: String) extends NotificationResponse

  object SuccessPushNotificationResponse {
    implicit val format: OFormat[SuccessPushNotificationResponse] = Json.format[SuccessPushNotificationResponse]
  }

  final case class SuccessBoxNotificationResponse(boxId: String) extends NotificationResponse

  object SuccessBoxNotificationResponse {
    implicit val format: OFormat[SuccessBoxNotificationResponse] = Json.format[SuccessBoxNotificationResponse]
  }

  final case class NotInUseNotificationResponse() extends NotificationResponse

  case class FailedBoxIdNotificationResponse(dateTime: Instant, debugMessage: String) extends GenericErrorResponse {
    override val message: String = "Box Id error"
  }

  object FailedBoxIdNotificationResponse {

    implicit val read: Reads[FailedBoxIdNotificationResponse] =
      Json.reads[FailedBoxIdNotificationResponse]

    implicit var write: OWrites[FailedBoxIdNotificationResponse] = {

      import play.api.libs.functional.syntax._

      (
        (__ \ "dateTime").write[Instant] and
          (__ \ "message").write[String] and
          (__ \ "debugMessage").write[String]
        ) (r => (r.dateTime, r.message, r.debugMessage))
    }
  }

  final case class FailedPushNotification(status: Int, debugMessage: String) extends NotificationResponse {
    val message: String = "Push notification error"
  }

  object FailedPushNotification {

    implicit val read: Reads[FailedPushNotification] =
      Json.reads[FailedPushNotification]

    implicit var write: OWrites[FailedPushNotification] = {

      import play.api.libs.functional.syntax._

      (
        (__ \ "status").write[Int] and
          (__ \ "message").write[String] and
          (__ \ "debugMessage").write[String]
        ) (r => (r.status, r.message, r.debugMessage))
    }
  }
}

case class Notification
(
  movementId: String,
  messageUri: String,
  messageId: String,
  messageType: String,
  consignor: String,
  consignee: Option[String],
  arc: Option[String],
  ern: String
)

object Notification {
  implicit val format: OFormat[Notification] = Json.format[Notification]
}
