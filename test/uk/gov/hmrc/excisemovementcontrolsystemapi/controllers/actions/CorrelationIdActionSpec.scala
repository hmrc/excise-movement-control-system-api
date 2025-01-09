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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

class CorrelationIdActionSpec
    extends PlaySpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach
    with TestXml {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val uuidRegex: Regex              = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r

  "refine" should {
    "return an EnrolmentRequest with existing correlationId when one is provided" in {
      val xmlStr =
        """<IE815>
          | <body></body>
          |</IE815>""".stripMargin

      val testCorrelationId = "testCorrelationId"
      val headers           = Seq(HttpHeader.xCorrelationId -> testCorrelationId)

      val inputRequest = EnrolmentRequest(
        FakeRequest()
          .withHeaders(FakeHeaders(headers))
          .withBody(xml.XML.loadString(xmlStr)),
        Set("ern"),
        UserDetails("123", "abc")
      )

      val message = IE815Message.createFromXml(IE815)

      val correlationIdAction = new CorrelationIdAction

      val result = correlationIdAction.transform(inputRequest).futureValue

      result mustBe inputRequest
    }
    "return an EnrolmentRequest with new correlationId when none is provided" in {
      val xmlStr =
        """<IE815>
          | <body></body>
          |</IE815>""".stripMargin

      val inputRequest = EnrolmentRequest(
        FakeRequest()
          .withBody(xml.XML.loadString(xmlStr)),
        Set("ern"),
        UserDetails("123", "abc")
      )

      val message = IE815Message.createFromXml(IE815)

      val correlationIdAction = new CorrelationIdAction

      val result = correlationIdAction.transform(inputRequest).futureValue

      //Question before merge - do we want to confirm more of the internal details are preserved?
      result.headers.get(HttpHeader.xCorrelationId).isDefined mustBe true
      uuidRegex.matches(result.headers.get(HttpHeader.xCorrelationId).get) mustBe true
    }
  }
}
