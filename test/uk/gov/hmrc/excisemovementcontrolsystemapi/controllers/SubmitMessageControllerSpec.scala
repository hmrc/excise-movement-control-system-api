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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.mvc.Results.NotFound
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateErnInMessageAction, FakeValidateLRNAction, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{SubmissionMessageService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{EmcsUtils, ErnsMapper}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class SubmitMessageControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeXmlParsers
    with FakeValidateErnInMessageAction
    with FakeValidateLRNAction
    with BeforeAndAfterEach
    with TestXml {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val request = createRequest(IE818)
  private val ieMessage = mock[IEMessage]
  private val submissionMessageService = mock[SubmissionMessageService]
  private val workItemService = mock[WorkItemService]
  private val ernsMapper = mock[ErnsMapper]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(submissionMessageService, workItemService)

    when(submissionMessageService.submit(any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "success", "123"))))
    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))
    when(ernsMapper.getSingleErnFromMessage(any, any)).thenReturn("testErn")
  }

  "submit" should {
    "return 200" in {
      val result = createWithSuccessfulAuth.submit("LRN")(request)
      status(result) mustBe ACCEPTED
    }

    "send a request to EIS" in {

      await(createWithSuccessfulAuth.submit("lrn")(request))

      verify(submissionMessageService).submit(any)(any)

    }

    "call the add work item routine to create or update the database" in {

      await(createWithSuccessfulAuth.submit("LRN")(request))

      verify(workItemService).addWorkItemForErn("testErn", fastMode = true)

    }

    "return an error when EIS error" in {
      when(submissionMessageService.submit(any)(any))
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

    "return an ern validation error" when {
      "consignee is not valid" in {
        val result = createWithValidateConsignorActionFailure.submit("lrn")(request)

        status(result) mustBe FORBIDDEN
      }
    }

    "return a ern / lrn mismatch error" when {
      "lrn and ern are not in the database" in {
        val result = createWithLRNValidationError.submit("LRN")(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "catch Future failure from Work Item service and log it but still process submission" in {

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.submit("lrn")(request)

      status(result) mustBe ACCEPTED

      verify(submissionMessageService).submit(any)(any)
    }

  }

  private def createWithSuccessfulAuth =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnInMessageAction(ieMessage),
      FakeSuccessfulValidateLRNAction,
      submissionMessageService,
      workItemService,
      ernsMapper,
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
      FakeSuccessfulValidateErnInMessageAction(ieMessage),
      FakeFailureValidateLRNAction,
      submissionMessageService,
      workItemService,
      ernsMapper,
      cc
    )

  private def createWithFailingXmlParserAction =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeFailureXMLParser,
      FakeSuccessfulValidateErnInMessageAction(ieMessage),
      FakeFailureValidateLRNAction,
      submissionMessageService,
      workItemService,
      ernsMapper,
      cc
    )

  private def createWithValidateConsignorActionFailure =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeFailureValidateErnInMessageAction,
      FakeFailureValidateLRNAction,
      submissionMessageService,
      workItemService,
      ernsMapper,
      cc
    )

  private def createWithLRNValidationError =
    new SubmitMessageController(
      FakeSuccessAuthentication,
      FakeSuccessXMLParser,
      FakeSuccessfulValidateErnInMessageAction(ieMessage),
      FakeFailureValidateLRNAction,
      submissionMessageService,
      workItemService,
      ernsMapper,
      cc
    )

}
