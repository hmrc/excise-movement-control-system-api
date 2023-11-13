package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import generated.NewMessagesDataResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage

import java.nio.charset.StandardCharsets
import javax.inject.Inject

class NewMessageParserService @Inject()
(
  factory: IEMessageFactory,
  emcsUtils: EmcsUtils
) {


  def countOfMessagesAvailable(encodedMessage: String): Long ={
    val newMessage = scala.xml.XML.loadString(emcsUtils.decode(encodedMessage))
    (newMessage \ "CountOfMessagesAvailable").text.toLong
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


