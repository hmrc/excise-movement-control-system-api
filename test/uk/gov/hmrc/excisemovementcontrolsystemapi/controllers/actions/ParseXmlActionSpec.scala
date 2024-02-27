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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.ExecutionContext

class ParseXmlActionSpec
  extends PlaySpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val messageFactory = mock[IEMessageFactory]
  private val dateTimeService = mock[DateTimeService]
  private val message = mock[IEMessage]
  private val parserXmlAction = new ParseXmlActionImpl(
    messageFactory,
    dateTimeService,
    stubControllerComponents()
  )
  private val timestamp = Instant.parse("2023-05-11T01:01:01.987654Z")

  private val xmlStr =
    """<IE815>
      | <body></body>
      |</IE815>""".stripMargin


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(messageFactory, dateTimeService, message)

    when(dateTimeService.timestamp()).thenReturn(timestamp)
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

        val expectedError = ErrorResponse(timestamp, "XML error", "Not valid XML or XML is empty")
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }

      "cannot message from xml" in {
        when(messageFactory.createFromXml(any, any)).thenThrow(new RuntimeException("Error message"))
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

        val result = parserXmlAction.refine(enrolmentRequest).futureValue

        val expectedError = ErrorResponse(timestamp, "Not valid IE815 message", "Error message")
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }
    }
  }

}
