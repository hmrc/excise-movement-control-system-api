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
import org.mockito.MockitoSugar.when
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import scalaxb.ParserFailure
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequestCopy}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.XmlParser
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseIE815XmlActionSpec extends PlaySpec with EitherValues with BeforeAndAfterAll {

  implicit val emcsUtils: EmcsUtils = mock[EmcsUtils]

  private val xmlParser = mock[XmlParser]
  private val ieMessageFactory = mock[IEMessageFactory]
  private val controller = new ParseXmlActionImpl(ieMessageFactory, emcsUtils, stubMessagesControllerComponents())

  private val currentDateTime = LocalDateTime.of(2023, 10, 18, 15, 33, 33)

  private val xmlStr =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<note>
      |  <to>Tove</to>
      |  <from>Jani</from>
      |  <heading>Reminder</heading>
      |  <body>Don't forget me this weekend!</body>
      |</note>""".stripMargin


  def block(authRequest: ParsedXmlRequestCopy[_]) =
    Future.successful(Ok)

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(emcsUtils.getCurrentDateTime).thenReturn(currentDateTime)
  }

  "parseXML" should {
    "return 400 if no body supplied" in {
      val request = EnrolmentRequest(FakeRequest().withBody(None), Set.empty, "123")
      val result = await(controller.refine(request))

      result.left.value mustBe BadRequest(Json.toJson(ErrorResponse(currentDateTime, "XML error", "Not valid XML or XML is empty")))

    }

    "return a request with the IE815Types object when supplied XML Node Sequence" in {

      val obj = mock[IEMessage]
      when(ieMessageFactory.createFromXml(any, any)).thenReturn(obj)

      val body = scala.xml.XML.loadString(xmlStr)
      val fakeRequest = FakeRequest().withBody(body)
      val request = EnrolmentRequest(fakeRequest, Set.empty, "123")

      val result = await(controller.refine(request))

      result mustBe Right(ParsedXmlRequestCopy(request, obj, Set.empty, "123"))
    }

    "return a Bad Request supplied XML Node Sequence that is not an IE815" in {

      when(ieMessageFactory.createFromXml(any, any)).thenThrow(new ParserFailure("exception"))

      val body = scala.xml.XML.loadString(xmlStr)
      val fakeRequest = FakeRequest().withBody(body)
      val request = EnrolmentRequest(fakeRequest, Set.empty, "123")

      val result = await(controller.refine(request))
      result.left.value mustBe BadRequest(Json.toJson(ErrorResponse(currentDateTime, "Not valid note message", "exception")))

    }

    "return 400 if body supplied is a string" in {
      val request = EnrolmentRequest(FakeRequest().withBody("<xml>asdasd</xml>"), Set.empty, "123")
      val result = await(controller.refine(request))

      result.left.value mustBe BadRequest(Json.toJson(ErrorResponse(currentDateTime, "XML error", "Not valid XML or XML is empty")))
    }
  }
}