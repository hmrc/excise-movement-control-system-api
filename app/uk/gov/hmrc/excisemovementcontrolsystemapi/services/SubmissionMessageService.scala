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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.google.inject.ImplementedBy
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EISErrorResponseDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubmissionMessageServiceImpl @Inject() (
  connector: EISSubmissionConnector,
  nrsServiceNew: NrsServiceNew,
  correlationIdService: CorrelationIdService,
  ernSubmissionRepository: ErnSubmissionRepository
)(implicit val ec: ExecutionContext)
    extends SubmissionMessageService
    with Logging {

  def submit(
    request: ParsedXmlRequest[_],
    authorisedErn: String
  )(implicit hc: HeaderCarrier): Future[Either[EISErrorResponseDetails, EISSubmissionResponse]] = {

    val correlationId = hc
      .headers(Seq(HttpHeader.xCorrelationId))
      .find(_._1 == HttpHeader.xCorrelationId)
      .map(_._2)
      .getOrElse(correlationIdService.generateCorrelationId())

    for {
      submitMessageResponse <- connector.submitMessage(request.ieMessage, request.body.toString, authorisedErn, correlationId)
      isSuccess              = submitMessageResponse.isRight
      _                      = if (isSuccess) ernSubmissionRepository.save(authorisedErn).recover {
                                  case NonFatal(error) => logger.warn(s"Failed to save ERN to ERNSubmissionRepository", error)
                               }
      _                      = if (isSuccess) nrsServiceNew.makeWorkItemAndQueue(request, authorisedErn)
    } yield submitMessageResponse
  }
}

@ImplementedBy(classOf[SubmissionMessageServiceImpl])
trait SubmissionMessageService {
  def submit(
    request: ParsedXmlRequest[_],
    authorisedErn: String
  )(implicit hc: HeaderCarrier): Future[Either[EISErrorResponseDetails, EISSubmissionResponse]]
}
