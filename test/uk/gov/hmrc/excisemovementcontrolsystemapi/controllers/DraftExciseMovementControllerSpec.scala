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


import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.mockito.captor.ArgCaptor
import org.mongodb.scala.MongoException
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateErnInMessageAction, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.BoxNotificationResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, SubmissionMessageService, WorkItemService}

import java.time.Instant
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
  private val submissionMessageService = mock[SubmissionMessageService]
  private val movementMessageService = mock[MovementService]
  private val cc = stubControllerComponents()
  private val request = createRequest
  private val mockIeMessage = mock[IE815Message]
  private val workItemService = mock[WorkItemService]
  private val notificationConnector = mock[PushNotificationConnector]
  private val boxId = "boxId"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(submissionMessageService, movementMessageService, workItemService, submissionMessageService)

    when(submissionMessageService.submit(any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))
    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))
    when(notificationConnector.getBoxId(any)(any))
      .thenReturn(Future.successful(Right(BoxNotificationResponse(boxId))))

    when(mockIeMessage.consigneeId).thenReturn(Some("789"))
    when(mockIeMessage.consignorId).thenReturn("456")
    when(mockIeMessage.localReferenceNumber).thenReturn("123")
  }

  "submit" should {
    "return 200" in {
      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement("123", "456", Some("789"), None, Instant.now))))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe ACCEPTED

      withClue("submit hte message") {
        val captor = ArgCaptor[ValidatedXmlRequest[_]]
        verify(submissionMessageService).submit(captor.capture)(any)
        captor.value.message mustBe mockIeMessage
      }

      withClue("should get the box id") {
        verify(notificationConnector).getBoxId(eqTo("clientId"))(any)
      }

      withClue("should save the new movement") {
        val captor = ArgCaptor[Movement]
        verify(movementMessageService).saveNewMovement(captor.capture)
        captor.value.localReferenceNumber mustBe "123"
        captor.value.consignorId mustBe "456"
        captor.value.consigneeId mustBe Some("789")
        captor.value.administrativeReferenceCode mustBe None
        captor.value.messages mustBe Seq.empty
      }
    }

    "call the add work item routine to create or update the database" in {
      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement("lrn", ern, None))))

      await(createWithSuccessfulAuth.submit(request))

      verify(workItemService).addWorkItemForErn("456", fastMode = true)
    }

    "return ACCEPTED if failing to add workItem " in {
      when(movementMessageService.saveNewMovement(any))
        .thenReturn(Future.successful(Right(Movement("lrn", ern, None))))
      when(workItemService.addWorkItemForErn(any, any))
        .thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.submit(request)

      status(result) mustBe ACCEPTED
    }

    "return an error" when {
      "get box id return an error" in {
        when(movementMessageService.saveNewMovement(any))
          .thenReturn(Future.successful(Right(Movement("123", "456", Some("789"), None, Instant.now))))
        when(notificationConnector.getBoxId(any)(any))
          .thenReturn(Future.successful(Left(BadRequest("error"))))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe BAD_REQUEST

        withClue("should not submit the message") {
          verifyZeroInteractions(submissionMessageService)
        }
      }

      "clientId is not available" in {
        when(movementMessageService.saveNewMovement(any))
          .thenReturn(Future.successful(Right(Movement("123", "456", Some("789"), None, Instant.now))))

        val result = createWithSuccessfulAuth.submit(createRequestWithoutClientId)

        status(result) mustBe BAD_REQUEST
      }

      "cannot submit a message" in {
        when(submissionMessageService.submit(any)(any))
          .thenReturn(Future.successful(Left(NotFound("not found"))))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe NOT_FOUND
      }

      "message xml cannot be parsed" in {
        val result = createWithFailingXmlParserAction.submit(request)

        status(result) mustBe BAD_REQUEST
      }

      "authentication fails" in {
        val result = createWithAuthActionFailure.submit(request)

        status(result) mustBe FORBIDDEN
      }

      "consignor is not valid" in {
        val result = createWithValidateConsignorActionFailure.submit(request)

        status(result) mustBe FORBIDDEN
      }

      "cannot save the movement" in {
        when(movementMessageService.saveNewMovement(any))
          .thenReturn(Future.successful(Left(InternalServerError("error"))))

        val result = createWithSuccessfulAuth.submit(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  private def createWithAuthActionFailure =
    new DraftExciseMovementController(
      FakeFailingAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnInMessageAction(mockIeMessage),
      movementMessageService,
      workItemService,
      submissionMessageService,
      notificationConnector,
      cc
    )

  private def createWithFailingXmlParserAction =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeFailureXMLParser,
      FakeSuccessfulValidateErnInMessageAction(mockIeMessage),
      movementMessageService,
      workItemService,
      submissionMessageService,
      notificationConnector,
      cc
    )

  private def createWithSuccessfulAuth =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnInMessageAction(mockIeMessage),
      movementMessageService,
      workItemService,
      submissionMessageService,
      notificationConnector,
      cc
    )

  private def createWithValidateConsignorActionFailure =
    new DraftExciseMovementController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeFailureValidateErnInMessageAction,
      movementMessageService,
      workItemService,
      submissionMessageService,
      notificationConnector,
      cc
    )

  private def createRequest: FakeRequest[Elem] = {
    FakeRequest()
      .withHeaders(FakeHeaders(Seq(
        HeaderNames.CONTENT_TYPE -> "application/xml",
        "X-Client-Id" -> "clientId"
      )))
      .withBody(IE815)
  }

  private def createRequestWithoutClientId: FakeRequest[Elem] = {
    FakeRequest()
      .withHeaders(FakeHeaders(Seq(
        HeaderNames.CONTENT_TYPE -> "application/xml",
      )))
      .withBody(IE815)
  }
}
