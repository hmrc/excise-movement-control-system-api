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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import com.fasterxml.jackson.core.JacksonException
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISErrorMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification._
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PushNotificationConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends ResponseHandler
    with Logging {

  private def enforceCorrelationId(hc: HeaderCarrier): HeaderCarrier =
    hc.headers(Seq(HttpHeader.xCorrelationId)).headOption match {
      case Some(_) => hc
      case None    =>
        val correlationId = UUID.randomUUID().toString
        logger.info(s"generated new correlation id: $correlationId")
        hc.withExtraHeaders(HttpHeader.xCorrelationId -> correlationId)
    }

  private def internalError(boxId: String, notification: Notification) =
    FailedPushNotification(
      INTERNAL_SERVER_ERROR,
      s"An exception occurred when sending a notification with excise number: ${notification.ern}, boxId: $boxId, messageId: ${notification.messageId}"
    )

  def getDefaultBoxId(
    clientId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, SuccessBoxNotificationResponse]] = {

    val hc2 = enforceCorrelationId(hc)
    val url = s"${appConfig.pushPullNotificationsHost}/box"

    httpClient
      .get(url"$url?boxName=${Constants.BoxName}&clientId=$clientId")(hc2)
      .execute[HttpResponse]
      .map { response =>
        extractIfSuccessful[SuccessBoxNotificationResponse](response)
          .fold(error => Left(handleBoxNotificationError(error, clientId)), Right(_))
      }
      .recover { case NonFatal(ex) =>
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
    val hc2 = enforceCorrelationId(hc)
    val url = appConfig.pushPullNotificationsUri(boxId)

    httpClient
      .post(url"$url")(hc2)
      .withBody(Json.toJson(notification))
      .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .execute[SuccessPushNotificationResponse]
      .map { response =>
        logger.info(
          s"[PushNotificationConnector] - notification successfully sent to boxId: $boxId, for messageId: ${notification.messageId}"
        )
        response
      }
      .recover {
        case _: JacksonException =>
          // JSON parsing error
          logger.error(
            s"[PushNotificationConnector] - error parsing response for notification with boxId: $boxId, for messageId: ${notification.messageId}"
          )
          internalError(boxId, notification)

        case _: JsResultException =>
          // JSON deserialization error
          logger.error(
            s"[PushNotificationConnector] - error deserializing response for notification with boxId: $boxId, for messageId: ${notification.messageId}"
          )
          internalError(boxId, notification)

        case response: UpstreamErrorResponse =>
          // Upstream error
          logger.error(
            s"[PushNotificationConnector] - error sending notification with boxId: $boxId, status: ${response.statusCode}"
          )
          FailedPushNotification(response.statusCode, response.message)

        case NonFatal(ex) =>
          // Something else
          logger.error(
            s"[PushNotificationConnector] - error sending notification with boxId: $boxId error, for messageId: ${notification.messageId}",
            ex
          )
          internalError(boxId, notification)
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
