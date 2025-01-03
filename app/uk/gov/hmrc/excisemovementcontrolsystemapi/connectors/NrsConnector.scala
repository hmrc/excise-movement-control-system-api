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
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.concurrent.Futures
import play.api.libs.json.JsObject
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew.XApiKeyHeaderKey
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.Retrying
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class NrsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  metrics: MetricRegistry
)(implicit val ec: ExecutionContext, val futures: Futures)
    extends Retrying
    with Logging {

  def sendToNrsOld(payload: NrsPayload, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val timer      = metrics.timer("emcs.nrs.submission.timer").time()
    val jsonObject = payload.toJsObject

    retry(appConfig.nrsRetryDelays.toList, canRetry, appConfig.getNrsSubmissionUrl) {
      send(jsonObject, correlationId)
    }
      .map { response: HttpResponse =>
        response.status match {
          case ACCEPTED =>
            val submissionId = response.json \ "nrSubmissionId"
            logger.info(
              s"Non repudiation submission accepted with nrSubmissionId: $submissionId"
            )
            Done
          case _        =>
            logger.warn(
              s"Error when submitting to Non repudiation system (NRS) with status: ${response.status}, body: ${response.body}, correlationId: $correlationId"
            )
            Done
        }
      }
      .andThen { case _ => timer.stop() }
  }

  private def send(
    jsonObject: JsObject,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val nrsSubmissionUrl = appConfig.getNrsSubmissionUrl
    logger.info(
      s"[NrsConnector] - NRS submission: sending POST request to $nrsSubmissionUrl. CorrelationId: $correlationId"
    )
    httpClient
      .post(url"$nrsSubmissionUrl")
      .setHeader(createHeader: _*)
      .withBody(jsonObject)
      .execute[HttpResponse]
  }

  private def canRetry(response: Try[HttpResponse]): Boolean =
    response match {
      case Success(r) => !is2xx(r.status)
      case _          => true
    }

  private def createHeader: Seq[(String, String)] =
    Seq(
      "Content-Type" -> "application/json",
      (XApiKeyHeaderKey, appConfig.nrsApiKey)
    )
}
