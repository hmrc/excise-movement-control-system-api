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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EISSubmissionConnector @Inject()
(
  httpClient: HttpClient,
  emcsUtils: EmcsUtils,
  appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) extends EISSubmissionHeader with Logging {

  def submitMessage(request: ValidatedXmlRequest[_])(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]] = {

    val timer = metrics.defaultRegistry.timer("emcs.submission.connector.timer").time()

    //todo: add retry
    val correlationId = emcsUtils.generateCorrelationId
    val createdDateTime = emcsUtils.getCurrentDateTimeString
    val encodedMessage = emcsUtils.createEncoder.encodeToString(request.body.toString.getBytes(StandardCharsets.UTF_8))
    val messageType = request.parsedRequest.ieMessage.messageType
    val eisRequest = EISRequest(correlationId, createdDateTime, messageType, EmcsSource, "user1", encodedMessage)

    val ern = getSingleErnFromMessage(request.parsedRequest.ieMessage, request.validErns)

    httpClient.POST[EISRequest, Either[Result, EISSubmissionResponse]](
      appConfig.emcsReceiverMessageUrl,
      eisRequest,
      build(correlationId, createdDateTime)
    )(EISRequest.format, EISHttpReader(correlationId, ern, createdDateTime), hc, ec)
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

  /*
    The illegal state exception for IE818 message should never happen here,
    because these should have been caught previously during the validation.

    We are trying to get the ERN to use in the logs here, so want the one that is both in the auth and the message
  */
  private def getSingleErnFromMessage(message: IEMessage, validErns: Set[String]) = {
    message match {
      case x: IE801Message => matchErn(x.consignorId, x.consigneeId, validErns, x.messageType)
      //For 810 & 813 we have no ERN in message so just use auth
      case _: IE810Message => validErns.head
      case _: IE813Message => validErns.head
      case x: IE815Message => x.consignorId
      case x: IE818Message => x.consigneeId.getOrElse(throw new IllegalStateException(s"[EISSubmissionConnector] - ern not supplied for message: ${x.messageType}"))
      case x: IE819Message => x.consigneeId.getOrElse(throw new IllegalStateException(s"[EISSubmissionConnector] - ern not supplied for message: ${x.messageType}"))
      case x: IE837Message => matchErn(x.consignorId, x.consigneeId, validErns, x.messageType)
      case _ => throw new RuntimeException(s"[EISSubmissionConnector] - Unsupported Message Type: ${message.messageType}")
    }
  }

  private def matchErn(
                        consignorId: Option[String],
                        consigneeId: Option[String],
                        erns: Set[String],
                        messageType: String
                      ): String = {
    val messageErn: Set[String] = Set(consignorId, consigneeId).flatten
    val availableErn = erns.intersect(messageErn)

    if (availableErn.nonEmpty) availableErn.head
    else throw new IllegalStateException(s"[EISSubmissionConnector] - ern not supplied for message: $messageType")
  }
}
