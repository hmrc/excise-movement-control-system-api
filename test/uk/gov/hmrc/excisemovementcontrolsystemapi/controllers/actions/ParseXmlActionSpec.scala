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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions


import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class ParseXmlActionSpec
  extends PlaySpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val messageFactory = mock[IEMessageFactory]
  private val emcsUtils = mock[EmcsUtils]
  private val message = mock[IEMessage]
  private val parserXmlAction = new ParseXmlActionImpl(
    messageFactory,
    emcsUtils,
    stubControllerComponents()
  )
  private val dateTime = LocalDateTime.of(2023, 5, 11, 1, 1, 1)

  val xmlStr =
    """<IE815>
      | <body></body>
      |</IE815>""".stripMargin


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(messageFactory, emcsUtils, message)

    when(emcsUtils.getCurrentDateTime).thenReturn(dateTime)
    when(messageFactory.createFromXml(any, any)).thenReturn(message)
  }

  "refine" should {
    "return a ParsedXmlRequest" in {
      val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

      val result = parserXmlAction.refine(enrolmentRequest).futureValue

      result mustBe Right(ParsedXmlRequest(enrolmentRequest, message, Set("ern"), "123"))
    }

    "return an error" when {
      "no xml format was sent" in {
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xmlStr), Set("ern"), "123")

        val result = parserXmlAction.refine(enrolmentRequest).futureValue

        val expectedError = ErrorResponse(dateTime, "XML error", "Not valid XML or XML is empty")
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }

      "cannot message from xml" in {
        when(messageFactory.createFromXml(any, any)).thenThrow(new RuntimeException("Error message"))
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

        val result = parserXmlAction.refine(enrolmentRequest).futureValue

        val expectedError = ErrorResponse(dateTime, "Not valid IE815 message", "Error message")
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }
    }
  }

}
