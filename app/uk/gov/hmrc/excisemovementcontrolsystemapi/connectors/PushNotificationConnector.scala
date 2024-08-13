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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends ResponseHandler
    with Logging {

  def getDefaultBoxId(
    clientId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, SuccessBoxNotificationResponse]] = {

    val url = s"${appConfig.pushPullNotificationsHost}/box"

    httpClient
      .get(url"$url?boxName=${Constants.BoxName}&clientId=$clientId")
      .execute[HttpResponse]
      .map { response =>
        extractIfSuccessful[SuccessBoxNotificationResponse](response)
          .fold(error => Left(handleBoxNotificationError(error, clientId)), Right(_))
      }
      .recover { case ex: Throwable =>
        logger.error(s"[PushNotificationConnector] - Error retrieving BoxId for clientId: $clientId", ex)
        Left(
          InternalServerError(
            Json.toJson(
              FailedBoxIdNotificationResponse(
                dateTimeService.timestamp(),
                s"Exception occurred when getting boxId for clientId: $clientId"
              )
            )
          )
        )
      }
  }

  def postNotification(
    boxId: String,
    notification: Notification
  )(implicit hc: HeaderCarrier): Future[NotificationResponse] = {

    val url = appConfig.pushPullNotificationsUri(boxId)

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(notification))
      .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .execute[HttpResponse]
      .map { response =>
        extractIfSuccessful[SuccessPushNotificationResponse](response)
          .fold(
            error => {
              logger.error(
                s"[PushNotificationConnector] - error sending notification with boxId: $boxId, status: ${response.status}, message: ${response.body}"
              )
              FailedPushNotification(error.status, error.body)
            },
            success => {
              logger.info(
                s"[PushNotificationConnector] - notification successfully sent to boxId: $boxId, for messageId: ${notification.messageId}"
              )
              success
            }
          )
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"[PushNotificationConnector] - error sending notification with boxId: $boxId error, for messageId: ${notification.messageId}",
          ex
        )
        FailedPushNotification(
          INTERNAL_SERVER_ERROR,
          s"An exception occurred when sending a notification with excise number: ${notification.ern}, boxId: $boxId, messageId: ${notification.messageId}"
        )
      }
  }

  private def handleBoxNotificationError(response: HttpResponse, clientId: String): Result = {
    logger.error(
      s"[PushNotificationConnector] - Error retrieving BoxId for clientId: $clientId, status: ${response.status}, message: ${response.body}"
    )
    val errorResponse = FailedBoxIdNotificationResponse(dateTimeService.timestamp(), response.body)

    response.status match {
      case 400 => BadRequest(Json.toJson(errorResponse))
      case 404 => NotFound(Json.toJson(errorResponse))
      case _   => InternalServerError(Json.toJson(errorResponse))
    }
  }
}
