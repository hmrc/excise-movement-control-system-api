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

import generated.IE815Type
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{times, verify, when}
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{AuthorizedIE815Request, AuthorizedRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.XmlParser
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext.Implicits.global

class ParseIE815XmlActionSpec extends PlaySpec with EitherValues{

  private val xmlParser = mock[XmlParser]
  private val controller = new ParseIE815XmlActionImpl(xmlParser, stubMessagesControllerComponents())

  val xmlStr = """<?xml version="1.0" encoding="UTF-8"?>
              |<note>
              |  <to>Tove</to>
              |  <from>Jani</from>
              |  <heading>Reminder</heading>
              |  <body>Don't forget me this weekend!</body>
              |</note>""".stripMargin

  val xmlStr1 = """<note>
                 |  <to>Tove</to>
                 |  <from>Jani</from>
                 |  <heading>Reminder</heading>
                 |  <body>Don't forget me this weekend!</body>
                 |</note>""".stripMargin

  "parseXML" should {
    "return 400" in {


      val request = AuthorizedRequest(FakeRequest(), Set.empty, "123")
      val result = await(controller.refine(request))

      result mustBe Left(BadRequest("error"))
    }

    "return a request with the IE815Types object" in {

      val obj = mock[IE815Type]
      when(xmlParser.fromXml(any)).thenReturn(obj)

      val body = scala.xml.XML.loadString(xmlStr)
      val fakeRequest = FakeRequest().withBody(body);
      val request = AuthorizedRequest(fakeRequest, Set.empty, "123")

      val result = await(controller.refine(request))

      verify(xmlParser).fromXml(eqTo(body))
      result mustBe Right(AuthorizedIE815Request(request, obj, "123"))
    }

    //todo: this need to be relooked at
    "return a request with the IE815Types object hjk" in {

      val json: JsValue = Json.parse(
      """{"xml": "<note><to>Tove</to><from>Jani</from><heading>Reminder</heading><body>Don't forget me this weekend!</body></note>"}""")
      val obj = mock[IE815Type]
      when(xmlParser.fromXml(any)).thenReturn(obj)

      val body = scala.xml.XML.loadString(xmlStr)
      val fakeRequest = FakeRequest().withHeaders("Content-Type" -> "application/json").withBody(json);
      val request = AuthorizedRequest(fakeRequest, Set.empty, "123")

      val result = await(controller.refine(request))


      result mustBe Right(AuthorizedIE815Request(request, obj, "123"))
    }
  }
}