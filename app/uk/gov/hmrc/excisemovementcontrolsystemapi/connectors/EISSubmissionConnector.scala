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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.EISHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils, ErnsMapper}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class EISSubmissionConnector @Inject()
(
  httpClient: HttpClient,
  emcsUtils: EmcsUtils,
  appConfig: AppConfig,
  metrics: Metrics,
  ernsMapper: ErnsMapper,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) extends EISSubmissionHeaders with Logging {

  def submitMessage(request: ValidatedXmlRequest[_], correlationId: String)(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]] = {

    val timer = metrics.defaultRegistry.timer("emcs.submission.connector.timer").time()

    val timestamp = dateTimeService.timestamp()
    //todo: add retry
    val createdDateTime = timestamp.toString
    val wrappedXml = wrapXmlInControlDocument(request.message.messageIdentifier, request.body.toString)
    val messageType = request.message.messageType
    val encodedMessage = emcsUtils.encode(wrappedXml.toString)

    val ern = ernsMapper.getSingleErnFromMessage(request.message, request.validErns)
    val eisRequest = EISSubmissionRequest(ern, messageType, encodedMessage)

    httpClient.POST[EISSubmissionRequest, Either[Result, EISSubmissionResponse]](
        appConfig.emcsReceiverMessageUrl,
        eisRequest,
        build(correlationId, createdDateTime, appConfig.submissionBearerToken)
      )(EISSubmissionRequest.format, EISHttpReader(correlationId, ern, createdDateTime), hc, ec)
      .andThen { case _ => timer.stop() }
      .recover {
        case ex: Throwable =>

          logger.warn(EISErrorMessage(createdDateTime, ern, ex.getMessage, correlationId, messageType), ex)

          val error = EISErrorResponse(
            timestamp,
            "INTERNAL_SERVER_ERROR",
            "Exception",
            ex.getMessage,
            correlationId
          )
          Left(InternalServerError(Json.toJson(error)))
      }
  }

  private def wrapXmlInControlDocument(messageIdentifier: String, innerXml: String): NodeSeq = {
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
}
