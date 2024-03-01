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

import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.PreValidateTraderHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.PreValidateTraderRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.PreValidateTraderResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreValidateTraderConnector @Inject()
(
  httpClient: HttpClient,
  emcsUtils: EmcsUtils,
  appConfig: AppConfig,
  metrics: Metrics,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) extends EISSubmissionHeaders with Logging {

  def submitMessage(request: PreValidateTraderRequest, ern: String)(implicit hc: HeaderCarrier):
  Future[Either[Result, PreValidateTraderResponse]] = {

    val timer = metrics.defaultRegistry.timer("emcs.prevalidatetrader.connector.timer").time()

    val correlationId = emcsUtils.generateCorrelationId
    val timestamp = dateTimeService.timestampToMilliseconds()
    val createdDateTime = timestamp.toString

    httpClient.POST[PreValidateTraderRequest, Either[Result, PreValidateTraderResponse]](
        appConfig.preValidateTraderUrl,
        request,
        build(correlationId, createdDateTime, appConfig.preValidateTraderBearerToken)
      )(PreValidateTraderRequest.format, PreValidateTraderHttpReader(correlationId, ern, createdDateTime), hc, ec)
      .andThen { case _ => timer.stop() }
      .recover {
        case ex: Throwable =>

          logger.warn(EISErrorMessage(createdDateTime, ern, ex.getMessage, correlationId, "PreValidateTrader"), ex)

          val error = EISErrorResponse(
            timestamp,
            "INTERNAL_SERVER_ERROR",
            "Exception",
            ex.getMessage,
            correlationId
          )
          Left(InternalServerError(Json.toJson(error)))

      }

  }

}
