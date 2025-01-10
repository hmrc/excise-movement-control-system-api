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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth

import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader

class EnrolmentRequestSpec extends PlaySpec {

  "correlationId" should {
    "return a correlationId if one already exists" in {
      val xmlStr        =
        """<IE815>
          | <body></body>
          |</IE815>""".stripMargin
      val correlationId = "correlationId"

      val request = EnrolmentRequest(
        FakeRequest()
          .withHeaders(HttpHeader.xCorrelationId -> correlationId)
          .withBody(xml.XML.loadString(xmlStr)),
        Set("ern"),
        UserDetails("123", "abc")
      )

      request.correlationId mustBe correlationId
    }

    "throw an exception if correlationId is not found" in {

      val xmlStr =
        """<IE815>
          | <body></body>
          |</IE815>""".stripMargin

      val request = EnrolmentRequest(
        FakeRequest()
          .withBody(xml.XML.loadString(xmlStr)),
        Set("ern"),
        UserDetails("123", "abc")
      )

      the[Exception] thrownBy {
        request.correlationId
      } must have message s"${HttpHeader.xCorrelationId} not found"
    }
  }
}
