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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import cats.implicits.toFunctorOps
import com.google.inject.ImplementedBy
import org.apache.pekko.Done
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.routes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Notification
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PushNotificationServiceImpl @Inject() (
  notificationConnector: PushNotificationConnector,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends PushNotificationService
    with Logging {

  def getBoxId(
    clientId: String,
    clientBoxId: Option[String] = None
  )(implicit hc: HeaderCarrier): Future[Either[Result, String]] =
    (clientBoxId match {
      case Some(id) => Future.successful(validateClientBoxId(id))
      case _        => notificationConnector.getDefaultBoxId(clientId)
    }).map(futureValue =>
      futureValue
        .map(response => response.boxId)
    )

  private def buildMessageUriAsString(movementId: String, messageId: String): String =
    s"/customs/excise${routes.GetMessagesController.getMessageForMovement(movementId, messageId).url}"

  private def validateClientBoxId(boxId: String): Either[Result, SuccessBoxNotificationResponse] =
    Try(UUID.fromString(boxId)) match {
      case Success(value) => Right(SuccessBoxNotificationResponse(value.toString))
      case Failure(_)     =>
        logger.warn("[PushNotificationService] - Client box id should be a valid UUID")
        Left(
          BadRequest(
            Json.toJson(
              FailedBoxIdNotificationResponse(dateTimeService.timestamp(), "Client box id should be a valid UUID")
            )
          )
        )
    }

  override def sendNotification(
    boxId: String,
    recipientErn: String,
    movementId: String,
    messageId: String,
    messageType: String,
    consignor: String,
    consignee: Option[String],
    arc: Option[String]
  )(implicit hc: HeaderCarrier): Future[Done] = {
    val notification = Notification(
      movementId,
      buildMessageUriAsString(movementId, messageId),
      messageId,
      messageType,
      consignor,
      consignee,
      arc,
      recipientErn
    )
    notificationConnector.postNotification(boxId, notification).as(Done)
  }
}

@ImplementedBy(classOf[PushNotificationServiceImpl])
trait PushNotificationService {

  def getBoxId(
    clientId: String,
    boxId: Option[String] = None
  )(implicit hc: HeaderCarrier): Future[Either[Result, String]]

  def sendNotification(
    boxId: String,
    recipientErn: String,
    movementId: String,
    messageId: String,
    messageType: String,
    consignor: String,
    consignee: Option[String],
    arc: Option[String]
  )(implicit hc: HeaderCarrier): Future[Done]
}
