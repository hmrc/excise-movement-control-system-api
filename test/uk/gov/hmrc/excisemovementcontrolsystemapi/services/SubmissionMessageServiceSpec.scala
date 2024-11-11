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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SubmissionMessageServiceSpec extends PlaySpec with ScalaFutures with EitherValues with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val connector               = mock[EISSubmissionConnector]
  private val nrsService              = mock[NrsService]
  private val correlationIdService    = mock[CorrelationIdService]
  private val ernSubmissionRepository = mock[ErnSubmissionRepository]
  private val sut                     =
    new SubmissionMessageServiceImpl(connector, nrsService, correlationIdService, ernSubmissionRepository)

  private val message                  = mock[IE815Message]
  private val xmlBody                  = "<IE815>test</IE815>"
  val fakeRequest: FakeRequest[String] = FakeRequest()
    .withBody(xmlBody)
    .withHeaders(
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml"))
    )

  private val ern              = "ern"
  private val enrolmentRequest = EnrolmentRequest(fakeRequest, Set(ern), "123")
  private val request          = ParsedXmlRequest(enrolmentRequest, message, Set(ern), "123")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector, nrsService, ernSubmissionRepository)

    when(message.consignorId).thenReturn("1234")
    when(correlationIdService.generateCorrelationId()).thenReturn("correlationId")
    when(connector.submitMessage(any, any, any, any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "IE815", "correlationId"))))
    when(nrsService.submitNrsOld(any, any, any)(any))
      .thenReturn(Future.successful(Done))
    when(ernSubmissionRepository.save(any)).thenReturn(Future.successful(Done))
  }

  "submit" should {
    "submit a message" in {
      await(sut.submit(request, ern))

      verify(connector).submitMessage(
        eqTo(message),
        eqTo(xmlBody),
        eqTo(ern),
        eqTo("correlationId")
      )(any)

      withClue("send to NRS when submitMessage is successful") {
        verify(nrsService).submitNrsOld(eqTo(request), eqTo(ern), eqTo("correlationId"))(any)
      }

      withClue("update the last submitted time for the ern") {
        verify(ernSubmissionRepository).save(ern)
      }
    }

    "return EISSubmissionResponse" in {
      val result = await(sut.submit(request, ern))

      result mustBe Right(EISSubmissionResponse("ok", "IE815", "correlationId"))
    }

    "return an error" in {
      when(connector.submitMessage(any, any, any, any)(any))
        .thenReturn(Future.successful(Left(InternalServerError("error"))))

      val result = await(sut.submit(request, ern))

      result.left.value mustBe InternalServerError("error")

      withClue("not send to NRS") {
        verifyZeroInteractions(nrsService)
      }

      withClue("not update last submitted time for ern") {
        verifyZeroInteractions(ernSubmissionRepository)
      }
    }

    "return submit message result" when {
      "NRS fails" in {
        when(nrsService.submitNrsOld(any, any, any)(any))
          .thenReturn(Future.successful(Done))

        val result = await(sut.submit(request, ern))

        result mustBe Right(EISSubmissionResponse("ok", "IE815", "correlationId"))
      }

      "NRS throw" in {
        when(nrsService.submitNrsOld(any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("NRS error")))

        val result = await(sut.submit(request, ern))

        result mustBe Right(EISSubmissionResponse("ok", "IE815", "correlationId"))
      }
    }
  }
}
