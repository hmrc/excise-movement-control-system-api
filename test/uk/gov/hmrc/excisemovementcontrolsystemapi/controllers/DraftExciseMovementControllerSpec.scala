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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.verify
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MovementMessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateConsignorAction, FakeXmlParsers}
import scala.concurrent.ExecutionContext
import scala.xml.Elem

import scala.concurrent.ExecutionContext
import scala.xml.Elem
    with FakeAuthentication
    with FakeXmlParsers
    with FakeValidateConsignorAction
    with TestXml
    with EitherValues {

class DraftExciseMovementControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeXmlParsers
    with FakeValidateConsignorAction
    with TestXml
    with EitherValues {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys = ActorSystem("DraftExciseMovementControllerSpec")
  private val connector = mock[MovementMessageConnector]
  private val cc = stubControllerComponents()

  private val cc = stubControllerComponents()

  "submit" should {
    "return 200" in {
      val result = createWithSuccessfulAuth.submit(createRequest(IE815))

      status(result) mustBe OK
    }

//    "send a request to EIS" in {
//
//      verify(connector).post(eqTo("<IE815></IE815>"), eqTo("IE815"))(any)
//    }

    "a validation parser error" when {
      "xml cannot be parsed" in {
        val result = createWithFailingXmlParserAction.submit(createRequest(IE815))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.submit(createRequest(IE815))

        status(result) mustBe FORBIDDEN
      }
    }

    "return a consignor validation error" when {
      "consignor is not valid" in {
        val result = createWithValidateConsignorActionFailure.submit(createRequest(IE815))

        status(result) mustBe FORBIDDEN
      }
    }
  }

  private def createWithAuthActionFailure =
    new DraftExciseMovementController(
      FakeFailingAuthentication,
      FakeSuccessIE815XMLParser,
      FakeSuccessfulValidateConsignorAction,
      connector,
      cc
    )

  private def createWithFailingXmlParserAction =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeFailureIE815XMLParser,
      FakeSuccessfulValidateConsignorAction,
      connector,
      cc
    )

  private def createWithSuccessfulAuth =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessIE815XMLParser,
      FakeSuccessfulValidateConsignorAction,
      connector,
      cc
    )

  private def createWithValidateConsignorActionFailure =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessIE815XMLParser,
      FakeFailureValidateConsignorAction,
      connector,
      cc
    )

   private def createRequest(body: Elem): FakeRequest[Elem] = {
    FakeRequest(
      method = "POST",
      uri = "/foo",
      headers = FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml")),
      body = body
    )
  }
}
