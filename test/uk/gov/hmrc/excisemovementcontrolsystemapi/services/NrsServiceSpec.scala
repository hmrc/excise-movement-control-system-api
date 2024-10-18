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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.NonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class NrsServiceSpec extends PlaySpec with ScalaFutures with NrsTestData with EitherValues with BeforeAndAfterEach {

  implicit val ec: ExecutionContext        = ExecutionContext.global
  implicit val hc: HeaderCarrier           = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
  private val nrsConnector                 = mock[NrsConnector]
  private val dateTimeService              = mock[DateTimeService]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val timeStamp                    = Instant.now()
  private val userHeaderData               = Seq("header" -> "test")
  private val service                      = new NrsService(
    authConnector,
    nrsConnector,
    dateTimeService,
    new EmcsUtils,
    new NrsEventIdMapper
  )

  private val message = mock[IE815Message]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector, nrsConnector, dateTimeService)

    when(dateTimeService.timestamp()).thenReturn(timeStamp)
    when(authConnector.authorise[NonRepudiationIdentityRetrievals](any, any)(any, any)) thenReturn
      Future.successful(testAuthRetrievals)
    when(nrsConnector.sendToNrs(any)(any))
      .thenReturn(Future.successful(NonRepudiationSubmissionAccepted("submissionId")))
    when(message.consignorId).thenReturn("ern")
  }

  "submitNrs" should {
    "return NonRepudiationSubmissionAccepted" in {
      submitNrs(hc) mustBe NonRepudiationSubmissionAccepted("submissionId")
    }

    "submit nrs payload" in {
      submitNrs(hc)

      val encodePayload = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
      val nrsPayload    = NrsPayload(encodePayload, createExpectedMetadata)

      verify(nrsConnector).sendToNrs(eqTo(nrsPayload))(eqTo(hc))
    }

    "return an error" when {
      "NRS submit request fails" in {
        when(nrsConnector.sendToNrs(any)(any))
          .thenReturn(Future.successful(NonRepudiationSubmissionFailed(INTERNAL_SERVER_ERROR, "any reason")))

        submitNrs(hc) mustBe NonRepudiationSubmissionFailed(INTERNAL_SERVER_ERROR, "any reason")
      }

      "cannot retrieve user AuthToken" in {
        val result = submitNrs(HeaderCarrier())

        result mustBe NonRepudiationSubmissionFailed(INTERNAL_SERVER_ERROR, "No auth token available for NRS")
      }
    }
  }

  private def createExpectedMetadata =
    NrsMetadata(
      businessId = "emcs",
      notableEvent = "emcs-create-a-movement-api",
      payloadContentType = "application/xml",
      payloadSha256Checksum = sha256Hash("<IE815>test</IE815>"),
      userSubmissionTimestamp = timeStamp.toString,
      identityData = testNrsIdentityData,
      userAuthToken = testAuthToken,
      headerData = userHeaderData.toMap,
      searchKeys = Map("ern" -> "ern")
    )

  private def submitNrs(hc: HeaderCarrier): NonRepudiationSubmission = {

    val request = createRequest(message)

    await(service.submitNrs(request, "ern")(hc))
  }

  private def createRequest(message: IEMessage): ParsedXmlRequest[_] = {
    val fakeRequest = FakeRequest()
      .withBody("<IE815>test</IE815>")
      .withHeaders(
        FakeHeaders(Seq("header" -> "test"))
      )

    val enrolmentRequest = EnrolmentRequest(fakeRequest, Set("ern"), "123")
    ParsedXmlRequest(enrolmentRequest, message, Set("ern"), "123")
  }
}
