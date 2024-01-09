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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NonRepudiationSubmission
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class SubmissionMessageServiceImpl @Inject()(
  connector: EISSubmissionConnector,
  nrsService: NrsService,
  emcsUtils: EmcsUtils,
) (implicit val ec: ExecutionContext) extends SubmissionMessageService with Logging {


  def submit(
    request: ValidatedXmlRequest[_]
  )(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]] = {

    val correlationId = emcsUtils.generateCorrelationId

    for {
      submitMessageResponse <- connector.submitMessage(request, correlationId)
      isSuccess = submitMessageResponse.isRight
      _ = if(isSuccess) sendToNrs(request, correlationId)
      } yield submitMessageResponse
  }

  private def sendToNrs(
    request: ValidatedXmlRequest[_],
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[Option[NonRepudiationSubmission]] = {
    nrsService.submitNrs(request, correlationId).transformWith {
      case Success(value) => Future.successful(Some(value))
      case _ => Future.successful(None)
    }
  }
}

@ImplementedBy(classOf[SubmissionMessageServiceImpl])
trait SubmissionMessageService {
  def submit(
    request: ValidatedXmlRequest[_]
  )(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]]
}
