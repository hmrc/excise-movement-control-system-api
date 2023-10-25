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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import generated.NewMessagesDataResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

import java.time.Instant
import javax.inject.Inject

class MessageFilter @Inject()
(
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils,
  factory: IEMessageFactory
) {

  def filter(encodedMessage: ShowNewMessageResponse, lrnToFilterBy: String): Seq[Message] = {

//    val xml = emcsUtils.decode(encodeMessage.message)
//    val messages = Seq(Message("message", "any", dateTimeService.now))

    val unfilteredMessages = extractMessages(encodedMessage.message)
    val filteredMessages = unfilteredMessages.filter(m => m.localReferenceNumber.contains(lrnToFilterBy))


    filteredMessages.map(m => {
      val encodedMessage = emcsUtils.encode(m.toXml.toString())
      Message(encodedMessage, m.getType, dateTimeService.now)
    })

  }

  def extractMessages(encodedMessage: String): Seq[IEMessage] = {
    val decodedMessage: String = emcsUtils.decode(encodedMessage)

    getNewMessageDataResponse(decodedMessage)
      .Messages.messagesoption.map(factory.createIEMessage(_))
  }

  private def getNewMessageDataResponse(decodedMessage: String) = {
    scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(decodedMessage))
  }
}
