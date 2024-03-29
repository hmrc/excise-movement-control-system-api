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

import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.StringSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

class MessageFilterSpec extends PlaySpec
  with StringSupport
  with NewMessagesXml {

  private val dateTimeService = mock[DateTimeService]
  private val emcsUtils = new EmcsUtils
  private val messageFactory = new IEMessageFactory

  private val timestamp = Instant.parse("2018-11-30T18:35:24.001234Z")
  when(dateTimeService.timestamp()).thenReturn(timestamp)

  "filter" should {
    "filter a message by LRN when multiple LRNs are in the NewMessagesXml" in {

      val messageFilter = new MessageFilter(dateTimeService, emcsUtils = emcsUtils, factory = messageFactory)

      val xml = scala.xml.XML.loadString(newMessageWith2IE801sXml.toString())
      val encodeXml = Base64.getEncoder.encodeToString(xml.toString.getBytes(StandardCharsets.UTF_8))

      val message: EISConsumptionResponse = EISConsumptionResponse(Instant.now(), "123", encodeXml)

      val result = messageFilter.filter(message, "token")

      result.size mustBe 1

      val v = "(IE|ie)801>$".r
      v.findAllMatchIn(decodeAndCleanUpMessage(result).head).toList.size mustBe 1
      result.head.messageType mustBe MessageTypes.IE801.value
    }

    "filter when no matching messages for lrn" in {

      val messageFilter = new MessageFilter(dateTimeService, emcsUtils = emcsUtils, factory = messageFactory)

      val xml = scala.xml.XML.loadString(newMessageWith2IE801sXml.toString())
      val encodeXml = Base64.getEncoder.encodeToString(xml.toString.getBytes(StandardCharsets.UTF_8))

      val message: EISConsumptionResponse = EISConsumptionResponse(Instant.now(), "123", encodeXml)

      val result = messageFilter.filter(message, "newLRN")

      result mustBe Seq.empty

    }

    "return nothing when no messages returned from eis" in {

      val messageFilter = new MessageFilter(dateTimeService, emcsUtils = emcsUtils, factory = messageFactory)

      val xml = scala.xml.XML.loadString(emptyNewMessageDataXml.toString())
      val encodeXml = Base64.getEncoder.encodeToString(xml.toString.getBytes(StandardCharsets.UTF_8))

      val message: EISConsumptionResponse = EISConsumptionResponse(Instant.now(), "123", encodeXml)

      val result = messageFilter.filter(message, "token")

      result mustBe Seq.empty

    }

  }

  private def decodeAndCleanUpMessage(messages: Seq[Message]): Seq[String] = {
    val decoder = Base64.getDecoder
    messages
      .map(o => new String(decoder.decode(o.encodedMessage), StandardCharsets.UTF_8))
      .map(clean)
  }
}
