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
import play.api.libs.json.OWrites
import scalaxb.XMLFormat
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats.GeneratedJsonWriters
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects._

class GeneratedJsonWritersSpec extends AnyFreeSpec with GeneratedJsonWriters with Matchers {

  "ie704Writes" - new TestType[IE704Type](IE704TestMessageType, this.IE704Type)
  "ie801Writes" - new TestType[IE801Type](IE801TestMessageType, this.IE801Type)
  "ie802Writes" - new TestType[IE802Type](IE802TestMessageType, this.IE802Type)
  "ie803Writes" - new TestType[IE803Type](IE803TestMessageType, this.IE803Type)
  "ie807Writes" - new TestType[IE807Type](IE807TestMessageType, this.IE807Type)
  "ie810Writes" - new TestType[IE810Type](IE810TestMessageType, this.IE810Type)
  "ie813Writes" - new TestType[IE813Type](IE813TestMessageType, this.IE813Type)
  "ie815Writes" - new TestType[IE815Type](IE815TestMessageType, this.IE815Type)
  "ie818Writes" - new TestType[IE818Type](IE818TestMessageType, this.IE818Type)
  "ie819Writes" - new TestType[IE819Type](IE819TestMessageType, this.IE819Type)
  "ie829Writes" - new TestType[IE829Type](IE829TestMessageType, this.IE829Type)
  "ie837Writes" - new TestType[IE837Type](IE837TestMessageType, this.IE837Type)
  "ie839Writes" - new TestType[IE839Type](IE839TestMessageType, this.IE839Type)
  "ie840Writes" - new TestType[IE840Type](IE840TestMessageType, this.IE840Type)
  "ie871Writes" - new TestType[IE871Type](IE871TestMessageType, this.IE871Type)
  "ie881Writes" - new TestType[IE881Type](IE881TestMessageType, this.IE881Type)
  "ie905Writes" - new TestType[IE905Type](IE905TestMessageType, this.IE905Type)

  case class TestType[T](testObject: TestMessageType, writes: OWrites[T])(implicit xmlFormat: XMLFormat[T]) extends XMLProtocol {
    lazy val model: T = scalaxb.fromXML[T](testObject.xml1)

    "converting model to Json" in {
      writes.writes(model) mustBe testObject.json1
    }

  }

}
