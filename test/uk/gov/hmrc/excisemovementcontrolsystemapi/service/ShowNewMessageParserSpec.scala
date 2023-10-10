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

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.GetNewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.Ie704XmlMessage.IE704
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.Ie801XmlMessage.IE801
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.Ie802XmlMessage.IE802
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.ShowNewMessageParser

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.xml.Elem

class ShowNewMessageParserSpec
  extends PlaySpec
    with GetNewMessagesXml {

  "parseEncodedMessage" should {
    "parse the message" in {
      val encodeGetNewMessage = Base64.getEncoder.encodeToString(newMessageXml.toString.getBytes(StandardCharsets.UTF_8))

      val result = new ShowNewMessageParser(new EisUtils()).parseEncodedMessage1(encodeGetNewMessage)

      result.size mustBe 3
      assertResults(result, IE704, "IE704", 0)
      assertResults(result, IE801, "IE801", 1)
      assertResults(result, IE802, "IE802", 2)
    }
  }

  private def assertResults(actual: Seq[Message], IE704: Elem, messageType: String, index: Int) =
  {
    val actualMessage = new String(Base64.getDecoder.decode(actual(index).encodeMessage),
      StandardCharsets.UTF_8).replaceAll("[\\t\\n\\r\\s]+", "")
    actualMessage mustBe IE704.toString.replaceAll("[\\t\\n\\r\\s]+", "")
    actual(index).messageType mustBe messageType
  }
}
