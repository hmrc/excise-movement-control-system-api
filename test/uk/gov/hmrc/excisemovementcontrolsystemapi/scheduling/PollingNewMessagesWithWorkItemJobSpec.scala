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

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, times, verify, verifyZeroInteractions, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatest.time.Minutes
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberQueueWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementService, NewMessageParserService}
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future.successful
import scala.concurrent.duration.{Duration, MINUTES, SECONDS}
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
  private val workItemRepository = mock[ExciseNumberQueueWorkItemRepository]
  private val dateTimeService = mock[TimestampSupport]
  private val message = mock[IEMessage]
  private val newMessageResponse = EISConsumptionResponse(
    LocalDateTime.of(2023, 5, 6, 9,10,13),
    "123",
    "any message"
  )

  private val job = new PollingNewMessageWithWorkItemJob(
    lockRepository,
    newMessageService,
    workItemRepository,
    movementService,
    newMessageParserService,
    appConfig,
    dateTimeService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, appConfig, newMessageService, newMessageParserService, lockRepository, workItemRepository)

    when(lockRepository.takeLock(any,any,any)).thenReturn(Future.successful(true))
    when(lockRepository.releaseLock(any,any)).thenReturn(successful(()))
    when(dateTimeService.timestamp()).thenReturn(Instant.now())
    when(appConfig.maxRetryAttempts).thenReturn(3)
    when(appConfig.runSubmissionWorkItemAfter).thenReturn(Duration.create(5, MINUTES))
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
      when(workItemRepository.pullOutstanding(any, any)).thenReturn(Future.successful(None))
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(Some(newMessageResponse)))
      await(job.executeInMutex)

      verify(lockRepository).takeLock(eqTo("PollingNewMessageWithWorkItem"), any, any)

      withClue("release the mongo lock") {
        verify(lockRepository).releaseLock(eqTo("PollingNewMessageWithWorkItem"), any)
      }
    }

    "should complete workItem in ToDo status when there are messages found" in {
      val workItem = createWorkItem()
      setUpWithTwoWorkItem(workItem)

      await(job.executeInMutex)

      verify(workItemRepository).completeAndDelete(eqTo(workItem.id))
    }

    "should mark as failed Work Item in ToDo when there are no messages found" in {

      val now = Instant.parse("2023-11-30T18:35:24.00Z")
      val fiveMinutesAfterNow = Instant.parse("2023-11-30T18:40:24.00Z")


      val workItem = createWorkItem(availableAt = now)
      setUpWithTwoWorkItem(workItem)

      await(job.executeInMutex)

      verify(workItemRepository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.Failed), eqTo(Some(fiveMinutesAfterNow)))
    }

    "should mark as permanently failed Work Item when there are no messages found three times" in {

      when(appConfig.maxRetryAttempts).thenReturn(2)

      val workItem = createWorkItem(failureCount = 2)
      setUpWithOneWorkItem(workItem)

      await(job.executeInMutex)

      verify(workItemRepository).markAs(eqTo(workItem.id), eqTo(ProcessingStatus.PermanentlyFailed), any)
    }

    "catch exception Mongo throw and error" in {
      val workItem = createWorkItem()
      setUpWithTwoWorkItem(workItem)
      when(workItemRepository.pullOutstanding(any, any)).thenReturn(
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
      setUpWithTwoWorkItem(workItem)

      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      await(job.executeInMutex)

      verify(workItemRepository, times(2)).markAs(
        eqTo(workItem.id),
        eqTo(ProcessingStatus.Failed),
        any
      )
    }

    "set the workItem state to PermanentlyFailed when failed three times" in {
      val retryAttempt = 2
      val workItem = createWorkItem(retryAttempt)
      setUpWithTwoWorkItem(workItem)
      when(appConfig.maxRetryAttempts).thenReturn(retryAttempt)
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      await(job.executeInMutex)

      verify(workItemRepository, times(2)).markAs(eqTo(workItem.id),eqTo(ProcessingStatus.PermanentlyFailed), any)
    }

    "parse the messages" in {
      setUpWithTwoWorkItem(createWorkItem())

      await(job.executeInMutex)

      verify(newMessageParserService).extractMessages(eqTo("any message"))
    }

    "send getNewMessage request for each pending ern" in {

      setUpWithTwoWorkItem(createWorkItem())

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-messages Job ran successfully."
      val captor = ArgCaptor[String]
      verify(newMessageService, times(2)).getNewMessagesAndAcknowledge(captor.capture)(any)

      captor.values mustBe Seq("123", "123")

      withClue("should save message to database") {
        verify(movementService).updateMovement(eqTo(message), eqTo("123"))
      }

    }

    "not process any message if no pending message exist" in {
      when(workItemRepository.pullOutstanding(any, any)).thenReturn(Future.successful(None))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-messages Job ran successfully."
      verifyZeroInteractions(newMessageService)
    }


    "not save message in the movement database" when {
      "API has no message" in {
        setUpWithTwoWorkItem(createWorkItem())
        when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
          .thenReturn(Future.successful(None))

        val result = await(job.executeInMutex)

        result.message mustBe "polling-new-messages Job ran successfully."
        verify(movementService, never()).updateMovement(any, any)
      }

      "API return an error" in {
        setUpWithTwoWorkItem(createWorkItem())
        when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(job.executeInMutex)

        result.message mustBe "polling-new-messages Job ran successfully."
        verify(movementService, never()).updateMovement(any, any)
      }
    }

  }

  private def setUpWithTwoWorkItem(workItem: WorkItem[ExciseNumberWorkItem]): Unit = {
    when(message.messageType).thenReturn(MessageTypes.IE802.value)
    when(workItemRepository.pullOutstanding(any, any)).thenReturn(
      Future.successful(Some(workItem)),
      Future.successful(Some(workItem)),
      Future.successful(None)
    )
    when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
      .thenReturn(
        Future.successful(Some(newMessageResponse)),
        Future.successful(None)
      )

    when(newMessageParserService.extractMessages(any)).thenReturn(Seq(message))
    when(movementService.updateMovement(any, any)).thenReturn(Future.successful(true))
    when(workItemRepository.complete(any, any)).thenReturn(Future.successful(true))
    when(workItemRepository.markAs(any, any, any)).thenReturn(Future.successful(true))
    when(workItemRepository.completeAndDelete(any)).thenReturn(Future.successful(true))
  }

  //TODO generalise all this stuff
  private def setUpWithOneWorkItem(workItem: WorkItem[ExciseNumberWorkItem]): Unit = {
    when(message.messageType).thenReturn(MessageTypes.IE802.value)
    when(workItemRepository.pullOutstanding(any, any)).thenReturn(
      Future.successful(Some(workItem)),
      Future.successful(None)
    )
    when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
      .thenReturn(
        Future.successful(None),
      )

    when(newMessageParserService.extractMessages(any)).thenReturn(Seq(message))
    when(movementService.updateMovement(any, any)).thenReturn(Future.successful(true))
    when(workItemRepository.complete(any, any)).thenReturn(Future.successful(true))
    when(workItemRepository.markAs(any, any, any)).thenReturn(Future.successful(true))
    when(workItemRepository.completeAndDelete(any)).thenReturn(Future.successful(true))
  }

  private def createWorkItem(failureCount: Int = 0, availableAt: Instant = Instant.now): WorkItem[ExciseNumberWorkItem] = {
    WorkItem(
      id = new ObjectId(),
      receivedAt = Instant.now,
      updatedAt = Instant.now,
      availableAt = availableAt,
      status = ProcessingStatus.ToDo,
      failureCount = failureCount,
      item = ExciseNumberWorkItem("123")
    )
  }
}
