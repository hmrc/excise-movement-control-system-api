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

import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.EISHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.Header.EmcsSource
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISRequest, EISResponse, Header}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MovementMessageConnector @Inject()
(
  httpClient: HttpClient,
  eisUtils: EisUtils,
  appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) extends Logging {


  def post(message: String, messageType: String)(implicit hc: HeaderCarrier): Future[Either[Result, EISResponse]] = {

    val timer = metrics.defaultRegistry.timer("emcs.eiscontroller.timer").time()

    //todo: add retry
    val correlationId = eisUtils.generateCorrelationId
    val createdDateTime = eisUtils.getCurrentDateTimeString
    val encodedMessage = eisUtils.createEncoder.encodeToString(message.getBytes(StandardCharsets.UTF_8))
    val eisRequest = EISRequest(correlationId, createdDateTime, messageType, EmcsSource, "user1", encodedMessage)

      httpClient.POST[EISRequest, Either[Result, EISResponse]](
        appConfig.emcsReceiverMessageUrl,
        eisRequest,
        Header.build(correlationId, createdDateTime)
      )(EISRequest.format, EISHttpReader(correlationId), hc, ec)
        .andThen { case _ => timer.stop() }
        .recover {
          case ex: Throwable =>
            logger.error(s"EIS error with message: ${ex.getMessage} and correlationId: $correlationId", ex)
            Left(InternalServerError(ex.getMessage))
    }
  }
}
