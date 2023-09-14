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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers


import akka.actor.ActorSystem
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeXmlParsers}

import scala.concurrent.ExecutionContext
import scala.xml.Elem

class DraftExciseMovementControllerSpec extends PlaySpec with FakeAuthentication with FakeXmlParsers with TestXml with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys = ActorSystem("DraftExciseMovementControllerSpec")

  "submit" should {
      "return 200" in {
        val result = createSuccessfulAuth.submit(createRequest(IE815))

        status(result) mustBe OK
    }

      "return 400" when {
        "xml cannot be parsed" in {
          val result = createFailingXmlParserAction.submit(createRequest(IE815))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return error" when {
        "authentication fails" in {
          val result = createAuthActionFailure.submit(createRequest(IE815))

          status(result) mustBe FORBIDDEN
        }
      }

  }

  private def createAuthActionFailure = {
    new DraftExciseMovementController(
      FakeFailingAuthentication,
      FakeSuccessIE815XMLParser,
      stubControllerComponents()
    )
  }

  private def createFailingXmlParserAction =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeFailureIE815XMLParser,
      stubControllerComponents()
    )

  private def createSuccessfulAuth =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessIE815XMLParser,
      stubControllerComponents()
    )

  private def createRequest(body: Elem) = {
    FakeRequest(
      method = "POST",
      uri = "/foo",
      headers = FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml")),
      body = body
    )
  }
}
