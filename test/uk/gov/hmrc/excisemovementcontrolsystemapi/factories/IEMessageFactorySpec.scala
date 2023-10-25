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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._

class IEMessageFactorySpec extends PlaySpec with BeforeAndAfterEach{

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
        }.getMessage mustBe s"Could not create Message object. Unsupported message: Anything"
      }

      "messageType is empty" in {
        when(message.key).thenReturn(None)
        intercept[RuntimeException] {
          sut.createIEMessage(message)
        }.getMessage mustBe "Could not create Message object. Message type is empty"
      }
    }

    "return an instance of IE801Message" in {
      when(message.key).thenReturn(Some("IE801"))
      sut.createIEMessage(message).isInstanceOf[IE801Message] mustBe true
    }

    "return an instance of IE818Message" in {
      when(message.key).thenReturn(Some("IE818"))
      sut.createIEMessage(message).isInstanceOf[IE818Message] mustBe true
    }
  }
}