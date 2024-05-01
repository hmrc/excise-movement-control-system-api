/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.syntax.all._
import org.apache.pekko.Done
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageReceiptConnector, ShowNewMessagesConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageService @Inject
(
  movementRepository: MovementRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  showNewMessagesConnector: ShowNewMessagesConnector,
  messageReceiptConnector: MessageReceiptConnector,
  newMessageParserService: NewMessageParserService,
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils
)(implicit executionContext: ExecutionContext){

  def updateMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] = {
    for {
      _ <- ernRetrievalRepository.getLastRetrieved(ern)
      response <- getNewMessages(ern)
      messages <- convertMessageResponse(response)
      _ <- updateMovements(ern, messages)
      _ <- ernRetrievalRepository.save(ern)
    } yield Done
  }

  private def updateMovements(ern: String, messages: Seq[IEMessage]): Future[Done] = {
    if (messages.nonEmpty) {
      movementRepository.getAllBy(ern).flatMap { movements =>
        movements.traverse { movement =>
          val updatedMovement = movement.copy(messages = messages.map(m => convertMessage(m)))
          movementRepository.updateMovement(updatedMovement)
        }
      }
    }.as(Done) else {
      Future.successful(Done)
    }
  }

  private def getNewMessages(ern: String)(implicit hc: HeaderCarrier): Future[EISConsumptionResponse] = {
    showNewMessagesConnector.get(ern).flatMap {
      case Right(response) => Future.successful(response)
      case Left(_) => Future.failed(new RuntimeException("error getting new messages"))
    }
  }

  private def convertMessageResponse(response: EISConsumptionResponse): Future[Seq[IEMessage]] = {
    Future.successful(newMessageParserService.extractMessages(response.message))
  }

  private def convertMessage(input: IEMessage): Message = {
    Message(
      encodedMessage = emcsUtils.encode(input.toXml.toString),
      messageType = input.messageType,
      messageId = input.messageIdentifier,
      createdOn = dateTimeService.timestamp()
    )
  }
}
