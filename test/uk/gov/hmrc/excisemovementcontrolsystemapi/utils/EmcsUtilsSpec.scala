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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE801Message, IE810Message, IE813Message, IE815Message, IE818Message, IE819Message, IE837Message, IEMessage}

import scala.xml.NodeSeq

class EmcsUtilsSpec extends PlaySpec {

  val emcsUtils = new EmcsUtils

  "getSingleErnFromMessage when IE801 with consignor" in {
    val ie801Message = mock[IE801Message]
    when(ie801Message.consignorId).thenReturn(Some("123"))
    when(ie801Message.consigneeId).thenReturn(Some("456"))

    emcsUtils.getSingleErnFromMessage(ie801Message, Set("123")) mustBe "123"

  }

  "getSingleErnFromMessage when IE801 with consignee" in {
    val ie801Message = mock[IE801Message]
    when(ie801Message.consignorId).thenReturn(Some("123"))
    when(ie801Message.consigneeId).thenReturn(Some("456"))

    emcsUtils.getSingleErnFromMessage(ie801Message, Set("456")) mustBe "456"
  }

  "getSingleErnFromMessage when IE810" in {
    val ie810Message = mock[IE810Message]

    emcsUtils.getSingleErnFromMessage(ie810Message, Set("123")) mustBe "123"
  }

  "getSingleErnFromMessage when IE813" in {
    val ie813Message = mock[IE813Message]

    emcsUtils.getSingleErnFromMessage(ie813Message, Set("123")) mustBe "123"
  }

  "getSingleErnFromMessage when IE815" in {
    val ie815Message = mock[IE815Message]
    when(ie815Message.consignorId).thenReturn("123")
    when(ie815Message.consigneeId).thenReturn(Some("456"))

    emcsUtils.getSingleErnFromMessage(ie815Message, Set("123")) mustBe "123"
  }

  "getSingleErnFromMessage when IE818" in {
    val ie818Message = mock[IE818Message]
    when(ie818Message.consigneeId).thenReturn(Some("123"))

    emcsUtils.getSingleErnFromMessage(ie818Message, Set("123")) mustBe "123"
  }

  "getSingleErnFromMessage when IE819" in {
    val ie819Message = mock[IE819Message]
    when(ie819Message.consigneeId).thenReturn(Some("123"))

    emcsUtils.getSingleErnFromMessage(ie819Message, Set("123")) mustBe "123"
  }

  "getSingleErnFromMessage when IE837 with consignor" in {
    val ie837Message = mock[IE837Message]
    when(ie837Message.consignorId).thenReturn(Some("123"))
    when(ie837Message.consigneeId).thenReturn(None)

    emcsUtils.getSingleErnFromMessage(ie837Message, Set("123")) mustBe "123"
  }

  "getSingleErnFromMessage when IE837 with consignee" in {
    val ie837Message = mock[IE837Message]
    when(ie837Message.consignorId).thenReturn(None)
    when(ie837Message.consigneeId).thenReturn(Some("123"))

    emcsUtils.getSingleErnFromMessage(ie837Message, Set("123")) mustBe "123"
  }

  "throw an error if unsupported message" in {
    class NonSupportedMessage extends IEMessage {
      override def consigneeId: Option[String] = None

      override def administrativeReferenceCode: Option[String] = None

      override def messageType: String = "any-type"

      override def toXml: NodeSeq = NodeSeq.Empty

      override def lrnEquals(lrn: String): Boolean = false
    }

    the[RuntimeException] thrownBy
      emcsUtils.getSingleErnFromMessage(new NonSupportedMessage, Set("123")) must
      have message "[EmcsUtils] - Unsupported Message Type: any-type"
  }
}