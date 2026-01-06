/*
 * Copyright 2025 HM Revenue & Customs
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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.MessageTypeFormats.GeneratedJsonWritersV1
import uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects._

class GeneratedJsonWritersV1Spec extends AnyFreeSpec with GeneratedJsonWritersV1 with Matchers with TestXml {

  "IE704MessageV1" - new TestType[IE704Type](IE704TestMessageType, IE704MessageV1.createFromXml(IE704))
  "IE801MessageV1" - new TestType[IE801Type](IE801TestMessageType, IE801MessageV1.createFromXml(IE801))
  "IE802MessageV1" - new TestType[IE802Type](IE802TestMessageType, IE802MessageV1.createFromXml(IE802))
  "IE803MessageV1" - new TestType[IE803Type](IE803TestMessageType, IE803MessageV1.createFromXml(IE803))
  "IE807MessageV1" - new TestType[IE807Type](IE807TestMessageType, IE807MessageV1.createFromXml(IE807))
  "IE810MessageV1" - new TestType[IE810Type](IE810TestMessageType, IE810MessageV1.createFromXml(IE810))
  "IE813MessageV1" - new TestType[IE813Type](IE813TestMessageType, IE813MessageV1.createFromXml(IE813))
  "IE815MessageV1" - new TestType[IE815Type](IE815TestMessageType, IE815MessageV1.createFromXml(IE815))
  "IE818MessageV1" - new TestType[IE818Type](IE818TestMessageType, IE818MessageV1.createFromXml(IE818))
  "IE819MessageV1" - new TestType[IE819Type](IE819TestMessageType, IE819MessageV1.createFromXml(IE819))
  "IE829MessageV1" - new TestType[IE829Type](IE829TestMessageType, IE829MessageV1.createFromXml(IE829))
  "IE837MessageV1" - new TestType[IE837Type](IE837TestMessageType, IE837MessageV1.createFromXml(IE837WithConsignor))
  "IE839MessageV1" - new TestType[IE839Type](IE839TestMessageType, IE839MessageV1.createFromXml(IE839))
  "IE840MessageV1" - new TestType[IE840Type](IE840TestMessageType, IE840MessageV1.createFromXml(IE840))
  "IE871MessageV1" - new TestType[IE871Type](IE871TestMessageType, IE871MessageV1.createFromXml(IE871WithConsignor))
  "IE881MessageV1" - new TestType[IE881Type](IE881TestMessageType, IE881MessageV1.createFromXml(IE881))
  "IE905MessageV1" - new TestType[IE905Type](IE905TestMessageType, IE905MessageV1.createFromXml(IE905))

  case class TestType[T](testObject: TestMessageType, message: IEMessage) extends XMLProtocol {

    "successfully converts model to Json" in {
      message.toJson mustBe testObject.json1
    }

  }

}
