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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NrsSubmissionWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.Retrying
import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NrsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  metrics: MetricRegistry,
  nrsSubmissionWorkItemRepository: NrsSubmissionWorkItemRepository
)(implicit val ec: ExecutionContext, val futures: Futures)
    extends Retrying
    with Logging {

  def sendToNrs(payload: NrsPayload)(implicit
    hc: HeaderCarrier
  ): Future[NonRepudiationSubmission] = {

    val timer      = metrics.timer("emcs.nrs.submission.timer").time()
    val jsonObject = payload.toJsObject

    send(jsonObject)
      .map { response: HttpResponse =>
        response.status match {
          case ACCEPTED           =>
            val submissionId = response.json.as[NonRepudiationSubmissionAccepted]
            logger.info(
              s"[NrsConnector] - Non repudiation submission accepted with nrSubmissionId: ${submissionId.nrSubmissionId}"
            )
            submissionId
          case x: Int if is5xx(x) =>
            nrsSubmissionWorkItemRepository.pushNew(NrsSubmissionWorkItem(payload))
            logger.warn(
              s"[NrsConnector] - Error when submitting to Non repudiation system (NRS) with status: ${response.status}, body: ${response.body}"
            )
            NonRepudiationSubmissionFailed(response.status, response.body)
          case _                  =>
            logger.warn(
              s"[NrsConnector] - Error when submitting to Non repudiation system (NRS) with status: ${response.status}, body: ${response.body}"
            )
            NonRepudiationSubmissionFailed(response.status, response.body)
        }
      }
      .andThen { case _ => timer.stop() }
  }

  private def send(
    jsonObject: JsObject
  )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val nrsSubmissionUrl = appConfig.getNrsSubmissionUrl
    httpClient
      .post(url"$nrsSubmissionUrl")
      .setHeader(createHeader: _*)
      .withBody(jsonObject)
      .execute[HttpResponse]
  }

  private def createHeader: Seq[(String, String)] =
    Seq(
      "Content-Type" -> "application/json",
      (XApiKeyHeaderKey, appConfig.nrsApiKey)
    )
}

object NrsConnector {
  val XApiKeyHeaderKey = "X-API-Key"
}
