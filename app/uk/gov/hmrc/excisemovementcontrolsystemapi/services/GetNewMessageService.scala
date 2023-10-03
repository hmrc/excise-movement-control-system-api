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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.ShowNewMessagesConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ShowNewMessageResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetNewMessageServiceImpl @Inject()
(
    showNewMessageConnector: ShowNewMessagesConnector
)(implicit val ec: ExecutionContext )extends GetNewMessageService with Logging {

  def getNewMessages(exciseNumber: String)(implicit hc: HeaderCarrier): Future[Either[Result, ShowNewMessageResponse]] = {
    showNewMessageConnector.get(exciseNumber).map {
      case Right(response) => Right(response)
      case other => other
    }
  }


}

@ImplementedBy(classOf[GetNewMessageServiceImpl])
trait GetNewMessageService {
  def getNewMessages(exciseNumber: String)(implicit hc: HeaderCarrier): Future[Either[Result, ShowNewMessageResponse]]
}
