/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISRequest, EISResponse, Header}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MovementMessageConnector @Inject()
(
  httpClient: HttpClient,
  eisUtils: EisUtils,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) extends Logging {

  def post(message: String, messageType: String)(implicit hc: HeaderCarrier): Future[Either[EISErrorResponse, EISResponse]] = {

    //todo: add metrics
    //todo: add retry
    //todo: message need to be encode Base64
    val eisRequest = EISRequest(eisUtils.generateCorrelationId, eisUtils.getCurrentDateTimeString, messageType, "APIP", "user1", message)

    httpClient.POST[EISRequest, HttpResponse](
      appConfig.emcsReceiverMessageUrl,
      eisRequest,
      Header.build(eisRequest.emcsCorrelationId, eisRequest.createdDateTime)
    ).map(
      response => {
        response.status match {
          case OK => Right(Json.parse(response.body).as[EISResponse])
          case _ =>
            val errorResponse = Json.parse(response.body).as[EISErrorResponse]
            logger.error(s"EIS errorResponse. Status: ${errorResponse.status}, message: ${errorResponse.message} and correlationId: ${errorResponse.emcsCorrelationId}")
            Left(errorResponse)
        }}
      )
  }
}
