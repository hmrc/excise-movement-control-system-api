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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages

import generated.{Number1Value31, Number2Value30}
import org.scalatest.Inspectors.forAll
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml

class IEMessageSpec extends PlaySpec with TestXml {

  object testClass extends SubmitterTypeConverter

  "convertSubmitterType" should {
    "return Consignor" when {
      "submitter type is consignor" in {
        testClass.convertSubmitterType(Number1Value31) mustBe Consignor
      }
    }

    "return Consignee" when {
      "submitter type is consignee" in {
        testClass.convertSubmitterType(Number2Value30) mustBe Consignee
      }
    }
  }

  "each message" should {
//TODO: Improve the messages generally to allow programmatic testing of them
    "have the correct starting tag using createFromXml (704)" in {
      val result = IE704Message.createFromXml(IE704)
      result.key mustBe Some("IE704")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (801)" in {
      val result = IE801Message.createFromXml(IE801)
      result.key mustBe Some("IE801")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (802)" in {
      val result = IE802Message.createFromXml(IE802)
      result.key mustBe Some("IE802")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (803)" in {
      val result = IE803Message.createFromXml(IE803)
      result.key mustBe Some("IE803")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (807)" in {
      val result = IE807Message.createFromXml(IE807)
      result.key mustBe Some("IE807")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (810)" in {
      val result = IE810Message.createFromXml(IE810)
      result.key mustBe Some("IE810")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (813)" in {
      val result = IE813Message.createFromXml(IE813)
      result.key mustBe Some("IE813")
      result.namespace mustBe None
    }

    //This message behaves differently to the others
    "have the correct starting tag using createFromXml (815)" in {
      val result = IE815Message.createFromXml(IE815)
      result.toXml.head.label mustBe "IE815"
    }

    "have the correct starting tag using createFromXml (818)" in {
      val result = IE818Message.createFromXml(IE818)
      result.key mustBe Some("IE818")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (819)" in {
      val result = IE819Message.createFromXml(IE819)
      result.key mustBe Some("IE819")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (829)" in {
      val result = IE829Message.createFromXml(IE829)
      result.key mustBe Some("IE829")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (837)" in {
      val result = IE837Message.createFromXml(IE837WithConsignor)
      result.key mustBe Some("IE837")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (839)" in {
      val result = IE839Message.createFromXml(IE839)
      result.key mustBe Some("IE839")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (840)" in {
      val result = IE840Message.createFromXml(IE840)
      result.key mustBe Some("IE840")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (871)" in {
      val result = IE871Message.createFromXml(IE871WithConsignor)
      result.key mustBe Some("IE871")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (881)" in {
      val result = IE881Message.createFromXml(IE881)
      result.key mustBe Some("IE881")
      result.namespace mustBe None
    }

    "have the correct starting tag using createFromXml (905)" in {
      val result = IE905Message.createFromXml(IE905)
      result.key mustBe Some("IE905")
      result.namespace mustBe None
    }
  }
}
