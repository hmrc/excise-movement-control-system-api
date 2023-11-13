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
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest, ValidatedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NonRepudiationSubmissionAccepted, NrsMetadata, NrsPayload}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.NonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, ErnsMapper, NrsEventIdMapper}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}


/*
todo: Add more tests for:
1. retrieval identitydata fails
2. retirval userAuthentication fail.
3. submitToNrs fails
 */
class NrsServiceSpec
  extends PlaySpec
    with ScalaFutures
    with NrsTestData
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
  private val nrsConnector = mock[NrsConnector]
  private val dateTimeService = mock[DateTimeService]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val timeStamp = ZonedDateTime.now()
  private val userHeaderData = Seq("header" -> "test")
  private val service = new NrsService(
    authConnector,
    nrsConnector,
    dateTimeService,
    new EmcsUtils,
    new ErnsMapper,
    new NrsEventIdMapper
  )

  val message = mock[IE815Message]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector, nrsConnector, dateTimeService)

    when(dateTimeService.nowUtc).thenReturn(timeStamp)
    when(authConnector.authorise[NonRepudiationIdentityRetrievals](any, any)(any, any)) thenReturn
      Future.successful(testAuthRetrievals)
    when(nrsConnector.sendToNrs(any)(any)).
      thenReturn(Future.successful(Right(NonRepudiationSubmissionAccepted("submissionId"))))
    when(message.consignorId).thenReturn("ern")
  }

  "submitNrs" should {
    "return NonRepudiationSubmissionAccepted" in {
      submitNrs(hc) mustBe Right(NonRepudiationSubmissionAccepted("submissionId"))
    }

    "submit nrs payload" in {
      submitNrs(hc)

      val encodePayload = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
      val nrsPayload = NrsPayload(encodePayload, createExpectedMetadata)

      verify(nrsConnector).sendToNrs(eqTo(nrsPayload))(eqTo(hc))
    }

    "return an error" when {
      "NRS submit request fails" in {
        when(nrsConnector.sendToNrs(any)(any)).
          thenReturn(Future.successful(Left(INTERNAL_SERVER_ERROR)))

        submitNrs(hc).left.value mustBe INTERNAL_SERVER_ERROR
      }

      "cannot retrieve credential" in {
        when(authConnector.authorise[NonRepudiationIdentityRetrievals](any, any)(any, any)) thenReturn
          Future.failed(new RuntimeException("Cannot retrieve credential"))

        submitNrs(hc).left.value mustBe INTERNAL_SERVER_ERROR
      }

      "cannot retrieve user AuthToken" in {
        val result = submitNrs(HeaderCarrier())

        result.left.value mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  private def createExpectedMetadata = {
    NrsMetadata(
      businessId = "emcs-api",
      notableEvent = "emcs-create-a-movement-ui",
      payloadContentType = "application/xml",
      payloadSha256Checksum = sha256Hash("<IE815>test</IE815>"),
      userSubmissionTimestamp = timeStamp.toString,
      identityData = testNrsIdentityData,
      userAuthToken = testAuthToken,
      headerData = userHeaderData.toMap,
      searchKeys = Map("ERN" -> "ern")
    )
  }

  private def submitNrs(hc: HeaderCarrier): Either[Int, NonRepudiationSubmissionAccepted] = {



    val request =  createRequest(message)

    service.submitNrs(request)(hc).futureValue
  }

  private def createRequest(message: IE815Message): ValidatedXmlRequest[_] = {
    val fakeRequest = FakeRequest()
      .withBody("<IE815>test</IE815>")
      .withHeaders(
        FakeHeaders(Seq("header" -> "test"))
      )

    val enrolmentRequest = EnrolmentRequest(fakeRequest, Set("ern"), "123")
    val parsedXmlRequest = ParsedXmlRequest(enrolmentRequest, message, Set("ern"), "123")
    ValidatedXmlRequest(parsedXmlRequest, Set("ern"))
  }
}
