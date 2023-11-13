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

package uk.gov.hmrc.excisemovementcontrolsystemapi.service

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.InternalServerError
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest, ValidatedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{NrsService, SubmissionMessageServiceImpl}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SubmissionMessageServiceSpec
  extends PlaySpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val connector = mock[EISSubmissionConnector]
  private val nrsService = mock[NrsService]
  private val sut = new SubmissionMessageServiceImpl(connector, nrsService)

  private val message = mock[IE815Message]
  val notableEventId = "notableEventId"
  val fakeRequest = FakeRequest()
    .withBody("<IE815>test</IE815>")
    .withHeaders(
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml"))
    )

  private val enrolmentRequest = EnrolmentRequest(fakeRequest, Set("ern"), "123")
  private val parsedXmlRequest = ParsedXmlRequest(enrolmentRequest, message, Set("ern"), "123")
  private val request = ValidatedXmlRequest(parsedXmlRequest, Set("ern"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector, nrsService)

    when(message.consignorId).thenReturn("1234")
  }
  "submit" should {
    "submit a message" in {

      when(connector.submitMessage(any)(any))
        .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "IE815", "correlationId"))))

      when(nrsService.submitNrs(any)(any))
        .thenReturn(Future.successful(Right(NonRepudiationSubmissionAccepted("submissionId"))))

      sut.submit(request).futureValue

      verify(connector).submitMessage(eqTo(request))(any)

    }

    "return EISSubmissionResponse" in {
      when(connector.submitMessage(any)(any))
        .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "IE815", "correlationId"))))

      val result = sut.submit(request).futureValue

      result mustBe Right(EISSubmissionResponse("ok", "IE815", "correlationId"))
    }

    "return an error" in {
      when(connector.submitMessage(any)(any))
        .thenReturn(Future.successful(Left(InternalServerError("error"))))

      val result = sut.submit(request).futureValue

      result.left.value mustBe InternalServerError("error")

      withClue("not send to NRS") {
        verifyZeroInteractions(nrsService)
      }
    }

    "send to NRS when submission is successful" in {
      when(connector.submitMessage(any)(any))
        .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "IE815", "correlationId"))))

      sut.submit(request)

      verify(nrsService).submitNrs(eqTo(request))(any)
    }

    "return response if NRS fails" in {

      when(connector.submitMessage(any)(any))
        .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "IE815", "correlationId"))))

      when(nrsService.submitNrs(any)(any))
        .thenReturn(Future.successful(Left(INTERNAL_SERVER_ERROR)))

      val result = sut.submit(request).futureValue

      result mustBe Right(EISSubmissionResponse("ok", "IE815", "correlationId"))
    }
  }
}
