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
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EISErrorResponseDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpReads, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.NodeSeq
import HttpReads.Implicits._
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse.format
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat

class EISSubmissionConnector @Inject() (
  httpClient: HttpClientV2,
  emcsUtils: EmcsUtils,
  appConfig: AppConfig,
  metrics: MetricRegistry,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends EISSubmissionHeaders
    with Logging
    with HttpErrorFunctions {

  implicit def errorRead(status: Int): Reads[EISErrorResponseDetails] =
    Json
      .reads[EISErrorResponse]
      .map(error => EISErrorResponseDetails.createFromEISError(status, dateTimeService.timestamp(), error))
      .orElse(
        Json
          .reads[RimValidationErrorResponse]
          .map(error => EISErrorResponseDetails.createFromRIMError(status, dateTimeService.timestamp(), error))
      )

  def submitMessage(
    message: IEMessage,
    requestXmlAsString: String,
    authorisedErn: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[EISErrorResponseDetails, EISSubmissionResponse]] = {
    logger.info("[EISSubmissionConnector]: Submitting a message to EIS")

    val timer           = metrics.timer("emcs.submission.connector.timer").time()
    val timestamp       = dateTimeService.timestamp()
    val createdDateTime = timestamp.asStringInMilliseconds
    val wrappedXml      = wrapXmlInControlDocument(message.messageIdentifier, requestXmlAsString, authorisedErn)
    val messageType     = message.messageType
    val encodedMessage  = emcsUtils.encode(wrappedXml.toString)

    val eisRequest = EISSubmissionRequest(authorisedErn, messageType, encodedMessage)

    httpClient
      .post(url"${appConfig.emcsReceiverMessageUrl}")
      .setHeader(build(correlationId, createdDateTime, appConfig.submissionBearerToken): _*)
      .withBody(Json.toJson(eisRequest))
      .execute[HttpResponse]
      .map { response =>
        if (is2xx(response.status)) {
          Right(response.json.as[EISSubmissionResponse])
        } else {
          Left(response.json.as[EISErrorResponseDetails](errorRead(response.status)))
        }
      }
      .andThen { case _ => timer.stop() }
      .recover { case NonFatal(ex) =>
        logger.warn(EISErrorMessage(createdDateTime, ex.getMessage, correlationId, messageType), ex)

        Left(
          EISErrorResponseDetails(
            INTERNAL_SERVER_ERROR,
            timestamp,
            "Internal server error",
            "Unexpected error occurred while processing Submission request",
            correlationId
          )
        )
      }
  }

  private def wrapXmlInControlDocument(messageIdentifier: String, innerXml: String, ern: String): NodeSeq =
    <con:Control xmlns:con="http://www.govtalk.gov.uk/taxation/InternationalTrade/Common/ControlDocument">
      <con:MetaData>
        <con:MessageId>
          {messageIdentifier}
        </con:MessageId>
        <con:Source>
          {Headers.APIPSource}
        </con:Source>
      </con:MetaData>
      <con:OperationRequest>
        <con:Parameters>
          <con:Parameter Name="ExciseRegistrationNumber">{ern}</con:Parameter>
          <con:Parameter Name="message">
            {scala.xml.PCData(innerXml)}
          </con:Parameter>
        </con:Parameters>
        <con:ReturnData>
          <con:Data Name="schema"/>
        </con:ReturnData>
      </con:OperationRequest>
    </con:Control>
}
