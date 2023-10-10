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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ShowNewMessageResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetNewMessageServiceImpl @Inject()(
    showNewMessageConnector: ShowNewMessagesConnector,
    messageReceiptConnector: MessageReceiptConnector,
    showNewMessageParser: ShowNewMessageParser
)(implicit val ec: ExecutionContext )extends GetNewMessageService with Logging {

  def getNewMessagesAndAcknowledge(
    exciseNumber: String
  )(implicit hc: HeaderCarrier): Future[Option[ShowNewMessageResponse]] = {
    showNewMessageConnector.get(exciseNumber).flatMap(response =>
      response.fold(
        _ => Future.successful(None),
        success =>
          //Todo store message into mongo. Do not store duplicated
          handleSuccess(exciseNumber, success)
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
    newMessageResponse: ShowNewMessageResponse
  )(implicit hc: HeaderCarrier): Future[Option[ShowNewMessageResponse]] = {
    //todo check CountOfMessagesAvailable as message return an xml with no messages

    val hasMessage = showNewMessageParser.countOfMessagesAvailable(newMessageResponse.message) > 0

    if(!hasMessage) {
      logger.warn(s"No more new message available for Excise Registration Number: $exciseNumber")
      Future.successful(None)
    } else {
      // todo: Acknowledge all the time?
      messageReceiptConnector.put(exciseNumber).map {
        case Right(_) => Some(newMessageResponse)
        case Left(_) if hasMessage => Some(newMessageResponse)
        case Left(_) => None
      }
    }
  }
}

@ImplementedBy(classOf[GetNewMessageServiceImpl])
trait GetNewMessageService {
  def getNewMessagesAndAcknowledge(exciseNumber: String)(implicit hc: HeaderCarrier): Future[Option[ShowNewMessageResponse]]
}
