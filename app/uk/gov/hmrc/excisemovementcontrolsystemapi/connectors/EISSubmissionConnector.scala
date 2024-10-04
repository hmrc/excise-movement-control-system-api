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

import play.api.Logging
import play.api.libs.concurrent.Futures
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.EISReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils._
import uk.gov.hmrc.http.HttpErrorFunctions.{is2xx, is4xx}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.xml.NodeSeq

class EISSubmissionConnector @Inject() (
  httpClient: HttpClientV2,
  emcsUtils: EmcsUtils,
  appConfig: AppConfig,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext, val futures: Futures)
    extends EISSubmissionHeaders
    with Logging
    with Retrying {

  def submitMessage(
    message: IEMessage,
    requestXmlAsString: String,
    authorisedErn: String,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]] = {
    logger.info("[EISSubmissionConnector]: Submitting a message to EIS")

    val timestamp       = dateTimeService.timestamp()
    val createdDateTime = timestamp.asStringInMilliseconds
    val wrappedXml      = wrapXmlInControlDocument(message.messageIdentifier, requestXmlAsString, authorisedErn)
    val messageType     = message.messageType
    val encodedMessage  = emcsUtils.encode(wrappedXml.toString)
    val eisRequest      = EISSubmissionRequest(authorisedErn, messageType, encodedMessage)

    val reader = EISReader(
      correlationId = correlationId,
      ern = authorisedErn,
      createDateTime = createdDateTime,
      dateTimeService = dateTimeService,
      messageType = messageType
    )
    for {
      response    <- send(correlationId, createdDateTime, eisRequest)
      readResponse = reader.read(response)
    } yield readResponse
  }

  private def send(correlationId: String, createdDateTime: String, eisRequest: EISSubmissionRequest)(implicit
    hc: HeaderCarrier
  )                                                                                                       =
    retry(appConfig.eisRetryDelays.toList, canRetry, appConfig.getNrsSubmissionUrl) {
      httpClient
        .post(url"${appConfig.emcsReceiverMessageUrl}")
        .setHeader(build(correlationId, createdDateTime, appConfig.submissionBearerToken): _*)
        .withBody(Json.toJson(eisRequest))
        .execute[HttpResponse]
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
          <con:Parameter Name="ExciseRegistrationNumber">
            {ern}
          </con:Parameter>
          <con:Parameter Name="message">
            {scala.xml.PCData(innerXml)}
          </con:Parameter>
        </con:Parameters>
        <con:ReturnData>
          <con:Data Name="schema"/>
        </con:ReturnData>
      </con:OperationRequest>
    </con:Control>

  private def canRetry(response: Try[HttpResponse]): Boolean =
    response match {
      case Success(r) => !(is2xx(r.status) || is4xx(r.status))
      case _          => true
    }
}
