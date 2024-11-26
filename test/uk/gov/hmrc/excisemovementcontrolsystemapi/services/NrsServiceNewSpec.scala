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
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew.UnexpectedResponseException
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.NonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils, NrsEventIdMapper}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, PermanentlyFailed, Succeeded, ToDo}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class NrsServiceNewSpec extends PlaySpec with ScalaFutures with NrsTestData with EitherValues with BeforeAndAfterEach {

  implicit val ec: ExecutionContext        = ExecutionContext.global
  implicit val hc: HeaderCarrier           = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
  private val mockNrsConnectorNew              = mock[NrsConnectorNew]
  private val mockNrsWorkItemRepository        = mock[NRSWorkItemRepository]
  private val mockCorrelationIdService         = mock[CorrelationIdService]
  private val mockDateTimeService              = mock[DateTimeService]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockLockRepository: MongoLockRepository = mock[MongoLockRepository]
  private val mockTimeStampSupport: TimestampSupport = mock[TimestampSupport]
  private val timeStamp                    = Instant.now()

  private val service = new NrsServiceNew(
    mockAuthConnector,
    mockNrsConnectorNew,
    mockNrsWorkItemRepository,
    mockDateTimeService,
    new EmcsUtils,
    new NrsEventIdMapper,
    mockCorrelationIdService,
    mockLockRepository,
    mockTimeStampSupport
  )

  private val message           = mock[IE815Message]
  private val testCorrelationId = "testCorrelationId"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockAuthConnector,
      mockNrsConnectorNew,
      mockDateTimeService,
      mockCorrelationIdService,
      mockNrsWorkItemRepository,
      mockLockRepository,
      mockTimeStampSupport
    )

    when(mockDateTimeService.timestamp()).thenReturn(timeStamp)
    when(mockCorrelationIdService.generateCorrelationId()).thenReturn(testCorrelationId)
    when(mockAuthConnector.authorise[NonRepudiationIdentityRetrievals](any, any)(any, any)) thenReturn
      Future.successful(testAuthRetrievals)
    when(mockNrsConnectorNew.sendToNrs(any, any)(any))
      .thenReturn(Future.successful(Done))
    when(message.consignorId).thenReturn("ern")
  }

  private val testRequest     = createRequest(message)
  private val testNrsMetadata = NrsMetadata(
    businessId = "emcs",
    notableEvent = "excise-movement-control-system",
    payloadContentType = "application/json",
    payloadSha256Checksum = sha256Hash("payload for NRS"),
    userSubmissionTimestamp = timeStamp.toString,
    identityData = testNrsIdentityData,
    userAuthToken = testAuthToken,
    headerData = Map(),
    searchKeys = Map("ern" -> "123")
  )

  private val encodedMessage  = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
  private val testNrsPayload  = NrsPayload(encodedMessage, testNrsMetadata)
  private val testNrsWorkItem = NrsSubmissionWorkItem(testNrsPayload)
  private val testWorkItem = WorkItem(new ObjectId(), timeStamp, timeStamp, timeStamp, ToDo, 0, testNrsWorkItem)

  "makeNrsWorkItemAndAddToRepository" should {
    "create the NRSSubmission model and call to add it to the NRSWorkItemRepository" in {

      when(mockNrsWorkItemRepository.pushNew(any(), any(), any()))
        .thenReturn(
          Future.successful(
            WorkItem(new ObjectId(), timeStamp, timeStamp, timeStamp, ToDo, 0, NrsSubmissionWorkItem(testNrsPayload))
          )
        )

      val result = await(service.makeWorkItemAndQueue(testRequest, "ern"))

      verify(mockNrsWorkItemRepository).pushNew(NrsSubmissionWorkItem(testNrsPayload))
      result mustBe Done
    }
  }

  "submitNrs" should {
    "submit to NRS and call the repository to mark the workitem as done if it succeeds with ACCEPTED" in {

      when(mockNrsConnectorNew.sendToNrs(any(), any())(any())).thenReturn(Future.successful(Done))

      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

      val result = await(service.submitNrs(testWorkItem))

      verify(mockNrsConnectorNew, times(1)).sendToNrs(testNrsPayload, testCorrelationId)

      result mustBe Done
      verify(mockNrsWorkItemRepository).complete(testWorkItem.id, Succeeded)
    }
    "mark the workItem as failed if submission fails with 5xx" in {
      when(mockNrsConnectorNew.sendToNrs(any(), any())(any()))
        .thenReturn(Future.failed(UnexpectedResponseException(INTERNAL_SERVER_ERROR, "body")))

      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

      val result = await(service.submitNrs(testWorkItem))

      verify(mockNrsConnectorNew, times(1)).sendToNrs(testNrsPayload, testCorrelationId)
      result mustBe Done
      verify(mockNrsWorkItemRepository).complete(testWorkItem.id, Failed)
    }
    "mark the workItem as PERMANENTLY failed if submission fails with 4xx" in {
      when(mockNrsConnectorNew.sendToNrs(any(), any())(any()))
        .thenReturn(Future.failed(UnexpectedResponseException(BAD_REQUEST, "body")))

      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

      val result = await(service.submitNrs(testWorkItem))

      verify(mockNrsConnectorNew, times(1)).sendToNrs(testNrsPayload, testCorrelationId)
      result mustBe Done
      verify(mockNrsWorkItemRepository).complete(testWorkItem.id, PermanentlyFailed)
    }
    "mark the workItem as PERMANENTLY failed if submission fails with a different status" in {
      when(mockNrsConnectorNew.sendToNrs(any(), any())(any()))
        .thenReturn(Future.failed(UnexpectedResponseException(SEE_OTHER, "body")))

      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

      val result = await(service.submitNrs(testWorkItem))

      verify(mockNrsConnectorNew, times(1)).sendToNrs(testNrsPayload, testCorrelationId)
      result mustBe Done
      verify(mockNrsWorkItemRepository).complete(testWorkItem.id, PermanentlyFailed)
    }
  }

  "processSingleNrs" should {
    "call to submit the NRS data and return true if it processed a thing successfully" in {


      when(mockNrsConnectorNew.sendToNrs(any(), any())(any())).thenReturn(Future.successful(Done))

      when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
        .thenReturn(Future.successful(Some(testWorkItem)))
      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

      val result = await(service.processSingleNrs())

      verify(mockNrsConnectorNew, times(1)).sendToNrs(testNrsPayload, testCorrelationId)
      verify(mockNrsWorkItemRepository).complete(testWorkItem.id, Succeeded)
      result mustBe true
    }
    "call to submit the NRS data and return true if it tried to process a thing but failed" in {

      when(mockNrsConnectorNew.sendToNrs(any(), any())(any()))
        .thenReturn(Future.failed(UnexpectedResponseException(INTERNAL_SERVER_ERROR, "body")))

      when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
        .thenReturn(Future.successful(Some(testWorkItem)))
      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

      val result = await(service.processSingleNrs())

      verify(mockNrsConnectorNew, times(1)).sendToNrs(testNrsPayload, testCorrelationId)
      verify(mockNrsWorkItemRepository).complete(testWorkItem.id, Failed)
      result mustBe true
    }
    "return false if there was nothing in the repository to process" in {

      when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.processSingleNrs())
      result mustBe false
    }
  }

  "processAllWithLock" should {
    "when a lock is available" should {
      val lock = Lock("id", "owner", timeStamp, timeStamp.plus(1, ChronoUnit.HOURS))

      "call NRS multiple times if there are more than one submission to process" in {
        when(mockTimeStampSupport.timestamp()).thenReturn(timeStamp)
        when(mockLockRepository.refreshExpiry(any, any, any))
          .thenReturn(Future.successful(false))
        when(mockLockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(lock)))
        when(mockLockRepository.releaseLock(any(), any())).thenReturn(Future.unit)

        when(mockNrsConnectorNew.sendToNrs(any(), any())(any())).thenReturn(Future.successful(Done))
        when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any())).thenReturn(
          Future.successful(Some(testWorkItem)),
          Future.successful(Some(testWorkItem)),
          Future.successful(Some(testWorkItem)),
          Future.successful(None)
        )

        service.processAllWithLock().futureValue

        verify(mockLockRepository, times(1)).refreshExpiry(any,any,any)
        verify(mockLockRepository, times(1)).takeLock(any,any,any)

        verify(mockNrsWorkItemRepository, times(4)).pullOutstanding(any(), any())
        verify(mockNrsWorkItemRepository, times(3)).complete(testWorkItem.id, Succeeded)
      }
      "not call NRS if there is nothing to process" in {
        when(mockLockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(lock)))
        when(mockLockRepository.releaseLock(any(), any())).thenReturn(Future.unit)

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(None))

        service.processAllWithLock().futureValue

        verify(mockNrsWorkItemRepository, times(1)).pullOutstanding(any(), any())
        verify(mockNrsWorkItemRepository, never()).complete(any(), any())
      }
    }
    "when a lock is not available" should {
      "not do anything" in {
        when(mockLockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(None))
        when(mockLockRepository.releaseLock(any(), any())).thenReturn(Future.unit)

        when(mockNrsConnectorNew.sendToNrs(any(), any())(any())).thenReturn(Future.successful(Done))
        when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any())).thenReturn(
          Future.successful(Some(testWorkItem)),
          Future.successful(Some(testWorkItem)),
          Future.successful(Some(testWorkItem)),
          Future.successful(None)
        )

        service.processAllWithLock().futureValue

        verify(mockNrsWorkItemRepository, never()).pullOutstanding(any(), any())
      }
    }
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
