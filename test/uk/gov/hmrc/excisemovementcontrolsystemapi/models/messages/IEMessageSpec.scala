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
    "have the correct starting tag " when {
      //TODO: Improve the messages generally to allow programmatic testing of them
      "using createFromXml (704) with namespace prefix" in {
        val result = IE704Message.createFromXml(IE704)
        result.key mustBe Some("IE704")
        result.namespace mustBe Some("http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3")
      }

      "using createFromXml (704) without namespace prefix" in {
        val result = IE704Message.createFromXml(IE704NoNamespace)
        result.key mustBe Some("IE704")
        result.namespace mustBe None
      }

      "using createFromXml (801) with namespace prefix" in {
        val result = IE801Message.createFromXml(IE801)
        result.key mustBe Some("IE801")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.13")
      }

      "using createFromXml (801) without namespace prefix" in {
        val result = IE801Message.createFromXml(IE801NoNamespace)
        result.key mustBe Some("IE801")
        result.namespace mustBe None
      }

      "using createFromXml (802) with namespace prefix" in {
        val result = IE802Message.createFromXml(IE802)
        result.key mustBe Some("IE802")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.13")
      }

      "using createFromXml (802) without namespace prefix" in {
        val result = IE802Message.createFromXml(IE802NoNamespace)
        result.key mustBe Some("IE802")
        result.namespace mustBe None
      }

      "using createFromXml (803) with namespace prefix" in {
        val result = IE803Message.createFromXml(IE803)
        result.key mustBe Some("IE803")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE803:V3.13")
      }

      "using createFromXml (803) without namespace prefix" in {
        val result = IE803Message.createFromXml(IE803NoNamespace)
        result.key mustBe Some("IE803")
        result.namespace mustBe None
      }

      "using createFromXml (807) with namespace prefix" in {
        val result = IE807Message.createFromXml(IE807)
        result.key mustBe Some("IE807")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE807:V3.13")
      }

      "using createFromXml (807) without namespace prefix" in {
        val result = IE807Message.createFromXml(IE807NoNamespace)
        result.key mustBe Some("IE807")
        result.namespace mustBe None
      }

      "using createFromXml (810) with namespace prefix" in {
        val result = IE810Message.createFromXml(IE810)
        result.key mustBe Some("IE810")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.13")
      }

      "using createFromXml (810) without namespace prefix" in {
        val result = IE810Message.createFromXml(IE810NoNamespace)
        result.key mustBe Some("IE810")
        result.namespace mustBe Some("http://www.hmrc.gov.uk/ChRIS/Service/Control")
      }

      "using createFromXml (813) with namespace prefix" in {
        val result = IE813Message.createFromXml(IE813)
        result.key mustBe Some("IE813")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE813:V3.13")
      }

      "using createFromXml (813) without namespace prefix" in {
        val result = IE813Message.createFromXml(IE813NoNamespace)
        result.key mustBe Some("IE813")
        result.namespace mustBe Some("http://www.hmrc.gov.uk/ChRIS/Service/Control")
      }

      //This message behaves differently to the others
      "using createFromXml (815)" in {
        val result = IE815Message.createFromXml(IE815)
        result.toXml.head.label mustBe "IE815"
      }

      "using createFromXml (818) with namespace prefix" in {
        val result = IE818Message.createFromXml(IE818)
        result.key mustBe Some("IE818")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.13")
      }

      "using createFromXml (818) without namespace prefix" in {
        val result = IE818Message.createFromXml(IE818NoNamespace)
        result.key mustBe Some("IE818")
        result.namespace mustBe Some("http://www.hmrc.gov.uk/ChRIS/Service/Control")
      }

      "using createFromXml (819) with namespace prefix" in {
        val result = IE819Message.createFromXml(IE819)
        result.key mustBe Some("IE819")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE819:V3.13")
      }

      "using createFromXml (819) without namespace prefix" in {
        val result = IE819Message.createFromXml(IE819NoNamespace)
        result.key mustBe Some("IE819")
        result.namespace mustBe Some("http://www.hmrc.gov.uk/ChRIS/Service/Control")
      }

      "using createFromXml (829) with namespace prefix" in {
        val result = IE829Message.createFromXml(IE829)
        result.key mustBe Some("IE829")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13")
      }

      "using createFromXml (829) without namespace prefix" in {
        val result = IE829Message.createFromXml(IE829NoNamespace)
        result.key mustBe Some("IE829")
        result.namespace mustBe None
      }

      "using createFromXml (837) with namespace prefix" in {
        val result = IE837Message.createFromXml(IE837WithConsignor)
        result.key mustBe Some("IE837")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.13")
      }

      "using createFromXml (837) without namespace prefix" in {
        val result = IE837Message.createFromXml(IE837WithConsignorNoNamespace)
        result.key mustBe Some("IE837")
        result.namespace mustBe Some("http://www.hmrc.gov.uk/ChRIS/Service/Control")
      }

      "using createFromXml (839) with namespace prefix" in {
        val result = IE839Message.createFromXml(IE839)
        result.key mustBe Some("IE839")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.13")
      }

      "using createFromXml (839) without namespace prefix" in {
        val result = IE839Message.createFromXml(IE839NoNamespace)
        result.key mustBe Some("IE839")
        result.namespace mustBe None
      }

      "using createFromXml (840) with namespace prefix" in {
        val result = IE840Message.createFromXml(IE840)
        result.key mustBe Some("IE840")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE840:V3.13")
      }

      "using createFromXml (840) without namespace prefix" in {
        val result = IE840Message.createFromXml(IE840NoNamespace)
        result.key mustBe Some("IE840")
        result.namespace mustBe None
      }

      "using createFromXml (871) with namespace prefix" in {
        val result = IE871Message.createFromXml(IE871WithConsignor)
        result.key mustBe Some("IE871")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE871:V3.13")
      }

      "using createFromXml (871) without namespace prefix" in {
        val result = IE871Message.createFromXml(IE871WithConsignorNoNamespace)
        result.key mustBe Some("IE871")
        result.namespace mustBe Some("http://www.hmrc.gov.uk/ChRIS/Service/Control")
      }

      "using createFromXml (881) with namespace prefix" in {
        val result = IE881Message.createFromXml(IE881)
        result.key mustBe Some("IE881")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE881:V3.13")
      }

      "using createFromXml (881) without namespace prefix" in {
        val result = IE881Message.createFromXml(IE881NoNamespace)
        result.key mustBe Some("IE881")
        result.namespace mustBe None
      }

      "using createFromXml (905) with namespace prefix" in {
        val result = IE905Message.createFromXml(IE905)
        result.key mustBe Some("IE905")
        result.namespace mustBe Some("urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE905:V3.13")
      }

      "using createFromXml (905) without namespace prefix" in {
        val result = IE905Message.createFromXml(IE905NoNamespace)
        result.key mustBe Some("IE905")
        result.namespace mustBe None
      }

    }
  }
  "IE704"        should {
    "return Some(correlationId) when populated" in {
      val result = IE704Message.createFromXml(IE704)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE704Message.createFromXml(IE704WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "return Some(localReferenceNumber) when populated" in {
      val result = IE704Message.createFromXml(IE704)
      result.localReferenceNumber mustBe Some("lrnie8158976912")

    }
    "return None when it's not populated" in {
      val result = IE704Message.createFromXml(IE704NoLocalReferenceNumber)
      result.localReferenceNumber mustBe None
    }
  }

  "IE801" should {
    "return Some(correlationId) when populated" in {
      val result = IE801Message.createFromXml(IE801)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE801Message.createFromXml(IE801WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return Some(localReferenceNumber)" in {
      val result = IE801Message.createFromXml(IE801)
      result.optionalLocalReferenceNumber mustBe Some("token")
    }
  }

  "IE802" should {
    "return Some(correlationId) when populated" in {
      val result = IE802Message.createFromXml(IE802)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated " in {
      val result = IE802Message.createFromXml(IE802WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE802Message.createFromXml(IE802)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE803" should {
    "return Some(correlationId) when populated" in {
      val result = IE803Message.createFromXml(IE803)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE803Message.createFromXml(IE803WithoutCorrelationId)
      result.correlationId mustBe None
    }
    "always return None for optionalLocalReferenceNumber" in {
      val result = IE803Message.createFromXml(IE803)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE807" should {
    "return Some(correlationId) when populated" in {
      val result = IE807Message.createFromXml(IE807)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE807Message.createFromXml(IE807WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE807Message.createFromXml(IE807)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE810" should {
    "return Some(correlationId) when populated" in {
      val result = IE810Message.createFromXml(IE810)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE810Message.createFromXml(IE810WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE810Message.createFromXml(IE810)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE813" should {
    "return Some(correlationId) when populated" in {
      val result = IE813Message.createFromXml(IE813)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE813Message.createFromXml(IE813WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE813Message.createFromXml(IE813)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE815" should {
    "return Some(correlationId) when populated" in {
      val result = IE815Message.createFromXml(IE815)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE815Message.createFromXml(IE815WithoutCorrelationId)
      result.correlationId mustBe None
    }
    "always return Some(localReferenceNumber)" in {
      val result = IE815Message.createFromXml(IE815)
      result.optionalLocalReferenceNumber mustBe Some("LRNQA20230909022221")
    }
  }

  "IE818" should {
    "return Some(correlationId) when populated" in {
      val result = IE818Message.createFromXml(IE818)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE818Message.createFromXml(IE818WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE818Message.createFromXml(IE818)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE819" should {
    "return Some(correlationId) when populated" in {
      val result = IE819Message.createFromXml(IE819)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE819Message.createFromXml(IE819WithoutCorrelationId)
      result.correlationId mustBe None
    }
    "always return None for optionalLocalReferenceNumber" in {
      val result = IE819Message.createFromXml(IE819)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE829" should {
    "return Some(correlationId) when populated" in {
      val result = IE829Message.createFromXml(IE829)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE829Message.createFromXml(IE829WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE829Message.createFromXml(IE829)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE837" should {
    "return Some(correlationId) when populated" in {
      val result = IE837Message.createFromXml(IE837WithConsignor)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE837Message.createFromXml(IE837WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE837Message.createFromXml(IE837WithConsignor)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE839" should {
    "return Some(correlationId) when populated" in {
      val result = IE839Message.createFromXml(IE839)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE839Message.createFromXml(IE839WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "return Some(localReferenceNumber) when populated" in {
      val result = IE839Message.createFromXml(IE839)
      result.optionalLocalReferenceNumber mustBe Some("lrnie8155755329")
    }
    "return None when it's not populated" in {
      val result = IE839Message.createFromXml(IE839NoLocalReferenceNumber)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE840" should {
    "return Some(correlationId) when populated" in {
      val result = IE840Message.createFromXml(IE840)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE840Message.createFromXml(IE840WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE840Message.createFromXml(IE840)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE871" should {
    "return Some(correlationId) when populated" in {
      val result = IE871Message.createFromXml(IE871WithConsignor)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE871Message.createFromXml(IE871WithoutCorrelationId)
      result.correlationId mustBe None
    }
    "always return None for optionalLocalReferenceNumber" in {
      val result = IE871Message.createFromXml(IE871WithConsignor)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE881" should {
    "return Some(correlationId) when populated" in {
      val result = IE881Message.createFromXml(IE881)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE881Message.createFromXml(IE881WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE881Message.createFromXml(IE881)
      result.optionalLocalReferenceNumber mustBe None
    }
  }

  "IE905" should {
    "return Some(correlationId) when populated" in {
      val result = IE905Message.createFromXml(IE905)
      result.correlationId mustBe Some(testCorrelationId)

    }
    "return None when correlationId is not populated" in {
      val result = IE905Message.createFromXml(IE905WithoutCorrelationId)
      result.correlationId mustBe None
    }

    "always return None for optionalLocalReferenceNumber" in {
      val result = IE905Message.createFromXml(IE905)
      result.optionalLocalReferenceNumber mustBe None
    }
  }
}
