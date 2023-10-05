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
import play.api.mvc.Results.NotFound
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageReceiptConnector, ShowNewMessagesConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ShowNewMessageResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetNewMessageServiceImpl @Inject()(
    showNewMessageConnector: ShowNewMessagesConnector,
    messageReceiptConnector: MessageReceiptConnector
)(implicit val ec: ExecutionContext )extends GetNewMessageService with Logging {

  def getNewMessagesAndAcknowledge(
    exciseNumber: String
  )(implicit hc: HeaderCarrier): Future[Either[Result, ShowNewMessageResponse]] = {
    showNewMessageConnector.get(exciseNumber).flatMap(response =>
      response.fold(
        error => Future.successful(Left(error)),
        success => handleSuccess(exciseNumber, success)
      )
    )
  }

  //todo: We only return success (and so cahce the message) if a message-receipt is successful.
  // There may be problem here as if the message-receipt fails between
  // between MDTP and EIS (but was successfule between (EIS and EMCS Core)
  // then the message would be deleted in EMCS core, but we do not cache them
  // so that messages will be lost.
  private def handleSuccess(
    exciseNumber: String,
    success: ShowNewMessageResponse
  )(implicit hc: HeaderCarrier): Future[Either[Result,ShowNewMessageResponse]] = {
    if (success.message.isEmpty) {
      logger.warn(s"No more new message available for Excise Registration Number: $exciseNumber")
      Future.successful(Left(NotFound(s"No more new message available for Excise Registration Number: $exciseNumber")))
    } else {
      messageReceiptConnector.put(exciseNumber).map {
        case Right(_) => Right(success)
        case Left(receiptError) => Left(receiptError)
      }
    }
  }
}

@ImplementedBy(classOf[GetNewMessageServiceImpl])
trait GetNewMessageService {
  def getNewMessagesAndAcknowledge(exciseNumber: String)(implicit hc: HeaderCarrier): Future[Either[Result,ShowNewMessageResponse]]
}
