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
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.concurrent.Futures
import play.api.libs.json.JsObject
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.XApiKeyHeaderKey
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NonRepudiationSubmission, NonRepudiationSubmissionAccepted, NonRepudiationSubmissionFailed, NrsPayload}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.Retrying
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class NrsConnector @Inject()
(
  httpClient: HttpClient,
  appConfig: AppConfig,
  metrics: MetricRegistry
)(implicit val ec: ExecutionContext, val futures: Futures) extends Retrying with Logging {

  def sendToNrs(payload: NrsPayload, correlationId: String)(implicit hc: HeaderCarrier): Future[NonRepudiationSubmission] = {

    val timer = metrics.timer("emcs.nrs.submission.timer").time()
    val jsonObject = payload.toJsObject

    retry(appConfig.nrsRetryDelays.toList, canRetry, appConfig.getNrsSubmissionUrl) {
      send(jsonObject, correlationId)
    }
      .map { response: HttpResponse =>
        response.status match {
          case ACCEPTED =>
            val submissionId = response.json.as[NonRepudiationSubmissionAccepted]
            logger.info(s"[NrsConnector] - Non repudiation submission accepted with nrSubmissionId: ${submissionId.nrSubmissionId}")
            submissionId
          case _ =>
            logger.warn(s"[NrsConnector] - Error when submitting to Non repudiation system (NRS) with status: ${response.status}, correlationId: $correlationId")
            NonRepudiationSubmissionFailed(response.status, response.body)
        }
      }
      .andThen { case _ => timer.stop() }
  }

  def send(
            jsonObject: JsObject,
            correlationId: String
          )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val nrsSubmissionUrl = appConfig.getNrsSubmissionUrl
    logger.info(s"[NrsConnector] - NRS submission: sending POST request to $nrsSubmissionUrl. CorrelationId: $correlationId")

    httpClient.POST[JsObject, HttpResponse](
      nrsSubmissionUrl,
      body = jsonObject,
      createHeader)
  }

  private def canRetry(response: Try[HttpResponse]): Boolean = {
    response match {
      case Success(r) => !is2xx(r.status)
      case _ => true
    }
  }


  private def createHeader: Seq[(String, String)] = {
    Seq(
      "Content-Type" -> "application/json",
      (XApiKeyHeaderKey, appConfig.nrsApiKey)
    )
  }
}

object NrsConnector {
  val XApiKeyHeaderKey = "X-API-Key"
}
