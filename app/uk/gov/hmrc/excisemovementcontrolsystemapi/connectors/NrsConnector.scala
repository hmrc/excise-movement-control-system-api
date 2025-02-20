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

import com.codahale.metrics.MetricRegistry
import org.apache.pekko.Done
import org.apache.pekko.pattern.CircuitBreaker
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.concurrent.Futures
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.{NrsCircuitBreaker, UnexpectedResponseException, XApiKeyHeaderKey}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.Retrying
import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class NrsConnector @Inject()(
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  metrics: MetricRegistry,
  nrsCircuitBreaker: NrsCircuitBreaker
)(implicit val ec: ExecutionContext, val futures: Futures)
    extends Retrying
    with Logging {

  def sendToNrs(payload: NrsPayload, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val exciseNumber = payload.metadata.searchKeys.getOrElse("ern", "No ERN present")

    val nrsSubmissionUrl = appConfig.getNrsSubmissionUrl
    logger.info(
      s"NRS submission: CorrelationId: $correlationId, for ERN: $exciseNumber"
    )

    def responseStatusAsFailure(): Try[HttpResponse] => Boolean = {
      case Success(n) => is5xx(n.status)
      case Failure(_) => true
    }

    nrsCircuitBreaker.breaker
      .withCircuitBreaker(
        httpClient
          .post(url"$nrsSubmissionUrl")
          .setHeader("Content-Type" -> "application/json")
          .setHeader(XApiKeyHeaderKey -> appConfig.nrsApiKey)
          .withBody(payload.toJsObject)
          .execute[HttpResponse],
        responseStatusAsFailure()
      )
      .flatMap { response =>
        response.status match {
          case ACCEPTED                =>
            logger.info(
              s"Non repudiation submission completed with status ${response.status}"
            )
            Future.successful(Done)
          case status if is5xx(status) =>
            logger.warn(
              s"Non repudiation submission failed with status ${response.status}"
            )
            Future.failed(UnexpectedResponseException(response.status, response.body))
          case _                       =>
            logger.warn(
              s"Non repudiation submission responded with status ${response.status}"
            )
            Future.failed(UnexpectedResponseException(response.status, response.body))
        }
      }
  }

}

object NrsConnector {
  val XApiKeyHeaderKey = "X-API-Key"

  final case class UnexpectedResponseException(status: Int, body: String) extends Exception {
    override def getMessage: String = s"Unexpected response from NRS, status: $status, body: $body"
  }

  final case class NrsCircuitBreaker(breaker: CircuitBreaker)
}
