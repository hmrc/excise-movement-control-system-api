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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.EisResponseHandler
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorMessage, Header}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EisUtils, MessageTypes, ShowNewMessageErrorResponse, ShowNewMessageResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ShowNewMessagesConnector @Inject()(
  httpClient: HttpClient,
  appConfig: AppConfig,
  eisUtils: EisUtils,
  metrics: Metrics
)(implicit ec: ExecutionContext) extends EisResponseHandler with Logging {

  def get(ern: String)(implicit hc: HeaderCarrier): Future[Either[Result, ShowNewMessageResponse]] = {

    val timer = metrics.defaultRegistry.timer("emcs.shownewmessage.timer").time()
    val correlationId = eisUtils.generateCorrelationId
    val dateTime = eisUtils.getCurrentDateTimeString

    httpClient.GET[HttpResponse](
      appConfig.showNewMessageUrl,
      Seq("exciseregistrationnumber" -> ern),
      Header.showNewMessage(correlationId, dateTime)
    ).map { response =>

      extractIfSuccessful[ShowNewMessageResponse](response) match {
        case Right(eisResponse) => Right(eisResponse)
        case Left(_) =>
          logger.warn(EISErrorMessage(dateTime,ern, response.body, correlationId, MessageTypes.IENewMessages))
          //todo: we might we want return the error message of the EIS
          Left(InternalServerError(response.body))
      }
    }
      .andThen {case _ => timer.stop() }
      .recover {
        case ex: Throwable =>
          logger.warn(EISErrorMessage(dateTime,ern, ex.getMessage, correlationId, MessageTypes.IENewMessages))
          Left(InternalServerError(ex.getMessage))
      }
  }

}

//todo: this my be generalised for reuse. see EISHttpReader
object ShowNewMessageReader extends Logging {
  def read(
    correlationId: String,
    ern: String,
    dateTime: String
  ): HttpReads[Either[ShowNewMessageErrorResponse, ShowNewMessageResponse]] =
    (_: String, _: String, response: HttpResponse) => {
      Try(Json.parse(response.body).as[ShowNewMessageResponse]) match {
        case Success(value) => Right(value)
        case Failure(exception) => {
          //Todo: output the default error message. see MovementMessageConnector. we may want to use EISErrorMessage
          logger.error("Error to pars JSON", exception)
          throw new RuntimeException(exception.getMessage)
        }
      }
    }
}
