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

import cats.data.EitherT
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, times, verify, verifyZeroInteractions, when}
import org.mockito.VerifyInOrder
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.BAD_REQUEST
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageReceiptSuccessResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE801Message, IE813Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{FailedPushNotification, SuccessPushNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumberWorkItem, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, TestUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
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

  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val appConfig = mock[AppConfig]
  private val newMessageService = mock[GetNewMessageService]
  private val movementService = mock[MovementService]
  private val newMessageParserService = mock[NewMessageParserService]
  private val lockRepository = mock[MongoLockRepository]
  private val workItemService = mock[WorkItemService]
  private val dateTimeService = mock[DateTimeService]
  private val message = mock[IEMessage]
  private val notificationService = mock[PushNotificationService]
  private val timestamp = Instant.now
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
  private val auditService = mock[AuditService]

  private val job = new PollingNewMessagesWithWorkItemJob(
    lockRepository,
    newMessageService,
    workItemService,
    movementService,
    newMessageParserService,
    notificationService,
    auditService,
    appConfig,
    dateTimeService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      movementService,
      appConfig,
      newMessageService,
      newMessageParserService,
      lockRepository,
      workItemService,
      notificationService,
      auditService
    )

    when(lockRepository.takeLock(any, any, any)).thenReturn(Future.successful(Some(Lock("id", "owner", Instant.now, Instant.now))))
    when(lockRepository.releaseLock(any, any)).thenReturn(successful(()))
    when(dateTimeService.timestamp()).thenReturn(timestamp)

    when(appConfig.maxFailureRetryAttempts).thenReturn(3)
    when(appConfig.workItemFastInterval).thenReturn(Duration.create(5, MINUTES))
    when(appConfig.workItemSlowInterval).thenReturn(Duration.create(1, HOURS))
    when(appConfig.failureRetryAfter).thenReturn(Duration.create(5, MINUTES))
    when(appConfig.pushNotificationsEnabled).thenReturn(true)

    when(newMessageParserService.extractMessages(any)).thenReturn(Seq(message))
    when(movementService.updateMovement(any, any))
      .thenReturn(Future.successful(Seq(Movement(Some("boxId"), "1", "2", Some("3"), Some("4")))))
    when(workItemService.markAs(any, any, any)).thenReturn(Future.successful(true))
    when(workItemService.rescheduleWorkItem(any)).thenReturn(Future.successful(true))
    when(notificationService.sendNotification(any, any, any, any)(any))
      .thenReturn(Future.successful(SuccessPushNotificationResponse("notificationId)")))

    when(auditService.auditMessage(any)(any)).thenReturn(EitherT.fromEither(Right(())))
    when(auditService.auditMessage(any, any)(any)).thenReturn(EitherT.fromEither(Right(())))

    when(newMessageService.acknowledgeMessage(any)(any))
      .thenReturn(successful(MessageReceiptSuccessResponse(timestamp, "", 0)))
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
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 5))))

      await(job.execute)

      verify(lockRepository).takeLock(eqTo("PollingNewMessageWithWorkItem"), any, any)

      withClue("release the mongo lock") {
        verify(lockRepository).releaseLock(eqTo("PollingNewMessageWithWorkItem"), any)
      }
    }

    "should reschedule workItem when successfully run" in {
      val workItem = createWorkItem()
      addOneItemToMockQueue(workItem)
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 10))))

      await(job.execute)

      verify(workItemService).rescheduleWorkItem(eqTo(workItem))
    }

    "should immediately add workItem back to queue when successfully run and remaining messages" in {
      val workItem = createWorkItem()
      addOneItemToMockQueue(workItem)
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 17))))

      await(job.execute)

      verify(workItemService).markAs(eqTo(workItem.id), eqTo(ToDo), eqTo(Some(workItem.availableAt)))
    }

    "catch any exception Mongo throws and error" in {
      val workItem = createWorkItem()
      addOneItemToMockQueue(workItem)
      when(workItemService.pullOutstanding(any, any)).thenReturn(
        Future.failed(new RuntimeException("error"))
      )

      val result = await(job.execute)

      result.message mustBe
        """The execution of scheduled job polling-new-messages failed with error 'error'.
          |The next execution of the job will do retry."""
          .stripMargin
          .replace('\n', ' ')
    }

    "retry failing EIS and mark a workItem as Failed" in {
      val workItem = createWorkItem()
      addOneItemToMockQueue(workItem)

      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      await(job.execute)

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
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(job.execute)

      verify(workItemService).rescheduleWorkItemForceSlow(eqTo(workItem))
      result.message mustBe "polling-new-messages Job ran successfully."
    }

    "parse the messages" in {
      addOneItemToMockQueue()
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 5))))

      await(job.execute)

      verify(newMessageParserService).extractMessages(eqTo("any message"))
    }

    "send getNewMessage request for each pending ern if there are multiple" in {
      addTwoItemsToMockQueue(createWorkItem(), createWorkItem(ern = "124"))
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 5))))

      val result = await(job.execute)

      result.message mustBe "polling-new-messages Job ran successfully."
      val captor = ArgCaptor[String]
      verify(newMessageService, times(2)).getNewMessages(captor.capture)(any)

      captor.values mustBe Seq("123", "124")

      withClue("should save message to database") {
        verify(movementService).updateMovement(eqTo(message), eqTo("123"))
        verify(movementService).updateMovement(eqTo(message), eqTo("124"))
      }

    }

    "not process any Work Items if no Work Items exist" in {
      when(workItemService.pullOutstanding(any, any)).thenReturn(Future.successful(None))

      val result = await(job.execute)

      result.message mustBe "polling-new-messages Job ran successfully."
      verifyZeroInteractions(newMessageService)
    }

    "not save any messages in the movement database" when {
      "API response has no messages inside" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponseEmpty, 0))))
        when(newMessageParserService.extractMessages(any)).thenReturn(Seq.empty)

        val result = await(job.execute)

        result.message mustBe "polling-new-messages Job ran successfully."
        verifyZeroInteractions(movementService)
        verifyZeroInteractions(notificationService)
      }

      "API returns no message" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(None))

        val result = await(job.execute)

        result.message mustBe "polling-new-messages Job ran successfully."
        verifyZeroInteractions(movementService)
        verifyZeroInteractions(notificationService)
      }

      "API returns an error" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(job.execute)

        result.message mustBe "polling-new-messages Job ran successfully."
        verify(movementService, never()).updateMovement(any, any)
        verifyZeroInteractions(notificationService)
      }
    }

    "save each message in sequential order" in {
      addOneItemToMockQueue(createWorkItem())
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 3))))

      val ie815Message = mock[IE801Message]
      val ie813Message = mock[IE813Message]
      when(newMessageParserService.extractMessages(any))
        .thenReturn(Seq(message, ie815Message, ie813Message))

      await(job.execute)

      val inOrder = VerifyInOrder(Seq(movementService))

      inOrder.verify(movementService).updateMovement(eqTo(message), eqTo("123"))
      inOrder.verify(movementService).updateMovement(eqTo(ie815Message), eqTo("123"))
      inOrder.verify(movementService).updateMovement(eqTo(ie813Message), eqTo("123"))
    }

    "audit each message" in {
      addOneItemToMockQueue(createWorkItem())
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 3))))

      val ie815Message = mock[IE801Message]
      val ie813Message = mock[IE813Message]
      when(newMessageParserService.extractMessages(any))
        .thenReturn(Seq(message, ie815Message, ie813Message))

      await(job.execute)

      verify(auditService).auditMessage(eqTo(message))(any)
      verify(auditService).auditMessage(eqTo(ie815Message))(any)
      verify(auditService).auditMessage(eqTo(ie813Message))(any)
    }

    "send a push notification" when {

      "feature flag enabled" in {
        addOneItemToMockQueue(createWorkItem())

      when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 3))))

        val ie801Message = mock[IE801Message]
        when(ie801Message.messageIdentifier).thenReturn("1")
        when(ie801Message.messageType).thenReturn("IE801")
        val ie813Message = mock[IE813Message]
        when(ie813Message.messageIdentifier).thenReturn("2")
        when(ie813Message.messageType).thenReturn("IE813")
        when(message.messageIdentifier).thenReturn("3")
        when(message.messageType).thenReturn("IE704")
        when(newMessageParserService.extractMessages(any))
          .thenReturn(Seq(message, ie801Message, ie813Message))

        when(movementService.updateMovement(any, any))
          .thenReturn(Future.successful(Seq(
            Movement(Some("id1"), "boxId1", "consignor", Some("consignee")),
            Movement(Some("id2"), "boxId1", "consignor", Some("consignee"))
          )))

        await(job.execute)

      verify(notificationService, times(2)).sendNotification(eqTo("123"), any[Movement], eqTo("1"), eqTo("IE801"))(any)
      verify(notificationService, times(2)).sendNotification(eqTo("123"), any[Movement], eqTo("2"), eqTo("IE813"))(any)
      verify(notificationService, times(2)).sendNotification(eqTo("123"), any[Movement], eqTo("3"), eqTo("IE704"))(any)
      }

    }

    "not send a push notification" when {

      "feature flag is not enabled" in {

        when(appConfig.pushNotificationsEnabled).thenReturn(false)

        addOneItemToMockQueue(createWorkItem())

        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 1))))

        when(message.messageIdentifier).thenReturn("3")
        when(newMessageParserService.extractMessages(any))
          .thenReturn(Seq(message))

        when(movementService.updateMovement(any, any))
          .thenReturn(Future.successful(Seq(Movement(Some("id1"), "boxId1", "consignor", Some("consignee")))))

        await(job.execute)

        verifyZeroInteractions(notificationService)

      }

    }

    "not send a push notification" when {
      "a message could not be saved in the database" in {
        addOneItemToMockQueue(createWorkItem())

        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 3))))

        when(newMessageParserService.extractMessages(any))
          .thenReturn(Seq(message))

        when(movementService.updateMovement(any, any))
          .thenReturn(Future.successful(Seq.empty))

        await(job.execute)

        verifyZeroInteractions(notificationService)
      }

    }

    "acknowledge the messages when all messages are saved to db" in {
      addOneItemToMockQueue()
      when(newMessageService.getNewMessages(any)(any))
        .thenReturn(Future.successful(Some((newMessageResponse, 5))))

      val movement = Movement(Some("bixId"), "lrn", "consigneeId", None, None, timestamp)
      when(movementService.updateMovement(any, any))
        .thenReturn(
          Future.successful(Seq(movement)))

      await(job.execute)

      val order1 = VerifyInOrder(Seq(movementService, newMessageService))

      order1.verify(movementService).updateMovement(any, any)
      order1.verify(newMessageService).acknowledgeMessage(any)(any)

    }

    "not acknowledge the messages" when {

      "none of the messages have been saved in the db" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 5))))

        when(movementService.updateMovement(any, any))
          .thenReturn(Future.successful(Seq.empty))

        await(job.execute)

        verify(newMessageService, never()).acknowledgeMessage(any)(any)
        verify(notificationService, never()).sendNotification(any, any, any, any)(any)
      }

      "any individual message is not saved to the database" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 5))))

        when(newMessageParserService.extractMessages(any)).thenReturn(Seq(message, message, message, message, message))
        when(movementService.updateMovement(any, any))
          .thenReturn(
            Future.successful(Seq(Movement(Some("boxId1"), "lrn1", "consignor", Some("consignee")))),
            Future.successful(Seq(Movement(Some("boxId2"), "lrn2", "consignor", Some("consignee")))),
            Future.successful(Seq.empty),
            Future.successful(Seq(Movement(Some("boxId3"), "lrn3", "consignor", Some("consignee")))),
            Future.successful(Seq(Movement(Some("boxId4"), "lrn4", "consignor", Some("consignee")))),
          )

        await(job.execute)

        verify(newMessageService, never()).acknowledgeMessage(any)(any)
        verify(notificationService, times(4)).sendNotification(any, any, any, any)(any)

        withClue("should audit successful messages") {
          verify(auditService, times(4)).auditMessage(eqTo(message))(any)
        }

        withClue("should audit failing message") {
          verify(auditService, times(1)).auditMessage(eqTo(message), eqTo("Failed to process"))(any)
        }

      }

      "update movement throws" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 5))))

        when(movementService.updateMovement(any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        await(job.execute)

        verify(newMessageService, never()).acknowledgeMessage(any)(any)
        verify(notificationService, never()).sendNotification(any, any, any, any)(any)
      }

      "there are no messages" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 5))))

        when(newMessageParserService.extractMessages(any))
          .thenReturn(Seq.empty)

        await(job.execute)

        verify(newMessageService, never()).acknowledgeMessage(any)(any)
        verify(notificationService, never()).sendNotification(any, any, any, any)(any)
      }

    }

    "acknowledge receipt" when {

      "the push notification could not be sent for all the messages" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 5))))

        when(movementService.updateMovement(any, any))
          .thenReturn(Future.successful(Seq(Movement(Some("boxId1"), "lrn1", "consignor", Some("consignee")))))

        when(notificationService.sendNotification(any, any, any, any)(any))
          .thenReturn(Future.successful(FailedPushNotification(BAD_REQUEST, "something went wrong :(")))

        await(job.execute)

        verify(newMessageService).acknowledgeMessage(any)(any)
      }

      "any individual push notification could not be sent" in {
        addOneItemToMockQueue()
        when(newMessageService.getNewMessages(any)(any))
          .thenReturn(Future.successful(Some((newMessageResponse, 5))))

        when(newMessageParserService.extractMessages(any)).thenReturn(Seq(message, message, message, message, message))

        when(movementService.updateMovement(any, any))
          .thenReturn(Future.successful(Seq(Movement(Some("boxId1"), "lrn1", "consignor", Some("consignee")))))

        when(notificationService.sendNotification(any, any, any, any)(any))
          .thenReturn(
            Future.successful(SuccessPushNotificationResponse("notificationId")),
            Future.successful(SuccessPushNotificationResponse("notificationId")),
            Future.successful(FailedPushNotification(BAD_REQUEST, "something went wrong :(")),
            Future.successful(SuccessPushNotificationResponse("notificationId")),
            Future.successful(SuccessPushNotificationResponse("notificationId"))
          )
        await(job.execute)

        verify(newMessageService).acknowledgeMessage(any)(any)
      }
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
