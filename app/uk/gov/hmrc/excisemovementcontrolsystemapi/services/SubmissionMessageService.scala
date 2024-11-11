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
import play.api.mvc.Result
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionMessageServiceImpl @Inject() (
  connector: EISSubmissionConnector,
  nrsService: NrsService,
  correlationIdService: CorrelationIdService,
  ernSubmissionRepository: ErnSubmissionRepository
)(implicit val ec: ExecutionContext)
    extends SubmissionMessageService
    with Logging {

  def submit(
    request: ParsedXmlRequest[_],
    authorisedErn: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]] = {

    val correlationId = correlationIdService.generateCorrelationId()

    for {
      submitMessageResponse <-
        connector.submitMessage(request.ieMessage, request.body.toString, authorisedErn, correlationId)
      isSuccess              = submitMessageResponse.isRight
      _                      = if (isSuccess) ernSubmissionRepository.save(authorisedErn)
      _                      = if (isSuccess) nrsService.submitNrsOld(request, authorisedErn, correlationId)
    } yield submitMessageResponse
  }
}

@ImplementedBy(classOf[SubmissionMessageServiceImpl])
trait SubmissionMessageService {
  def submit(
    request: ParsedXmlRequest[_],
    authorisedErn: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]]
}
