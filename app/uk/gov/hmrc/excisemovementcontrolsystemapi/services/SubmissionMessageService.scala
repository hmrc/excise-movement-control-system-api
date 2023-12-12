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
import play.api.mvc.Result
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class SubmissionMessageServiceImpl @Inject()(
  connector: EISSubmissionConnector,
  nrsService: NrsService,
) (implicit val ec: ExecutionContext) extends SubmissionMessageService {

  def submit(
    request: ValidatedXmlRequest[_]
  )(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]] = {

    connector.submitMessage(request).map {
      case Right(response) =>
        nrsService.submitNrs(request)
        Right(response)
      case error => error
    }
  }
}

@ImplementedBy(classOf[SubmissionMessageServiceImpl])
trait SubmissionMessageService {
  def submit(
    request: ValidatedXmlRequest[_]
  )(implicit hc: HeaderCarrier): Future[Either[Result, EISSubmissionResponse]]
}
