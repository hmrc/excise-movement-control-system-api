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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, times, verify, verifyZeroInteractions, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementService, NewMessageParserService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, TestUtils}
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant
import scala.concurrent.Future.successful
import scala.concurrent.duration.{Duration, HOURS, MINUTES, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

class PollingNewMessagesWithWorkItemJobSpec
  extends PlaySpec
    with BeforeAndAfterEach
    with NewMessagesXml {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val appConfig = mock[AppConfig]
  private val newMessageService = mock[GetNewMessageService]
  private val movementService = mock[MovementService]
  private val newMessageParserService = mock[NewMessageParserService]
  private val lockRepository = mock[MongoLockRepository]
  private val workItemService = mock[WorkItemService]
  private val dateTimeService = mock[DateTimeService]
  private val message = mock[IEMessage]
  private val newMessageResponse = EISConsumptionResponse(
    Instant.parse("2023-05-06T09:10:13Z"),
    "123",
    "any message"
  )
  private val newMessageResponseEmpty = EISConsumptionResponse(
    Instant.parse("2023-05-06T09:10:13Z"),
    "123",
    emptyNewMessageDataXml.toString()
  )

  private val job = new PollingNewMessagesWithWorkItemJob(
    lockRepository,
    newMessageService,
    workItemService,
    movementService,
    newMessageParserService,
    appConfig,
    dateTimeService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, appConfig, newMessageService, newMessageParserService, lockRepository, workItemService)

    when(lockRepository.takeLock(any, any, any)).thenReturn(Future.successful(true))
    when(lockRepository.releaseLock(any, any)).thenReturn(successful(()))
    when(dateTimeService.timestamp()).thenReturn(Instant.now())

    when(appConfig.maxFailureRetryAttempts).thenReturn(3)
    when(appConfig.workItemFastInterval).thenReturn(Duration.create(5, MINUTES))
    when(appConfig.workItemSlowInterval).thenReturn(Duration.create(1, HOURS))
    when(appConfig.failureRetryAfter).thenReturn(Duration.create(5, MINUTES))

    when(newMessageParserService.extractMessages(any)).thenReturn(Seq(message))
    when(movementService.updateMovement(any, any)).thenReturn(Future.successful(true))
    when(workItemService.markAs(any, any, any)).thenReturn(Future.successful(true))
    when(workItemService.rescheduleWorkItem(any)).thenReturn(Future.successful(true))

  }

  "Job" should {

    "have interval defined" in {
      val interval = Duration.create(5, SECONDS)
      when(appConfig.interval).thenReturn(interval)
      job.intervalBetweenJobRunning mustBe interval
    }

    "have initial delay defined" in {
      val initialDelay = Duration.create(5, SECONDS)
      when(appConfig.initialDelay).thenReturn(initialDelay)
      job.initialDelay mustBe initialDelay
    }

    "acquire a mongo lock" in {
      when(workItemService.pullOutstanding(any, any)).thenReturn(Future.successful(None))
      setGetNewMessagesAndAcknowledgeResponse(5)
      await(job.executeInMutex)

      verify(lockRepository).takeLock(eqTo("PollingNewMessageWithWorkItem"), any, any)

      withClue("release the mongo lock") {
        verify(lockRepository).releaseLock(eqTo("PollingNewMessageWithWorkItem"), any)
      }
    }

    "should reschedule workItem when successfully run" in {

      val workItem = createWorkItem()

      addOneItemToMockQueue(workItem)

      setGetNewMessagesAndAcknowledgeResponse(10)

      await(job.executeInMutex)

      verify(workItemService).rescheduleWorkItem(eqTo(workItem))
    }

    "should immediately add workItem back to queue when successfully run and remaining messages" in {

      val workItem = createWorkItem()

      addOneItemToMockQueue(workItem)

      setGetNewMessagesAndAcknowledgeResponse(17)

      await(job.executeInMutex)

      verify(workItemService).markAs(eqTo(workItem.id), eqTo(ToDo), eqTo(Some(workItem.availableAt)))
    }

    "catch any exception Mongo throws and error" in {
      val workItem = createWorkItem()
      addOneItemToMockQueue(workItem)
      when(workItemService.pullOutstanding(any, any)).thenReturn(
        Future.failed(new RuntimeException("error"))
      )

      val result = await(job.executeInMutex)

      result.message mustBe
        """The execution of scheduled job polling-new-messages failed with error 'error'.
          |The next execution of the job will do retry."""
          .stripMargin
          .replace('\n', ' ')
    }

    "retry failing EIS and mark a workItem as Failed" in {
      val workItem = createWorkItem()
      addOneItemToMockQueue(workItem)

      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      await(job.executeInMutex)

      verify(workItemService).markAs(
        eqTo(workItem.id),
        eqTo(ProcessingStatus.Failed),
        any
      )
    }

    "set the workItem state to slow poll when failed three times" in {
      val retryAttempt = 2
      val workItem = createWorkItem(retryAttempt)
      addOneItemToMockQueue(workItem)

      when(workItemService.rescheduleWorkItemForceSlow(any)).thenReturn(Future.successful(true))
      when(appConfig.maxFailureRetryAttempts).thenReturn(retryAttempt)
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(job.executeInMutex)

      verify(workItemService).rescheduleWorkItemForceSlow(eqTo(workItem))
      result.message mustBe "polling-new-messages Job ran successfully."
    }

    "parse the messages" in {
      addOneItemToMockQueue()

      setGetNewMessagesAndAcknowledgeResponse(5)

      await(job.executeInMutex)

      verify(newMessageParserService).extractMessages(eqTo("any message"))
    }

    "send getNewMessage request for each pending ern if there are multiple" in {

      addTwoItemsToMockQueue(createWorkItem(), createWorkItem(ern = "124"))

      setGetNewMessagesAndAcknowledgeResponse(5)

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-messages Job ran successfully."
      val captor = ArgCaptor[String]
      verify(newMessageService, times(2)).getNewMessagesAndAcknowledge(captor.capture)(any)

      captor.values mustBe Seq("123", "124")

      withClue("should save message to database") {
        verify(movementService).updateMovement(eqTo(message), eqTo("123"))
        verify(movementService).updateMovement(eqTo(message), eqTo("124"))
      }

    }

    "not process any Work Items if no Work Items exist" in {
      when(workItemService.pullOutstanding(any, any)).thenReturn(Future.successful(None))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-messages Job ran successfully."
      verifyZeroInteractions(newMessageService)
    }

    "not save message in the movement database" when {
      "API response has no messages inside" in {
        addOneItemToMockQueue()
        setGetNewMessagesAndAcknowledgeResponse()

        when(newMessageParserService.extractMessages(any)).thenReturn(Seq.empty)

        val result = await(job.executeInMutex)

        result.message mustBe "polling-new-messages Job ran successfully."
        verify(movementService, never()).updateMovement(any, any)
      }

      "API returns no message" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
          .thenReturn(Future.successful(None))

        val result = await(job.executeInMutex)

        result.message mustBe "polling-new-messages Job ran successfully."
        verify(movementService, never()).updateMovement(any, any)
      }

      "API return an error" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(job.executeInMutex)

        result.message mustBe "polling-new-messages Job ran successfully."
        verify(movementService, never()).updateMovement(any, any)
      }
    }

  }

  private def setGetNewMessagesAndAcknowledgeResponse(messageCount: Int = 0): Unit = {
    if (messageCount == 0) {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponseEmpty, 0))))
    } else {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, messageCount))))
    }
  }

  private def addOneItemToMockQueue(workItem1: WorkItem[ExciseNumberWorkItem] = createWorkItem()): Unit = {

    when(workItemService.pullOutstanding(any, any)).thenReturn(
      Future.successful(Some(workItem1)),
      Future.successful(None)
    )

  }

  private def addTwoItemsToMockQueue(workItem1: WorkItem[ExciseNumberWorkItem], workItem2: WorkItem[ExciseNumberWorkItem]): Unit = {

    when(workItemService.pullOutstanding(any, any)).thenReturn(
      Future.successful(Some(workItem1)),
      Future.successful(Some(workItem2)),
      Future.successful(None)
    )

  }

  private def createWorkItem(failureCount: Int = 0,
                             availableAt: Instant = Instant.now,
                             ern: String = "123"): WorkItem[ExciseNumberWorkItem] = {

    TestUtils.createWorkItem(
      ern = ern,
      availableAt = Instant.now,
      receivedAt = Instant.now,
      updatedAt = Instant.now,
      failureCount = failureCount,
      fastPollRetries = 3
    )
  }
}
