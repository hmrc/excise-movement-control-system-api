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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.{BoxNotificationResponse, Constants}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
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

  def getBoxId(clientId: String)(implicit hc: HeaderCarrier): Future[Either[Result, BoxNotificationResponse]] = {

    //todo: We may want correlationID and timestamp in the header. Push-poll-notification
    // does not do nothing with the header
    val queryParams = Seq(
      "boxName"  -> Constants.BoxName,
      "clientId" -> clientId
    )

    httpClient.GET[HttpResponse](
      s"${appConfig.pushPullNotificationHost}/box",
      queryParams
    ).map { response =>

      extractIfSuccessful[BoxNotificationResponse](response).fold(
        error => Left(handleNotificationError(error)),
        success => Right(success)
      )
    }.recover {
      case ex: Throwable =>
        //todo: Is this an error?
        logger.error(s"[PushPullNotificationConnector] - Error Error retrieving BoxId, message: ${ex.getMessage}", ex)
        Left(InternalServerError(createJsonErrorResponse(ex.getMessage)))
    }
  }

  private def handleNotificationError(response: HttpResponse): Result = {
    logger.error(s"[PushPullNotificationConnector] - push-pull-notification-api return an error, status: ${response.status}, message: ${response.body}")
    //todo: change to Instant type time
    val errorResponse = createJsonErrorResponse(response.body)

    response.status match {
      case 400 => BadRequest(Json.toJson(errorResponse))
      case 404 => NotFound(Json.toJson(errorResponse))
      case _ => InternalServerError(Json.toJson(errorResponse))
    }
  }

  private def createJsonErrorResponse(message: String): JsValue = {
    Json.toJson(ErrorResponse(dateTimeService.currentLocalDateTime, "Push Notification Error", message))
  }
}

