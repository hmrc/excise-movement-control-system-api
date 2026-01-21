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
import com.fasterxml.jackson.core.JacksonException
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsResultException, Json, Reads}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EISErrorResponseDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse.format
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.NodeSeq

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

  private def enforceCorrelationId(hc: HeaderCarrier): (HeaderCarrier, String) =
    hc.headers(Seq(HttpHeader.xCorrelationId)).headOption match {
      case Some(id) => (hc, id._2)
      case None     =>
        val correlationId = UUID.randomUUID().toString
        logger.info(s"generated new correlation id: $correlationId")
        (hc.withExtraHeaders(HttpHeader.xCorrelationId -> correlationId), correlationId)
    }

  private def internalError(timestamp: Instant, correlationId: String): Either[EISErrorResponseDetails, Nothing] =
    Left(
      EISErrorResponseDetails(
        INTERNAL_SERVER_ERROR,
        timestamp,
        "Internal server error",
        "Unexpected error occurred while processing Submission request",
        correlationId
      )
    )

  private def errorRead(status: Int): Reads[EISErrorResponseDetails] =
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
    authorisedErn: String
  )(implicit hc: HeaderCarrier): Future[Either[EISErrorResponseDetails, EISSubmissionResponse]] = {
    logger.info("[EISSubmissionConnector]: Submitting a message to EIS")
    val (hc2, correlationId) = enforceCorrelationId(hc)
    val timer                = metrics.timer("emcs.submission.connector.timer").time()
    val timestamp            = dateTimeService.timestamp()
    val createdDateTime      = timestamp.asStringInMilliseconds
    val wrappedXml           = wrapXmlInControlDocument(message.messageIdentifier, requestXmlAsString, authorisedErn)
    val messageType          = message.messageType
    val encodedMessage       = emcsUtils.encode(wrappedXml.toString)

    val eisRequest = EISSubmissionRequest(authorisedErn, messageType, encodedMessage)

    httpClient
      .post(url"${appConfig.emcsReceiverMessageUrl}")(hc2)
      .setHeader(build(correlationId, createdDateTime, appConfig.submissionBearerToken): _*)
      .withBody(Json.toJson(eisRequest))
      .execute[HttpResponse]
      .map { response =>
        if (is2xx(response.status)) {
          Right(response.json.as[EISSubmissionResponse])
        } else {
          val errorDetails = response.json.as[EISErrorResponseDetails](errorRead(response.status))
          logger.warn(EISErrorMessage(createdDateTime, s"status: ${errorDetails.status}", correlationId, messageType))
          Left(errorDetails)
        }
      }
      .andThen { case _ => timer.stop() }
      .recover {
        case _: JacksonException =>
          // JSON parsing error
          logger.error(EISErrorMessage.parseError(createdDateTime, correlationId, messageType))
          internalError(timestamp, correlationId)

        case _: JsResultException =>
          // JSON deserialization error
          logger.error(EISErrorMessage.readError(createdDateTime, correlationId, messageType))
          internalError(timestamp, correlationId)

        case NonFatal(ex) =>
          // Something else
          logger.error(EISErrorMessage(createdDateTime, ex.getMessage, correlationId, messageType), ex)
          internalError(timestamp, correlationId)
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
