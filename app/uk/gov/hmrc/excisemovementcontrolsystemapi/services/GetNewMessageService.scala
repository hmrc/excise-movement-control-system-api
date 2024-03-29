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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageReceiptConnector, ShowNewMessagesConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageReceiptResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetNewMessageServiceImpl @Inject()(
  showNewMessageConnector: ShowNewMessagesConnector,
  messageReceiptConnector: MessageReceiptConnector,
  newMessageParserService: NewMessageParserService
)(implicit val ec: ExecutionContext )extends GetNewMessageService with Logging {

  def getNewMessages(
                                    exciseNumber: String
                                  )(implicit hc: HeaderCarrier): Future[Option[(EISConsumptionResponse, Long)]] = {
    showNewMessageConnector.get(exciseNumber).flatMap(response =>
      response.fold(
        _ => Future.successful(None),
        success => handleSuccess(exciseNumber, success)
      )
    )
  }

  def acknowledgeMessage(
    exciseNumber: String
    )(implicit hc: HeaderCarrier): Future[MessageReceiptResponse] = {
    messageReceiptConnector.put(exciseNumber)
  }

  private def handleSuccess(
                             exciseNumber: String,
                             newMessageResponse: EISConsumptionResponse
                           ): Future[Option[(EISConsumptionResponse, Long)]] = {

    val messageCount = newMessageParserService.countOfMessagesAvailable(newMessageResponse.message)
    val hasMessage = messageCount  > 0

    if(!hasMessage) {
      logger.info(s"[GetNewMessageService] - No new messages available for Excise Registration Number: $exciseNumber")
      Future.successful(Some((newMessageResponse,0)))
    } else {
        logger.debug(s"[GetNewMessageService] - $messageCount messages available for Excise Registration Number: $exciseNumber")
        Future.successful(Some((newMessageResponse, messageCount)))
    }
  }
}

@ImplementedBy(classOf[GetNewMessageServiceImpl])
trait GetNewMessageService {
  def getNewMessages(exciseNumber: String)(implicit hc: HeaderCarrier): Future[Option[(EISConsumptionResponse, Long)]]
  def acknowledgeMessage(exciseNumber: String)(implicit hc: HeaderCarrier): Future[MessageReceiptResponse]
}

