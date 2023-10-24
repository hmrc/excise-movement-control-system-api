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

import generated.NewMessagesDataResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage

import java.nio.charset.StandardCharsets
import javax.inject.Inject

class ShowNewMessageParser @Inject()
(
  factory: IEMessageFactory,
  timeService: DateTimeService,
  eisUtils: EisUtils) {


  def countOfMessagesAvailable(encodedMessage: String): Long ={
    val newMessage = scala.xml.XML.loadString(decodeMessage(encodedMessage))
    (newMessage \ "CountOfMessagesAvailable").text.toLong
  }

  def extractMessages(encodedMessage: String): Seq[IEMessage] = {
    val decodedMessage: String = decodeMessage(encodedMessage)

    getNewMessageDataResponse(decodedMessage)
      .Messages.messagesoption.map(factory.createIEMessage(_))
  }

  private def getNewMessageDataResponse(decodedMessage: String) = {
    scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(decodedMessage))
  }

  private def decodeMessage(encodedMessage: String) = {
    new String(
      eisUtils.createDecoder.decode(encodedMessage),
      StandardCharsets.UTF_8
    )
  }
}

