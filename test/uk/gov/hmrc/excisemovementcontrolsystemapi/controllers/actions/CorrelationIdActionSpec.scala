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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.PlaySpec
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader

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

  "transform" should {
    "return an EnrolmentRequest with existing correlationId when one is provided" in {
      val xmlStr =
        """<IE815>
          | <body>test</body>
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

      val correlationIdAction = new CorrelationIdAction

      val result = correlationIdAction.transform(inputRequest).futureValue

      result mustBe inputRequest

      withClue("without modifying other parts of the request") {
        result.body.toString mustBe xmlStr
      }
    }
    "return an EnrolmentRequest with new correlationId when none is provided" in {
      val xmlStr =
        """<IE815>
          | <body>test</body>
          |</IE815>""".stripMargin

      val inputRequest = EnrolmentRequest(
        FakeRequest()
          .withBody(xml.XML.loadString(xmlStr)),
        Set("ern"),
        UserDetails("123", "abc")
      )

      val correlationIdAction = new CorrelationIdAction

      val result = correlationIdAction.transform(inputRequest).futureValue

      result.headers.get(HttpHeader.xCorrelationId).isDefined mustBe true
      uuidRegex.matches(result.headers.get(HttpHeader.xCorrelationId).get) mustBe true

      withClue("without modifying other parts of the request") {
        result.body.toString mustBe xmlStr
      }
    }
  }
}
