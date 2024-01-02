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

package uk.gov.hmrc.excisemovementcontrolsystemapi.factories

import generated.MessagesOption
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._

class IEMessageFactorySpec
  extends PlaySpec
    with TestXml
    with BeforeAndAfterEach {

  private val message = mock[DataRecord[MessagesOption]]
  private val sut = IEMessageFactory()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(message)

    when(message.value).thenReturn(mock[MessagesOption])
  }

  "createIEMessage" should {

    "return throw an error" when {
      "cannot handle message type" in {
        when(message.key).thenReturn(Some("Anything"))
        intercept[RuntimeException] {
          sut.createIEMessage(message)
        }.getMessage mustBe s"[IEMessageFactory] - Could not create Message object. Unsupported message: Anything"
      }

      "messageType is empty" in {
        when(message.key).thenReturn(None)
        intercept[RuntimeException] {
          sut.createIEMessage(message)
        }.getMessage mustBe "[IEMessageFactory] - Could not create Message object. Message type is empty"
      }
    }

    "return an instance of IE704Message" in {
      when(message.key).thenReturn(Some("IE704"))
      sut.createIEMessage(message).isInstanceOf[IE704Message] mustBe true
    }

    "return an instance of IE801Message" in {
      when(message.key).thenReturn(Some("IE801"))
      sut.createIEMessage(message).isInstanceOf[IE801Message] mustBe true
    }

    "return an instance of IE802Message" in {
      when(message.key).thenReturn(Some("IE802"))
      sut.createIEMessage(message).isInstanceOf[IE802Message] mustBe true
    }

    "return an instance of IE803Message" in {
      when(message.key).thenReturn(Some("IE803"))
      sut.createIEMessage(message).isInstanceOf[IE803Message] mustBe true
    }

    "return an instance of IE807Message" in {
      when(message.key).thenReturn(Some("IE807"))
      sut.createIEMessage(message).isInstanceOf[IE807Message] mustBe true
    }

    "return an instance of IE810Message" in {
      when(message.key).thenReturn(Some("IE810"))
      sut.createIEMessage(message).isInstanceOf[IE810Message] mustBe true
    }

    "return an instance of IE813Message" in {
      when(message.key).thenReturn(Some("IE813"))
      sut.createIEMessage(message).isInstanceOf[IE813Message] mustBe true
    }

    "return an instance of IE818Message" in {
      when(message.key).thenReturn(Some("IE818"))
      sut.createIEMessage(message).isInstanceOf[IE818Message] mustBe true
    }

    "return an instance of IE819Message" in {
      when(message.key).thenReturn(Some("IE819"))
      sut.createIEMessage(message).isInstanceOf[IE819Message] mustBe true
    }

    "return an instance of IE837Message" in {
      when(message.key).thenReturn(Some("IE837"))
      sut.createIEMessage(message).isInstanceOf[IE837Message] mustBe true
    }

    "return an instance of IE840Message" in {
      when(message.key).thenReturn(Some("IE840"))
      sut.createIEMessage(message).isInstanceOf[IE840Message] mustBe true
    }

    "return an instance of IE871Message" in {
      when(message.key).thenReturn(Some("IE871"))
      sut.createIEMessage(message).isInstanceOf[IE871Message] mustBe true
    }

  }

  "createFromType" should {
    "return throw an error" when {
      "cannot handle message type" in {

        the[Exception] thrownBy {
          sut.createFromXml("Anything", IE801)
        } must have message s"[IEMessageFactory] - Could not create Message object. Unsupported message: Anything"
      }
    }

    "return an instance of IE704Message" in {
      val result = sut.createFromXml("IE704", IE704).asInstanceOf[IE704Message]
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000000012"))
      result.messageIdentifier mustBe "XI000001"
      result.messageType mustBe MessageTypes.IE704.value
      result.lrnEquals("lrnie8158976912") mustBe true
      result.lrnEquals("diffLrn") mustBe false
    }

    "return an instance of IE704Message when no arc" in {
      val result = sut.createFromXml("IE704", IE704NoArc).asInstanceOf[IE704Message]
      result.administrativeReferenceCode mustBe Seq(None)
    }

    "return an instance of IE801Message" in {
      val result = sut.createFromXml("IE801", IE801).asInstanceOf[IE801Message]
      result.consignorId mustBe Some("tokentokentok")
      result.consigneeId mustBe Some("token")
      result.administrativeReferenceCode mustBe Seq(Some("tokentokentokentokent"))
      result.localReferenceNumber mustBe Some("token")
      result.messageIdentifier mustBe "token"
      result.messageType mustBe MessageTypes.IE801.value
      result.lrnEquals("token") mustBe true
    }

    "return an instance of IE802Message" in {
      val result = sut.createFromXml("IE802", IE802).asInstanceOf[IE802Message]
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000000090"))
      result.messageIdentifier mustBe "X00004"
      result.messageType mustBe MessageTypes.IE802.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE803Message" in {
      val result = sut.createFromXml("IE803", IE803).asInstanceOf[IE803Message]
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000056333"))
      result.messageIdentifier mustBe "GB002312688"
      result.messageType mustBe MessageTypes.IE803.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE807Message" in {
      val result = sut.createFromXml("IE807", IE807).asInstanceOf[IE807Message]
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000000331"))
      result.messageIdentifier mustBe "GB0023121"
      result.messageType mustBe MessageTypes.IE807.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE810Message" in {
      val result = sut.createFromXml("IE810", IE810).asInstanceOf[IE810Message]
      result.consignorId mustBe None
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23GB00000000000377161"))
      result.localReferenceNumber mustBe None
      result.messageIdentifier mustBe "GB100000000302249"
      result.messageType mustBe MessageTypes.IE810.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE813Message" in {
      val result = sut.createFromXml("IE813", IE813).asInstanceOf[IE813Message]
      result.consignorId mustBe None
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23GB00000000000378126"))
      result.messageIdentifier mustBe "GB100000000302715"
      result.messageType mustBe MessageTypes.IE813.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE815Message" in {
      val result = sut.createFromXml("IE815", IE815).asInstanceOf[IE815Message]
      result.consignorId mustBe "GBWK002281023"
      result.consigneeId mustBe Some("GBWKQOZ8OVLYR")
      result.administrativeReferenceCode mustBe Seq(None)
      result.localReferenceNumber mustBe "LRNQA20230909022221"
      result.messageIdentifier mustBe "6de1b822562c43fb9220d236e487c920"
      result.messageType mustBe MessageTypes.IE815.value
      result.lrnEquals("LRNQA20230909022221") mustBe true
      result.lrnEquals("otherLrn") mustBe false
    }

    "return an instance of IE818Message" in {
      val result = sut.createFromXml("IE818", IE818).asInstanceOf[IE818Message]
      result.consignorId mustBe None
      result.consigneeId mustBe Some("GBWK002281023")
      result.administrativeReferenceCode mustBe Seq(Some("23GB00000000000378553"))
      result.messageIdentifier mustBe "GB100000000302814"
      result.messageType mustBe MessageTypes.IE818.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE819Message" in {
      val result = sut.createFromXml("IE819", IE819).asInstanceOf[IE819Message]
      result.consignorId mustBe None
      result.consigneeId mustBe Some("GBWK002281023")
      result.administrativeReferenceCode mustBe Seq(Some("23GB00000000000378574"))
      result.messageIdentifier mustBe "GB100000000302820"
      result.messageType mustBe MessageTypes.IE819.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE829Message" in {
      val result = sut.createFromXml("IE829", IE829).asInstanceOf[IE829Message]
      result.consigneeId mustBe Some("AT00000612157")
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000056339"),Some("23XI00000000000056340"))
      result.messageIdentifier mustBe "XI004321B"
      result.messageType mustBe MessageTypes.IE829.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE837Message with Consignor" in {
      val result = sut.createFromXml("IE837", IE837WithConsignor).asInstanceOf[IE837Message]
      result.consignorId mustBe Some("GBWK240176600")
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("16GB00000000000192223"))
      result.messageIdentifier mustBe "GB100000000302681"
      result.messageType mustBe MessageTypes.IE837.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE837Message with Consignee" in {
      val result = sut.createFromXml("IE837", IE837WithConsignee).asInstanceOf[IE837Message]
      result.consignorId mustBe None
      result.consigneeId mustBe Some("GBWK240176600")
      result.administrativeReferenceCode mustBe Seq(Some("16GB00000000000192223"))
    }

    "return an instance of IE839Message" in {
      val result = sut.createFromXml("IE839", IE839).asInstanceOf[IE839Message]
      result.consigneeId mustBe Some("AT00000612158")
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000056341"))
      result.messageIdentifier mustBe "XI004322"
      result.messageType mustBe MessageTypes.IE839.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE839Message with multiple ARCs" in {
      val result = sut.createFromXml("IE839", IE839MultipleArcs).asInstanceOf[IE839Message]
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000056341"), Some("23XI00000000000056342"), Some("23XI00000000000056343"))
    }

    "return an instance of IE840Message" in {
      val result = sut.createFromXml("IE840", IE840).asInstanceOf[IE840Message]
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000000333"))
      result.messageIdentifier mustBe "XI0003265"
      result.messageType mustBe MessageTypes.IE840.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE871Message" in {
      val result = sut.createFromXml("IE871", IE871).asInstanceOf[IE871Message]
      result.consignorId mustBe Some("GBWK240176600")
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23GB00000000000377768"))
      result.messageIdentifier mustBe "GB100000000302708"
      result.messageType mustBe MessageTypes.IE871.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE881Message" in {
      val result = sut.createFromXml("IE881", IE881).asInstanceOf[IE881Message]
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000056349"))
      result.messageIdentifier mustBe "XI00432M"
      result.messageType mustBe MessageTypes.IE881.value
      result.lrnEquals("anyLrn") mustBe false
    }

    "return an instance of IE905Message" in {
      val result = sut.createFromXml("IE905", IE905).asInstanceOf[IE905Message]
      result.consigneeId mustBe None
      result.administrativeReferenceCode mustBe Seq(Some("23XI00000000000056349"))
      result.messageIdentifier mustBe "XI00432RR"
      result.messageType mustBe MessageTypes.IE905.value
      result.lrnEquals("anyLrn") mustBe false
    }
  }
}