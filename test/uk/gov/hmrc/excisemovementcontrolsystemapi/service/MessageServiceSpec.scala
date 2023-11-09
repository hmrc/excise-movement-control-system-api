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

import dispatch.Future
import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{Ie801XmlMessage, TestXml}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class MessageServiceSpec extends PlaySpec with EitherValues with TestXml {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val movementRepository = mock[MovementRepository]
  private val messageService = new MessageService(movementRepository, ec)

  "getErns" should {

    "return the consignee and consignor from the message for an IE801" in {

      val ie801Message = IE801Message.createFromXml(Ie801XmlMessage.IE801)

      await(messageService.getErns(ie801Message)) mustBe Set("tokentokentok", "token")

    }

    "return the consignor and consignee from the associated movement for an IE810" in {

      val testArc = "23GB00000000000377161"

      when(movementRepository.getMovementByARC(testArc)).thenReturn(Future.successful(
        Seq(Movement("lrn", "consignor", Some("consignee"), Some(testArc)))))

      val ie810Message = IE810Message.createFromXml(IE810)

      await(messageService.getErns(ie810Message)) mustEqual Set("consignor", "consignee")

    }

    "return the consignor from the associated movement for an IE813" in {

      val testArc = "23GB00000000000378126"

      when(movementRepository.getMovementByARC(testArc)).thenReturn(Future.successful(
        Seq(Movement("lrn", "consignor", Some("consignee"), Some(testArc)))))

      val ie813Message = IE813Message.createFromXml(IE813)

      await(messageService.getErns(ie813Message)) mustEqual Set("consignor")

    }

    "return the consignor from the message for an IE815" in {

      val ie815Message = IE815Message.createFromXml(IE815)

      await(messageService.getErns(ie815Message)) mustBe Set("GBWK002281023")

    }

    "return the consignee from the message for an IE818" in {

      val ie818Message = IE818Message.createFromXml(IE818)

      await(messageService.getErns(ie818Message)) mustBe Set("GBWK002281023")

    }

    "return the consignee from the message for an IE819" in {

      val ie819Message = IE819Message.createFromXml(IE819)

      await(messageService.getErns(ie819Message)) mustBe Set("GBWK002281023")

    }

    "return the consignee from the message for an IE837 if consignee sent" in {

      val ie837Message = IE837Message.createFromXml(IE837WithConsignee)

      await(messageService.getErns(ie837Message)) mustBe Set("GBWK240176600")

    }

    "return the consignor from the message for an IE837 if consignor sent" in {

      val ie837Message = IE837Message.createFromXml(IE837WithConsignor)

      await(messageService.getErns(ie837Message)) mustBe Set("GBWK240176600")

    }

    "return the consignor from the message for an IE871" in {

      val ie871Message = IE871Message.createFromXml(IE871)

      await(messageService.getErns(ie871Message)) mustBe Set("GBWK240176600")

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
        await(messageService.getErns(new NonSupportedMessage())) must
        have message "[MessageService] - Unsupported Message Type: any-type"
    }

  }

}
