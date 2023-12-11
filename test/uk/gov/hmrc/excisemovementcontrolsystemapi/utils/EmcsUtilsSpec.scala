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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._

import scala.xml.NodeSeq

class EmcsUtilsSpec extends PlaySpec {

  val emcsUtils = new EmcsUtils

  "getSingleErnFromMessage" should {

    "get ERN" when {

      "IE801 with consignor" in {
        val ie801Message = mock[IE801Message]
        when(ie801Message.consignorId).thenReturn(Some("123"))
        when(ie801Message.consigneeId).thenReturn(Some("456"))

        emcsUtils.getSingleErnFromMessage(ie801Message, Set("123")) mustBe "123"

      }

      "IE801 with consignee" in {
        val ie801Message = mock[IE801Message]
        when(ie801Message.consignorId).thenReturn(Some("123"))
        when(ie801Message.consigneeId).thenReturn(Some("456"))

        emcsUtils.getSingleErnFromMessage(ie801Message, Set("456")) mustBe "456"
      }

      "IE801 throws exception if neither supplied" in {
        val ie801Message = mock[IE801Message]
        when(ie801Message.consigneeId).thenReturn(None)
        when(ie801Message.consignorId).thenReturn(None)
        when(ie801Message.messageType).thenReturn("IE801")

        the[RuntimeException] thrownBy
          emcsUtils.getSingleErnFromMessage(ie801Message, Set("123")) must
          have message "[EmcsUtils] - ern not supplied for IE801 message"
      }

      "IE810" in {
        val ie810Message = mock[IE810Message]

        emcsUtils.getSingleErnFromMessage(ie810Message, Set("123")) mustBe "123"
      }

      "IE813" in {
        val ie813Message = mock[IE813Message]

        emcsUtils.getSingleErnFromMessage(ie813Message, Set("123")) mustBe "123"
      }

      "IE815" in {
        val ie815Message = mock[IE815Message]
        when(ie815Message.consignorId).thenReturn("123")
        when(ie815Message.consigneeId).thenReturn(Some("456"))

        emcsUtils.getSingleErnFromMessage(ie815Message, Set("123")) mustBe "123"
      }

      "IE818" in {
        val ie818Message = mock[IE818Message]
        when(ie818Message.consigneeId).thenReturn(Some("123"))

        emcsUtils.getSingleErnFromMessage(ie818Message, Set("123")) mustBe "123"
      }

      "IE818 throws exception if no consignee supplied" in {
        val ie818Message = mock[IE818Message]
        when(ie818Message.consigneeId).thenReturn(None)

        the[RuntimeException] thrownBy
          emcsUtils.getSingleErnFromMessage(ie818Message, Set("123")) must
          have message "[EmcsUtils] - ern not supplied for IE818 message"
      }

      "IE819" in {
        val ie819Message = mock[IE819Message]
        when(ie819Message.consigneeId).thenReturn(Some("123"))

        emcsUtils.getSingleErnFromMessage(ie819Message, Set("123")) mustBe "123"
      }

      "IE819 throws exception if no consignee supplied" in {
        val ie819Message = mock[IE819Message]
        when(ie819Message.consigneeId).thenReturn(None)

        the[RuntimeException] thrownBy
          emcsUtils.getSingleErnFromMessage(ie819Message, Set("123")) must
          have message "[EmcsUtils] - ern not supplied for IE819 message"
      }

      "IE837 with consignor" in {
        val ie837Message = mock[IE837Message]
        when(ie837Message.consignorId).thenReturn(Some("123"))
        when(ie837Message.consigneeId).thenReturn(None)

        emcsUtils.getSingleErnFromMessage(ie837Message, Set("123")) mustBe "123"
      }

      "IE837 with consignee" in {
        val ie837Message = mock[IE837Message]
        when(ie837Message.consignorId).thenReturn(None)
        when(ie837Message.consigneeId).thenReturn(Some("123"))

        emcsUtils.getSingleErnFromMessage(ie837Message, Set("123")) mustBe "123"
      }

      "IE871" in {
        val ie871Message = mock[IE871Message]
        when(ie871Message.consignorId).thenReturn(Some("123"))

        emcsUtils.getSingleErnFromMessage(ie871Message, Set("123")) mustBe "123"
      }

      "IE819 throws exception if no consignor supplied" in {
        val ie871Message = mock[IE871Message]
        when(ie871Message.consignorId).thenReturn(None)

        the[RuntimeException] thrownBy
          emcsUtils.getSingleErnFromMessage(ie871Message, Set("123")) must
          have message "[EmcsUtils] - ern not supplied for IE871 message"
      }

    }

    "throw an error if unsupported message type" in {
      class NonSupportedMessage extends IEMessage {
        override def consigneeId: Option[String] = None

        override def administrativeReferenceCode: Option[String] = None

        override def messageType: String = "any-type"

        override def messageIdentifier: String = "messageId"

        override def toXml: NodeSeq = NodeSeq.Empty

        override def lrnEquals(lrn: String): Boolean = false
      }

      the[RuntimeException] thrownBy
        emcsUtils.getSingleErnFromMessage(new NonSupportedMessage, Set("123")) must
        have message "[EmcsUtils] - Unsupported Message Type: any-type"
    }
  }

}