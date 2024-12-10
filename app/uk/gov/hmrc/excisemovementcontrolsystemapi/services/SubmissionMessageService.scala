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
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EISErrorResponseDetails, EisErrorResponsePresentation}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubmissionMessageServiceImpl @Inject() (
  connector: EISSubmissionConnector,
  nrsService: NrsService,
  nrsServiceNew: NrsServiceNew,
  correlationIdService: CorrelationIdService,
  ernSubmissionRepository: ErnSubmissionRepository,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext)
    extends SubmissionMessageService
    with Logging {

  def submit(
    request: ParsedXmlRequest[_],
    authorisedErn: String
  )(implicit hc: HeaderCarrier): Future[Either[EISErrorResponseDetails, EISSubmissionResponse]] = {

    val correlationId = correlationIdService.generateCorrelationId()

    for {
      submitMessageResponse <-
        connector.submitMessage(request.ieMessage, request.body.toString, authorisedErn, correlationId)
      isSuccess              = submitMessageResponse.isRight
      _                      = if (isSuccess) ernSubmissionRepository.save(authorisedErn).recover { case NonFatal(error) =>
                                 logger.warn(s"Failed to save ERN to ERNSubmissionRepository", error)
                               }
      _                      = if (isSuccess) {
                                 if (appConfig.nrsNewEnabled) nrsServiceNew.makeWorkItemAndQueue(request, authorisedErn)
                                 else nrsService.submitNrsOld(request, authorisedErn, correlationId)
                               }
    } yield submitMessageResponse

//    for {
//      submitMessageResponse <- connector.submitMessage(request.ieMessage, request.body.toString, authorisedErn, correlationId)
//      _ <- if (submitMessageResponse.isRight) {
//        ernSubmissionRepository.save(authorisedErn).recover { case NonFatal(error) =>
//          logger.warn(s"Failed to save ERN to ERNSubmissionRepository", error)
//        }.flatMap(_ => nrsService.submitNrsOld(request, authorisedErn, correlationId))
//      } else {
//        logger.warn(s"Failed to submit message")
//        Future.successful(())
//      }
//    } yield submitMessageResponse

  }
}
// if we have failed to submit it to the backend, the user hasn't submitted. NRS isn't for the user to prove to us. No point in recording things that failed
// Us proving that something was submitted to our HOD.
@ImplementedBy(classOf[SubmissionMessageServiceImpl])
trait SubmissionMessageService {
  def submit(
    request: ParsedXmlRequest[_],
    authorisedErn: String
  )(implicit hc: HeaderCarrier): Future[Either[EISErrorResponseDetails, EISSubmissionResponse]]
}
