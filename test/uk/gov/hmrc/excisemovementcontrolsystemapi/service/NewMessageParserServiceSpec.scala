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

import java.nio.charset.StandardCharsets
import java.util.Base64

class NewMessageParserServiceSpec
  extends PlaySpec
    with NewMessagesXml {

  private val messageFactory = mock[IEMessageFactory]
  private val parser = new NewMessageParserService(messageFactory, new EmcsUtils())

  "countOfMessagesAvailable" should {
    "return the number of messages" in {
      val encodeGetNewMessage = Base64.getEncoder.encodeToString(
        newMessageWith2IE801sXml.toString.getBytes(StandardCharsets.UTF_8)
      )

      parser.countOfMessagesAvailable(encodeGetNewMessage) mustBe 2
    }
  }

  "extractMessages" should {
    "extract all messages" in {

      val message1 = mock[IEMessage]
      val message2 = mock[IEMessage]

      val encodeGetNewMessage = Base64.getEncoder.encodeToString(
        newMessageWith818And802.toString.getBytes(StandardCharsets.UTF_8)
      )
      when(messageFactory.createIEMessage(any)).thenReturn(message1, message2)

      parser.extractMessages(encodeGetNewMessage) mustBe Seq(message1, message2)
      verify(messageFactory, times(2)).createIEMessage(any)
    }
  }
}

