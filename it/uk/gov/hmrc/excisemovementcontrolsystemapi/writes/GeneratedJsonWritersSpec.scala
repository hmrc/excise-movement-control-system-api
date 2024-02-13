/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.writes

import generated.{IE704Type, IE801Type, IE802Type, IE803Type, IE807Type, IE810Type, IE813Type, IE815Type, IE818Type, IE819Type, IE829Type, IE837Type, IE839Type, IE840Type, IE871Type, IE881Type, IE905Type, XMLProtocol}
import scalaxb.XMLFormat
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE704Message, IE801Message, IE802Message, IE803Message, IE807Message, IE810Message, IE813Message, IE815Message, IE818Message, IE819Message, IE829Message, IE837Message, IE839Message, IE840Message, IE871Message, IE881Message, IE905Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects._

class GeneratedJsonWritersSpec extends AnyFreeSpec with GeneratedJsonWriters with Matchers with TestXml {

  "IE704Message" - new TestType[IE704Type](IE704TestMessageType, IE704Message.createFromXml(IE704))
  "IE801Message" - new TestType[IE801Type](IE801TestMessageType, IE801Message.createFromXml(IE801))
  "IE802Message" - new TestType[IE802Type](IE802TestMessageType, IE802Message.createFromXml(IE802))
  "IE803Message" - new TestType[IE803Type](IE803TestMessageType, IE803Message.createFromXml(IE803))
  "IE807Message" - new TestType[IE807Type](IE807TestMessageType, IE807Message.createFromXml(IE807))
  "IE810Message" - new TestType[IE810Type](IE810TestMessageType, IE810Message.createFromXml(IE810))
  "IE813Message" - new TestType[IE813Type](IE813TestMessageType, IE813Message.createFromXml(IE813))
  "IE815Message" - new TestType[IE815Type](IE815TestMessageType, IE815Message.createFromXml(IE815))
  "IE818Message" - new TestType[IE818Type](IE818TestMessageType, IE818Message.createFromXml(IE818))
  "IE819Message" - new TestType[IE819Type](IE819TestMessageType, IE819Message.createFromXml(IE819))
  "IE829Message" - new TestType[IE829Type](IE829TestMessageType, IE829Message.createFromXml(IE829))
  "IE837Message" - new TestType[IE837Type](IE837TestMessageType, IE837Message.createFromXml(IE837WithConsignor))
  "IE839Message" - new TestType[IE839Type](IE839TestMessageType, IE839Message.createFromXml(IE839))
  "IE840Message" - new TestType[IE840Type](IE840TestMessageType, IE840Message.createFromXml(IE840))
  "IE871Message" - new TestType[IE871Type](IE871TestMessageType, IE871Message.createFromXml(IE871WithConsignor))
  "IE881Message" - new TestType[IE881Type](IE881TestMessageType, IE881Message.createFromXml(IE881))
  "IE905Message" - new TestType[IE905Type](IE905TestMessageType, IE905Message.createFromXml(IE905))

  case class TestType[T](testObject: TestMessageType, message: IEMessage)(implicit xmlFormat: XMLFormat[T]) extends XMLProtocol {

    "successfully converts model to Json" in {
      message.toJson mustBe testObject.json1
    }

  }

}
