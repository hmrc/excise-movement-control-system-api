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
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{FailedBoxIdNotificationResponse, FailedPushNotification, SuccessBoxNotificationResponse, SuccessPushNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.{Notification, NotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.routes

import scala.concurrent.{ExecutionContext, Future}

class PushNotificationServiceImpl @Inject()(
  notificationConnector: PushNotificationConnector,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext) extends PushNotificationService with ResponseHandler with Logging{

  def getBoxId(
    clientId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, SuccessBoxNotificationResponse]] = {

    notificationConnector.getBoxId(clientId)
    .map { response =>
      extractIfSuccessful[SuccessBoxNotificationResponse](response)
        .fold(error => Left(handleBoxNotificationError(error)), Right(_))
    }.recover {
      case ex: Throwable =>
        // todo: Is this an error?
        logger.error(s"[PushNotificationService] - Error retrieving BoxId, message: ${ex.getMessage}", ex)
        Left(InternalServerError(Json.toJson(FailedBoxIdNotificationResponse(
          dateTimeService.timestamp(),
          s"Exception occurred when getting boxId for clientId: $clientId"))))
    }
  }

  def sendNotification(
    ern: String,
    movement: Movement,
    messageId: String
  )(implicit hc: HeaderCarrier): Future[NotificationResponse] = {

    val notification = Notification(
      movement._id,
      buildMessageUriAsString(movement._id, messageId),
      messageId,
      movement.consignorId,
      movement.consigneeId,
      getArcOrThrowIfEmpty(movement.administrativeReferenceCode, messageId),
      ern)

    notificationConnector.postNotification(movement.boxId, notification)
      .map { response =>
        extractIfSuccessful[SuccessPushNotificationResponse](response)
          .fold(error => {
            logger.error(s"[PushNotificationService] - push notification error, status: ${response.status}, message: ${response.body}")
            FailedPushNotification(error.status, error.body)
          },
            success => success)
      }.recover {
      case ex: Throwable => {
        logger.error(s"[PushNotificationService] - push notification error, message: ${ex.getMessage}", ex)
        //todo: we may need a better message.
        FailedPushNotification(INTERNAL_SERVER_ERROR, s"An exception occurred when sending a notification with excise number: $ern, for message: $messageId")
      }
    }

  }

  private def buildMessageUriAsString(movementId: String, messageId: String): String = {
    routes.GetMessagesController.getMessageForMovement(movementId, messageId).url
   // s"/customs/excise/movements/$movementId/messages/$messageId"
  }

  private def getArcOrThrowIfEmpty(arc: Option[String], messageId: String): String = {
    arc match {
      case Some(v) if v.trim.nonEmpty => v
      case _ => throw new RuntimeException(s"[PushNotificationService] - Could not push notification for message: $messageId. Administration Reference code is empty")
    }
  }

  private def handleBoxNotificationError(response: HttpResponse): Result = {
    logger.error(s"[PushNotificationService] - Error retrieving BoxId. Status: ${response.status}, message: ${response.body}")
    val errorResponse = FailedBoxIdNotificationResponse(dateTimeService.timestamp(), response.body)

    response.status match {
      case 400 => BadRequest(Json.toJson(errorResponse))
      case 404 => NotFound(Json.toJson(errorResponse))
      case _ => InternalServerError(Json.toJson(errorResponse))
    }
  }
}

@ImplementedBy(classOf[PushNotificationServiceImpl])
trait PushNotificationService {

  def getBoxId(clientId: String)(implicit hc: HeaderCarrier): Future[Either[Result, SuccessBoxNotificationResponse]]

  def sendNotification(
    ern: String,
    movement: Movement,
    messageId: String
  )(implicit hc: HeaderCarrier): Future[NotificationResponse]
}
