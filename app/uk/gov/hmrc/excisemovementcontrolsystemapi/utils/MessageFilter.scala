package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import generated.NewMessagesDataResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

import java.time.Instant
import javax.inject.Inject

class MessageFilter @Inject()
(
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils
) {

  def filter(encodeMessage: ShowNewMessageResponse, lrn: String): Seq[Message] = {

    val xml = emcsUtils.decode(encodeMessage.message)
    Seq(Message("message", "any", dateTimeService.now))
  }

  def extractMessages(encodedMessage: String): Seq[IEMessage] = {
    val decodedMessage: String = emcsUtils.decode(encodedMessage.message)

    getNewMessageDataResponse(decodedMessage)
      .Messages.messagesoption.map(factory.createIEMessage(_))
  }

  private def getNewMessageDataResponse(decodedMessage: String) = {
    scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(decodedMessage))
  }
}
