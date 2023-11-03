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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{ParsedXmlRequest, ValidatedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE801Message, IE815Message, IE818Message}
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

    //TODO: remember to rename this
    val timer = metrics.defaultRegistry.timer("emcs.eiscontroller.timer").time()

    //todo: add retry
    val correlationId = emcsUtils.generateCorrelationId
    val createdDateTime = emcsUtils.getCurrentDateTimeString
    val encodedMessage = emcsUtils.createEncoder.encodeToString(request.body.toString.getBytes(StandardCharsets.UTF_8))
    val messageType = request.parsedRequest.ieMessage.messageType
    val eisRequest = EISRequest(correlationId, createdDateTime, messageType, EmcsSource, "user1", encodedMessage)
    //TODO: .head bad in case empty

    //Store the valid Set from action
    //Check if consignor is in set
    //If not, check consignee
    // If not, ????

//    val actualErn = {
//      request.ieMessage match {
//        case x: IE815Message => if (matchedErns.contains(x.consignorId)) {
//          x.consignorId
//        } else if (matchedErns.contains(x.consigneeId.get)) { //TODO not this
//          x.consigneeId.get
//        } else {
//          //TODO exception?
//        }
//
//        case _ =>
//      }
//
    //val ern = {
      //request.parsedRequest.erns.intersect(request.parsedRequest.ieMessage.getErns).head
    //}

    val ern = {
      request.parsedRequest.ieMessage match {
        case x: IE801Message if (request.validErns.contains(x.consignorId.getOrElse("TODO"))) => x.consignorId.get
        case x: IE801Message if (request.validErns.contains(x.consigneeId.getOrElse("TODO"))) => x.consigneeId.get
        case x: IE815Message => x.consignorId
        case x: IE818Message => x.consigneeId.getOrElse("TODO")
        case _ => ???
      }
    }

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
}
