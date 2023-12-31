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
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class EISSubmissionConnector @Inject()
(
  httpClient: HttpClient,
  emcsUtils: EmcsUtils,
  appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) extends EISSubmissionHeaders with Logging {

  def submitMessage(request: ValidatedXmlRequest[NodeSeq])(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]] = {

    val timer = metrics.defaultRegistry.timer("emcs.submission.connector.timer").time()

    //todo: add retry
    val correlationId = emcsUtils.generateCorrelationId
    val createdDateTime = emcsUtils.getCurrentDateTimeString
    val wrappedXml = wrapXmlInControlDocument(request.parsedRequest.ieMessage.messageIdentifier, request.body)
    val encodedMessage = emcsUtils.encode(wrappedXml.toString)
    val messageType = request.parsedRequest.ieMessage.messageType

    val ern = emcsUtils.getSingleErnFromMessage(request.parsedRequest.ieMessage, request.validErns)
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
            LocalDateTime.parse(createdDateTime),
            "Exception",
            ex.getMessage,
            correlationId
          )
          Left(InternalServerError(Json.toJson(error)))
      }
  }

  private def wrapXmlInControlDocument(messageIdentifier: String, innerXml: NodeSeq): NodeSeq = {
    <con:Control xmlns:con="http://www.govtalk.gov.uk/taxation/InternationalTrade/Common/ControlDocument">
      <con:MetaData>
        <con:MessageId>{messageIdentifier}</con:MessageId>
        <con:Source>{Headers.APIPSource}</con:Source>
      </con:MetaData>
      <con:OperationRequest>
        <con:Parameters>
          <con:Parameter Name="message">
            {scala.xml.PCData(innerXml.toString)}
          </con:Parameter>
        </con:Parameters>
        <con:ReturnData>
          <con:Data Name="schema"/>
        </con:ReturnData>
      </con:OperationRequest>
    </con:Control>
  }

}
