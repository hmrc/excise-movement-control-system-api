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
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.NonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils, NrsEventIdMapper}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class NrsServiceSpec extends PlaySpec with ScalaFutures with NrsTestData with EitherValues with BeforeAndAfterEach {

  implicit val ec: ExecutionContext        = ExecutionContext.global
  implicit val hc: HeaderCarrier           = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
  private val nrsConnector                 = mock[NrsConnector]
  private val nrsWorkItemRepository        = mock[NRSWorkItemRepository]
  private val dateTimeService              = mock[DateTimeService]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val timeStamp                    = Instant.now()
  private val userHeaderData               = Seq("header" -> "test")
  private val service                      = new NrsService(
    authConnector,
    nrsConnector,
    nrsWorkItemRepository,
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
    when(nrsConnector.sendToNrs(any, any)(any))
      .thenReturn(Future.successful(Done))
    when(message.consignorId).thenReturn("ern")
  }

  private val testRequest = createRequest(message)

  "makeNrsWorkItemAndAddToRepository" should {
    "create the NRSSubmission model and call to add it to the NRSWorkItemRepository" in {

      val encodedMessage = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
      val nrsMetaData =     NrsMetadata(
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
      val expectedPayload = NrsPayload(encodedMessage, nrsMetaData)

      when(nrsWorkItemRepository.pushNew(any(), any(), any()))
        .thenReturn(Future.successful(WorkItem(new ObjectId(),timeStamp,timeStamp,timeStamp,ToDo,0,NrsSubmissionWorkItem(expectedPayload))))

      val result = await(service.makeNrsWorkItemAndAddToRepository(testRequest, "ern", "correlationId")(hc))

      verify(nrsWorkItemRepository).pushNew(NrsSubmissionWorkItem(expectedPayload))
      result mustBe Done
    }
  }

  "submitNrsOld" should {
    "return Done" in {
      submitNrsOld(hc) mustBe Done
    }

    "submit nrs payload" in {
      submitNrsOld(hc)

      val encodePayload = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
      val nrsPayload    = NrsPayload(encodePayload, createExpectedMetadata)

      verify(nrsConnector).sendToNrs(eqTo(nrsPayload), eqTo("correlationId"))(eqTo(hc))
    }

    "return Done when there's an error" when {
      "NRS submit request fails" in {
        when(nrsConnector.sendToNrs(any, any)(any))
          .thenReturn(Future.successful(Done))

        submitNrsOld(hc) mustBe Done
      }

      "cannot retrieve user AuthToken" in {
        val result = submitNrsOld(HeaderCarrier())

        result mustBe Done
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

  private def submitNrsOld(hc: HeaderCarrier): Done = {

    val request = createRequest(message)

    await(service.submitNrsOld(request, "ern", "correlationId")(hc))
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
