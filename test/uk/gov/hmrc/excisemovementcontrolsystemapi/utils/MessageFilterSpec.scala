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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, MessageTypes, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDateTime}
import java.util.Base64

class MessageFilterSpec extends PlaySpec {

  private val dateTimeService = mock[DateTimeService]
  private val emcsUtils = new EmcsUtils
  private val messageFactory = new IEMessageFactory

  "filter" should {
    "filter a message by LRN" in {
      val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")
      when(dateTimeService.now).thenReturn(timestamp)

      val messageFilter = new MessageFilter(dateTimeService, emcsUtils = emcsUtils, factory = messageFactory)

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
