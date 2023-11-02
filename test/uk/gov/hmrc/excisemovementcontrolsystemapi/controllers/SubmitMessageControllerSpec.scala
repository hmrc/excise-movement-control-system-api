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

import generated.IE818Type
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.mvc.Results.NotFound
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateErnsAction, FakeValidateLRNAction, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.DataRequestIE818
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class SubmitMessageControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeXmlParsers
    with FakeValidateErnsAction
    with FakeValidateLRNAction
    with BeforeAndAfterEach
    with TestXml {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val request = createRequest(IE818)
  private val movementService = mock[MovementService]
  private val ieMessage = mock[IEMessage]
  private val connector = mock[EISSubmissionConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector)

    when(connector.submitExciseMovement(any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))
    when(movementService.getMovementMessagesByLRNAndERNIn(any, any))
      .thenReturn(Future.successful(Some(Movement("lrn", "consignorId", None))))

    when(ieMessage.messageType).thenReturn("IE815")
    when(ieMessage.consigneeId).thenReturn(Some("GBWK002281023"))
  }

  "submit" should {
    "return 200" in {
      val result = createWithSuccessfulAuth.submit("LRN")(request)
      status(result) mustBe ACCEPTED
    }

    "send a request to EIS" in {

      await(createWithSuccessfulAuth.submit("lrn")(request))

      verify(connector).submitExciseMovement(any)(any)

    }

    "return an error when EIS error" in {
      when(connector.submitExciseMovement(any)(any))
        .thenReturn(Future.successful(Left(NotFound("not found"))))

      val result = createWithSuccessfulAuth.submit("lrn")(request)

      status(result) mustBe NOT_FOUND
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.submit("LRN")(request)

        status(result) mustBe FORBIDDEN
      }
    }

    "a validation parser error" when {
      "xml cannot be parsed" in {
        val result = createWithFailingXmlParserAction.submit("lrn")(request)

        status(result) mustBe BAD_REQUEST
      }
    }

    "return a consignor validation error" when {
      "consignor is not valid" in {
        val result = createWithValidateConsignorActionFailure.submit("lrn")(request)

        status(result) mustBe FORBIDDEN
      }
    }

    "return a ern / lrn mismatch error" when {
      "lrn and ern are not in the database" in {

        when(movementService.getMovementMessagesByLRNAndERNIn(any, any))
          .thenReturn(Future.successful(None))

        val result = createWithSuccessfulAuth.submit("LRN")(request)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  private def createWithSuccessfulAuth =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnsAction(ieMessage),
      FakeSuccessfulValidateLRNAction,
      connector,
      movementService,
      cc
    )

  private def createRequest(body: Elem): FakeRequest[Elem] = {
    FakeRequest("POST", "/foo")
      .withHeaders(FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml")))
      .withBody(body)
  }

  private def createWithAuthActionFailure =
    new SubmitMessageController(
      FakeFailingAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnsAction(ieMessage),
      FakeFailureValidateLRNAction,
      connector,
      movementService,
      cc
    )

  private def createWithFailingXmlParserAction =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeFailureXMLParser,
      FakeSuccessfulValidateErnsAction(ieMessage),
      FakeFailureValidateLRNAction,
      connector,
      movementService,
      cc
    )

  private def createWithValidateConsignorActionFailure =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeFailureValidateErnsAction,
      FakeFailureValidateLRNAction,
      connector,
      movementService,
      cc
    )

}
