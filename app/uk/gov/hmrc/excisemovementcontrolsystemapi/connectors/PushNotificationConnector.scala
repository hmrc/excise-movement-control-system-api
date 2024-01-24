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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{FailedBoxIdNotificationResponse, FailedPushNotification, SuccessBoxNotificationResponse, SuccessPushNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationConnector @Inject()(
                                           httpClient: HttpClient,
                                           appConfig: AppConfig,
                                           dateTimeService: DateTimeService
                                         )(implicit val ec: ExecutionContext) extends ResponseHandler with Logging
{
  def getBoxId(clientId: String)(implicit hc: HeaderCarrier): Future[Either[Result, SuccessBoxNotificationResponse]] = {

    val url = s"${appConfig.pushPullNotificationHost}/box"
    val queryParams = Seq(
      "boxName"  -> Constants.BoxName,
      "clientId" -> clientId
    )

    httpClient.GET[HttpResponse](
      url,
      queryParams
    ).map { response =>
      extractIfSuccessful[SuccessBoxNotificationResponse](response)
        .fold(error => Left(handleBoxNotificationError(error, url)), Right(_))
    }.recover {
      case ex: Throwable =>
        // todo: Is this an error?
        logger.error(s"[PushNotificationConnector] - Error retrieving BoxId, url: $url, message: ${ex.getMessage}", ex)
        Left(InternalServerError(Json.toJson(FailedBoxIdNotificationResponse(dateTimeService.timestamp(), ex.getMessage))))
    }
  }

  def postNotification(boxId: String, notification: Notification)(implicit hc: HeaderCarrier): Future[NotificationResponse] = {

    val url = appConfig.pushNotificationUri(boxId)

    httpClient.POST[Notification, HttpResponse](
      url,
      notification,
      Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
    ).map { response =>
      extractIfSuccessful[SuccessPushNotificationResponse](response)
        .fold(error => {
          logger.error(s"[PushNotificationConnector] - push notification error, url: $url, status: ${response.status}, message: ${response.body}")
          FailedPushNotification(error.status, error.body)
        }, r => r)
    }.recover {
      case ex: Throwable => {
        logger.error(s"[PushNotificationConnector] - push notification error: url: $url, message: ${ex.getMessage}", ex)
        FailedPushNotification(INTERNAL_SERVER_ERROR, ex.getMessage)
      }
    }
  }

  private def handleBoxNotificationError(response: HttpResponse, url: String): Result = {
    logger.error(s"[PushNotificationConnector] - Error retrieving BoxId, url: $url, status: ${response.status}, message: ${response.body}")
    val errorResponse = FailedBoxIdNotificationResponse(dateTimeService.timestamp(), response.body)

    response.status match {
      case 400 => BadRequest(Json.toJson(errorResponse))
      case 404 => NotFound(Json.toJson(errorResponse))
      case _ => InternalServerError(Json.toJson(errorResponse))
    }
  }
}

