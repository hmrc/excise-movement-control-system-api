package uk.gov.hmrc.excisemovementcontrolsystemapi.service

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NewMessageParserService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.nio.charset.StandardCharsets
import java.util.Base64

class ShowNewMessageParserServiceSpec
  extends PlaySpec {

  private val messageFactory = mock[IEMessageFactory]
  private val timeService = mock[DateTimeService]
  private val parser = new NewMessageParserService(messageFactory, new EmcsUtils())

  "countOfMessagesAvailable" should {
    "return the number of messages" in {
      val encodeGetNewMessage = Base64.getEncoder.encodeToString(
        NewMessagesXml.newMessageWith2IE801sXml.toString.getBytes(StandardCharsets.UTF_8)
      )

      parser.countOfMessagesAvailable(encodeGetNewMessage) mustBe 2
    }
  }

  "extractMessages" should {
    "extract all messages" in {

      val message1 = mock[IEMessage]
      val message2 = mock[IEMessage]

      val encodeGetNewMessage = Base64.getEncoder.encodeToString(
        "<ie801>message</ie801>".toString.getBytes(StandardCharsets.UTF_8)
      )
      when(messageFactory.createIEMessage(any)).thenReturn(message1, message2)

      parser.extractMessages(encodeGetNewMessage) mustBe Seq(message1, message2)
      verify(messageFactory, times(2)).createIEMessage(any)
    }
  }
}

