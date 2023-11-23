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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.ResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageReceiptResponse, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISConsumptionHeaders, EISErrorMessage}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MessageReceiptConnector @Inject()
(
  httpClient: HttpClient,
  appConfig: AppConfig,
  eisUtils: EmcsUtils,
  metrics: Metrics
)(implicit val ec: ExecutionContext) extends EISConsumptionHeaders with ResponseHandler with Logging {

  def put(ern: String)(implicit hc: HeaderCarrier): Future[Either[Result, MessageReceiptResponse]] = {

    val timer = metrics.defaultRegistry.timer("emcs.messagereceipt.timer").time()
    val dateTime = eisUtils.getCurrentDateTimeString
    val correlationId = eisUtils.generateCorrelationId

    httpClient.PUTString[HttpResponse](
      appConfig.messageReceiptUrl(ern),
      "",
      build(correlationId, dateTime, appConfig.messageReceiptBearerToken)
    ).map {
      response =>
        extractIfSuccessful[MessageReceiptResponse](response) match {
          case Right(eisResponse) => Right(eisResponse)
          case Left(error) =>
            logger.error(EISErrorMessage(dateTime, ern, response.body, correlationId, MessageTypes.IE_MESSAGE_RECEIPT.value))
            Left(InternalServerError(error.body))
        }
    }
      .andThen(_ => timer.stop())
      .recover {
        case ex: Throwable =>
          logger.error(EISErrorMessage(dateTime, ern, ex.getMessage, correlationId, MessageTypes.IE_MESSAGE_RECEIPT.value), ex)
          Left(InternalServerError(ex.getMessage))
      }
  }
}