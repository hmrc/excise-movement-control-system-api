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
import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PushNotificationConnector @Inject()(
  httpClient: HttpClient,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext) {

  def getBoxId(clientId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val url = s"${appConfig.pushPullNotificationHost}/box"
    val queryParams = Seq(
      "boxName"  -> Constants.BoxName,
      "clientId" -> clientId
    )

    httpClient.GET[HttpResponse](url, queryParams)
  }

  def postNotification(
    boxId: String,
    notification: Notification
  )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val url = appConfig.pushNotificationUri(boxId)

    httpClient.POST[Notification, HttpResponse](
      url,
      notification,
      Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
    )
  }
}

