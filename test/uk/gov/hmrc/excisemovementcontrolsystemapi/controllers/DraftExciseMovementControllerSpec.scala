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



import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mongodb.scala.MongoException
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.mvc.Results.{InternalServerError, NotFound}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateErnInMessageAction, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class DraftExciseMovementControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeXmlParsers
    with FakeValidateErnInMessageAction
    with TestXml
    with BeforeAndAfterEach
    with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val connector = mock[EISSubmissionConnector]
  private val movementMessageService = mock[MovementService]
  private val cc = stubControllerComponents()
  private val request = createRequest(IE815)
  private val mockIeMessage = mock[IE815Message]
  private val workItemService = mock[WorkItemService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector, movementMessageService, workItemService)

    when(connector.submitMessage(any)(any)).thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))

    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))

    when(mockIeMessage.consigneeId).thenReturn(Some("789"))
    when(mockIeMessage.consignorId).thenReturn("456")
    when(mockIeMessage.localReferenceNumber).thenReturn("123")
  }

  "submit" should {
    "return 200" in {
      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement("", "", None))))
      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe ACCEPTED

    }

    "send a request to EIS" in {
      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement("", "", None))))
      await(createWithSuccessfulAuth.submit(request))

      verify(connector).submitMessage(any)(any)

    }

    "call the add work item routine to create or update the database" in {

      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement("lrn", ern, None))))

      await(createWithSuccessfulAuth.submit(request))

      verify(workItemService).addWorkItemForErn("456", fastMode = true)

    }

    "catch Future failure from Work Item service and log it but still process submission" in {
      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement("lrn", ern, None))))

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe ACCEPTED

      verify(connector).submitMessage(any)(any)
    }

    "return an error when EIS error" in {
      when(connector.submitMessage(any)(any))
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

    "return 500 when message saving movement fails" in {
      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Left(InternalServerError("error"))))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  private def createWithAuthActionFailure =
    new DraftExciseMovementController(
      FakeFailingAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnInMessageAction(mockIeMessage),
      connector,
      movementMessageService,
      workItemService,
      cc
    )

  private def createWithFailingXmlParserAction =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeFailureXMLParser,
      FakeSuccessfulValidateErnInMessageAction(mockIeMessage),
      connector,
      movementMessageService,
      workItemService,
      cc
    )

  private def createWithSuccessfulAuth =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnInMessageAction(mockIeMessage),
      connector,
      movementMessageService,
      workItemService,
      cc
    )

  private def createWithValidateConsignorActionFailure =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeFailureValidateErnInMessageAction,
      connector,
      movementMessageService,
      workItemService,
      cc
    )

  private def createRequest(body: Elem): FakeRequest[Elem] = {
    FakeRequest("POST", "/foo")
      .withHeaders(FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml")))
      .withBody(body)
  }
}
