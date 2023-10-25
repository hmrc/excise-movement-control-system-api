package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageTypes, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDateTime}
import java.util.Base64

class MessageFilterSpec extends PlaySpec {

  private val dateTimeService = mock[DateTimeService]

  "filter" should {
    "filter a message by LRN" in {
      val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")
      when(dateTimeService.now).thenReturn(timestamp)

      val messageFilter = new MessageFilter(dateTimeService)

      val xml = scala.xml.XML.loadString(NewMessagesXml.newMessageWithIE801.toString())
      val encodeXml = Base64.getEncoder.encodeToString(xml.toString.getBytes(StandardCharsets.UTF_8))

      val message: ShowNewMessageResponse = ShowNewMessageResponse(LocalDateTime.now(), "123", encodeXml)

      val result = messageFilter.filter(message, "token")

      result.size mustBe 1

      decodeAndCleanUpMessage(result).head mustBe decodeAndCleanUpMessage(Seq(
        Message(encodeXml, MessageTypes.IE801.value, timestamp))).head
    }
  }

  private def decodeAndCleanUpMessage(messages: Seq[Message]): Seq[String] = {
    val decoder = Base64.getDecoder
    messages
      .map(o => new String(decoder.decode(o.encodedMessage), StandardCharsets.UTF_8))
      .map(cleanUpString(_))
  }

  private def cleanUpString(str: String): String = {
    str.replaceAll("[\\t\\n\\r\\s]+", "")
  }
}
