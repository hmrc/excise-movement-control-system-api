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
import generated.IE815Type
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.mvc.Results.NotFound
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MovementMessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateConsignorAction, FakeXmlParsers}
import scala.concurrent.ExecutionContext
import scala.xml.Elem
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.DataRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISResponse

import scala.concurrent.{ExecutionContext, Future}
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
    with BeforeAndAfterEach
    with EitherValues {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val sys = ActorSystem("DraftExciseMovementControllerSpec")
  private val connector = mock[MovementMessageConnector]
  private val cc = stubControllerComponents()
  private val cc = stubControllerComponents()
  private val ieMessage = scalaxb.fromXML[IE815Type](IE815)
  private val request = createRequest(IE815)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector)

    when(connector.submitExciseMovement(any, any)(any)).thenReturn(Future.successful(Right(EISResponse("ok", "success", "123"))))
  }

  "submit" should {
    "return 200" in {
      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe ACCEPTED

    }

    "send a request to EIS" in {
      await(createWithSuccessfulAuth.submit(request))

      val captor = ArgCaptor[DataRequest[_]]
      verify(connector).submitExciseMovement(
        captor.capture,
        eqTo("IE815")
      )(any)

      verifyDataRequest(captor.value)
    }

    "return an error when EIS error" in {

      when(connector.submitExciseMovement(any, any)(any))
        .thenReturn(Future.successful(Left(NotFound("not found"))))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe NOT_FOUND
    }

    "a validation parser error" when {
      "xml cannot be parsed" in {
        val result = createWithFailingXmlParserAction.submit(request)

        status(result) mustBe BAD_REQUEST
      }
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.submit(request)

        status(result) mustBe FORBIDDEN
      }
    }

    "return a consignor validation error" when {
      "consignor is not valid" in {
        val result = createWithValidateConsignorActionFailure.submit(request)

        status(result) mustBe FORBIDDEN
      }
    }
  }

  private def verifyDataRequest(actual: DataRequest[_]) = {
    actual.consignorId mustBe "123"
    actual.consigneeId mustBe Some("456")
    actual.localRefNumber mustBe "789"
    actual.internalId mustBe "1234"
  }

  private def createWithAuthActionFailure =
    new DraftExciseMovementController(
      FakeFailingAuthentication,
      FakeSuccessIE815XMLParser(ieMessage),
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
      FakeSuccessIE815XMLParser(ieMessage),
      FakeSuccessfulValidateConsignorAction,
      connector,
      cc
    )

  private def createWithValidateConsignorActionFailure =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessIE815XMLParser(ieMessage),
      FakeFailureValidateConsignorAction,
      connector,
      cc
    )

   private def createRequest(body: Elem): FakeRequest[Elem] = {
    FakeRequest("POST", "/foo")
      .withHeaders(FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml")))
      .withBody(body)


  }
}
