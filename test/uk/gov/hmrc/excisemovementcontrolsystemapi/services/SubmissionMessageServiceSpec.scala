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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.EISSubmissionConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EISErrorResponseDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815MessageV1
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class SubmissionMessageServiceSpec extends PlaySpec with ScalaFutures with EitherValues with BeforeAndAfterEach {

  val correalationId                = "some-id"
  implicit val hc: HeaderCarrier    =
    HeaderCarrierConverter.fromRequest(FakeRequest().withHeaders(HttpHeader.xCorrelationId -> correalationId))
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockAppconfig           = mock[AppConfig]
  private val connector               = mock[EISSubmissionConnector]
  private val nrsServiceNew           = mock[NrsService]
  private val ernSubmissionRepository = mock[ErnSubmissionRepository]
  private val sut                     = new SubmissionMessageServiceImpl(
    connector,
    nrsServiceNew,
    ernSubmissionRepository,
    mockAppconfig
  )

  private val message                  = mock[IE815MessageV1]
  private val xmlBody                  = "<IE815>test</IE815>"
  val fakeRequest: FakeRequest[String] = FakeRequest()
    .withBody(xmlBody)
    .withHeaders(
      FakeHeaders(Seq(HeaderNames.CONTENT_TYPE -> "application/xml"))
    )

  private val ern              = "ern"
  private val enrolmentRequest = EnrolmentRequest(fakeRequest, Set(ern), UserDetails("abc", "123"))
  private val request          = ParsedXmlRequest(enrolmentRequest, message, Set(ern), UserDetails("abc", "123"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector, nrsServiceNew, ernSubmissionRepository, mockAppconfig)

    when(message.consignorId).thenReturn(Some("1234"))
    when(connector.submitMessage(any, any, any)(any))
      .thenReturn(Future.successful(Right(EISSubmissionResponse("ok", "IE815", "correlationId"))))
  }

  "submit using new NRS implementation" when {
    "submission succeeds" should {
      "return an EISSubmissionResponse, save to the submission repository and call to queue NRS submission" in {
        when(nrsServiceNew.makeWorkItemAndQueue(any, any)(any)).thenReturn(Future.successful(Done))
        when(ernSubmissionRepository.save(any)).thenReturn(Future.successful(Done))
        val result = await(sut.submit(request, ern))

        result mustBe Right(EISSubmissionResponse("ok", "IE815", "correlationId"))

        verify(connector).submitMessage(
          eqTo(message),
          eqTo(xmlBody),
          eqTo(ern)
        )(any)

        withClue("send to NRS when submitMessage is successful") {
          verify(nrsServiceNew).makeWorkItemAndQueue(eqTo(request), eqTo(ern))(any)
        }

        withClue("update the last submitted time for the ern") {
          verify(ernSubmissionRepository).save(ern)
        }
      }
      "throw an exception but still call to queue NRS submission if message submission succeeds but the repository call fails" in {
        when(nrsServiceNew.makeWorkItemAndQueue(any, any)(any)).thenReturn(Future.successful(Done))
        when(ernSubmissionRepository.save(any)).thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.submit(request, ern))

        result mustBe Right(EISSubmissionResponse("ok", "IE815", "correlationId"))

        verify(connector).submitMessage(
          eqTo(message),
          eqTo(xmlBody),
          eqTo(ern)
        )(any)

        withClue("send to NRS when submitMessage is successful") {
          verify(nrsServiceNew).makeWorkItemAndQueue(eqTo(request), eqTo(ern))(any)
        }

        withClue("update the last submitted time for the ern") {
          verify(ernSubmissionRepository).save(ern)
        }
      }
    }
    "submission fails"    should {
      "return an EISErrorResponsePresentation" in {
        when(nrsServiceNew.makeWorkItemAndQueue(any, any)(any)).thenReturn(Future.successful(Done))
        when(ernSubmissionRepository.save(any)).thenReturn(Future.successful(Done))

        val testError =
          EISErrorResponseDetails(INTERNAL_SERVER_ERROR, Instant.now(), "message", "debug", "cId", None)

        when(connector.submitMessage(any, any, any)(any))
          .thenReturn(Future.successful(Left(testError)))

        val result = await(sut.submit(request, ern))

        result.left.value mustBe testError

        withClue("not send to NRS") {
          verifyZeroInteractions(nrsServiceNew)
        }

        withClue("not update last submitted time for ern") {
          verifyZeroInteractions(ernSubmissionRepository)
        }
      }
    }
  }
}
