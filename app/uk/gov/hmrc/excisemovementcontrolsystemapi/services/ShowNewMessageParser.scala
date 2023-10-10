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

import generated.{MessagesOption, NewMessagesDataResponse}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EisUtils, MessageTypes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.xml.{Elem, TopScope}

class ShowNewMessageParser @Inject()(eisUtils: EisUtils) {
  def parseEncodedMessage(encodedMessage: String): Seq[Message] = {

    val decodedMessage = new String(
      eisUtils.createDecoder.decode(encodedMessage),
      StandardCharsets.UTF_8
    )

    val xml = scala.xml.XML.loadString(decodedMessage)

    (xml \\ "NewMessagesDataResponse" \\ "Messages").flatMap { o => o.child }
      .flatMap { _ match {
        case e: Elem =>
          e.copy(scope = TopScope)
        case _ => Seq.empty
      }}
      .filterNot(_.isEmpty)
      .map(message => {
        val encodeMessage = eisUtils.createEncoder.encodeToString(message.toString.getBytes(StandardCharsets.UTF_8))
        val messageType = message.toString.substring(message.toString.indexOf("IE"), message.toString().indexOf(" "))
        Message(encodeMessage = encodeMessage, messageType = messageType)
      })
  }


  def parseEncodedMessage1(encodedMessage: String): Seq[Message] = {

    val decodedMessage = new String(
      eisUtils.createDecoder.decode(encodedMessage),
      StandardCharsets.UTF_8
    )

    val newMessage: NewMessagesDataResponse = scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(decodedMessage))
    newMessage.Messages.messagesoption.map((o: DataRecord[MessagesOption]) => {
      val namespace =  o.namespace.fold(generated.defaultScope.uri)(o => o)
      val xml = scalaxb.DataRecord.toXML(o, None, o.key, scalaxb.toScope(o.key -> namespace), true)
      val encodedXml = eisUtils.createEncoder.encodeToString(xml.toString().getBytes(StandardCharsets.UTF_8))

      Message(encodedXml, MessageTypes.withValue(o.key.getOrElse("unknown")).value)
    })
  }

  def countOfMessagesAvailable(encodedMessage: String): Long ={
    val decodedMessage = new String(eisUtils.createDecoder.decode(encodedMessage),  StandardCharsets.UTF_8)
    val newMessage = scala.xml.XML.loadString(decodedMessage)
    (newMessage \ "CountOfMessagesAvailable").text.toLong
  }
}

