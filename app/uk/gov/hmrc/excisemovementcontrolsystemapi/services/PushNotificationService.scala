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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.routes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{FailedBoxIdNotificationResponse, SuccessBoxNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.{Notification, NotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PushNotificationServiceImpl @Inject()(
                                             notificationConnector: PushNotificationConnector,
                                             dateTimeService: DateTimeService,
                                             appConfig: AppConfig
                                           )(implicit val ec: ExecutionContext) extends PushNotificationService with ResponseHandler with Logging {

  def getBoxId(
    clientId: String,
    clientBoxId: Option[String] = None
  )(implicit hc: HeaderCarrier): Future[Either[Result, Option[String]]] = {
    clientId: String
                clientId: String
              )(implicit hc: HeaderCarrier): Future[Either[Result, String]] = {

    notificationConnector.getBoxId(clientId)
    .map { response =>
      extractIfSuccessful[SuccessBoxNotificationResponse](response)
        .map(s => Some(s.boxId))
        .fold(error => Left(handleBoxNotificationError(error)), Right(_))
    }.recover {
      case ex: Throwable =>
        // todo: Is this an error?
        logger.error(s"[PushNotificationService] - Error retrieving BoxId, message: ${ex.getMessage}", ex)
        Left(InternalServerError(Json.toJson(FailedBoxIdNotificationResponse(
    clientBoxId match {
      case Some(id) => Future.successful(validateClientBoxId(id).map(s => s.boxId))
      case _ => notificationConnector.getDefaultBoxId(clientId)
    }
  }

  def validateClientBoxId(boxId: String): Either[Result, SuccessBoxNotificationResponse] = {
    Try(UUID.fromString(boxId)) match {
      case Success(value) => Right(SuccessBoxNotificationResponse(value.toString))
      case Failure(_) =>
        Left(BadRequest(Json.toJson(FailedBoxIdNotificationResponse(
          dateTimeService.timestamp(),
          "Client box id should be a valid UUID")))
        )
    }
  //  )(implicit hc: HeaderCarrier): Future[Either[Result, Option[String]]] = {
    logger.debug("<<<<<IN GET BOXID>>>>")

    //  )(implicit hc: HeaderCarrier): Future[Either[Result, Option[String]]] = {

    //    if (appConfig.featureFlagPPN) {
    notificationConnector.getBoxId(clientId)
      .map { response =>
        extractIfSuccessful[SuccessBoxNotificationResponse](response)
          .map(s => s.boxId)
          .fold(error => Left(handleBoxNotificationError(error)), Right(_))
      }.recover {
      case ex: Throwable =>
        // todo: Is this an error?
        logger.error(s"[PushNotificationService] - Error retrieving BoxId, message: ${ex.getMessage}", ex)
        Left(InternalServerError(Json.toJson(FailedBoxIdNotificationResponse(
          dateTimeService.timestamp(),
          s"Exception occurred when getting boxId for clientId: $clientId"))))
    }

    //    } else {
    //      Future.successful(Right(None))
    //    }

  }
  def sendNotification(
                        ern: String,
                        movement: Movement,
                        messageId: String
                      )(implicit hc: HeaderCarrier): Future[NotificationResponse] = {

    val boxId = movement.boxId
    logger.debug("<<<<<IN SEND NOTIFICATION>>>>")

    val notification = Notification(
      movement._id,
      buildMessageUriAsString(movement._id, messageId),
      messageId,
      movement.consignorId,
      movement.consigneeId,
      getArcOrThrowIfEmpty(movement.administrativeReferenceCode, messageId),
      ern)

    notificationConnector.postNotification(boxId, notification)
    movement.boxId match {
      case Some(boxId) =>

        notificationConnector.postNotification(boxId, notification)
          .map { response =>
            extractIfSuccessful[SuccessPushNotificationResponse](response)
              .fold(error => {
                logger.error(s"[PushNotificationService] - push notification error, status: ${response.status}, message: ${response.body}")
                FailedPushNotification(error.status, error.body)
              },
                success => success)
          }.recover {
          case ex: Throwable =>
            logger.error(s"[PushNotificationService] - push notification error, message: ${ex.getMessage}", ex)
            //todo: we may need a better message.
            FailedPushNotification(INTERNAL_SERVER_ERROR, s"An exception occurred when sending a notification with excise number: $ern, for message: $messageId")

        }

      case None =>
        logger.error("<<<<<NONE>>>>")
        Future.successful(NotInUseNotificationResponse())
    }
  }

  private def buildMessageUriAsString(movementId: String, messageId: String): String = {
    routes.GetMessagesController.getMessageForMovement(movementId, messageId).url
  }

  private def getArcOrThrowIfEmpty(arc: Option[String], messageId: String): String = {
    arc match {
      case Some(v) if v.trim.nonEmpty => v
      case _ => throw new RuntimeException(s"[PushNotificationService] - Could not push notification for message: $messageId. Administration Reference code is empty")
    }
  }
}

@ImplementedBy(classOf[PushNotificationServiceImpl])
trait PushNotificationService {

  def getBoxId(
    clientId: String,
    boxId: Option[String] = None
  )(implicit hc: HeaderCarrier): Future[Either[Result, Option[String]]]
  def getBoxId(clientId: String)(implicit hc: HeaderCarrier): Future[Either[Result, String]]

  def sendNotification(
                        ern: String,
                        movement: Movement,
                        messageId: String
                      )(implicit hc: HeaderCarrier): Future[NotificationResponse]
}
