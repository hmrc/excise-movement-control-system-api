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
import org.mockito.Mockito
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberQueueWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementService, NewMessageParserService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.workitem.WorkItemRepository

import java.time.LocalDateTime
import scala.concurrent.Future.successful
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

class PollingNewMessagesWithWorkItemJobSpec
  extends PlaySpec
    with BeforeAndAfterEach
    with NewMessagesXml {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val appConfig = mock[AppConfig]
  private val newMessageService = mock[GetNewMessageService]
  private val movementService = mock[MovementService]
  private val shoeNewMessageParser = mock[NewMessageParserService]
  private val lockRepository = mock[MongoLockRepository]
  private val workItemRepository = mock[ExciseNumberQueueWorkItemRepository]
  private val dateTimeService = mock[DateTimeService]
  private val newMessageResponse = EISConsumptionResponse(
    LocalDateTime.of(2023, 5, 6, 9,10,13),
    "123",
    "any message"
  )

  private val cachedMovements = Seq(
    Movement("2","1", None),
    Movement("3","3", None),
    Movement("4","4", None)
  )

  private val job = new PollingNewMessageWithWorkItemJob(
    lockRepository,
    newMessageService,
    workItemRepository,
    movementService,
    shoeNewMessageParser,
    appConfig,
    dateTimeService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, appConfig, newMessageService, shoeNewMessageParser, lockRepository)

    when(lockRepository.takeLock(any,any,any)).thenReturn(Future.successful(true))
    when(lockRepository.releaseLock(any,any)).thenReturn(successful(()))
  }

  "Job" should {

    "have interval defined" in {
      val interval = Duration.create(5, SECONDS)
      when(appConfig.interval).thenReturn(interval)
      job.interval mustBe interval
    }

    "have initial delay defined" in {
      val initialDelay = Duration.create(5, SECONDS)
      when(appConfig.initialDelay).thenReturn(initialDelay)
      job.initialDelay mustBe initialDelay
    }

    "acquire a mongo lock" in {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(Some(newMessageResponse)))
      await(job.executeInMutex)

      val captor = ArgCaptor[String]
      verify(lockRepository).takeLock(eqTo("PollingNewMessagesJob"), captor.capture, any)

      withClue("release the mongo lock") {
        lockRepository.releaseLock("PollingNewMessagesJob", captor.value)
      }
    }

    "parse the messages" in {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(Some(newMessageResponse)))
      when(shoeNewMessageParser.extractMessages(any)).thenReturn(Seq(mock[IEMessage]))

      await(job.executeInMutex)

      verify(shoeNewMessageParser, times(3)).extractMessages(eqTo("any message"))
    }

    "send getNewMessage request for each pending ern" in {
      val message1 = mock[IEMessage]
      val message2 = mock[IEMessage]

      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(Some(newMessageResponse)))
      when(shoeNewMessageParser.extractMessages(any))
        .thenReturn(Seq(message1, message2), Seq(message1), Seq(message2))
      when(movementService.updateMovement(any, any)).thenReturn(Future.successful(true))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."
      val captor = ArgCaptor[String]
      verify(newMessageService, times(3)).getNewMessagesAndAcknowledge(captor.capture)(any)

      captor.values mustBe Seq("1", "3", "4")

    }

//    "process IE801 and IE704 first " in {
//      val message1 = mock[IEMessage]
//      when(message1.getType).thenReturn(MessageTypes.IE810.value)
//      val message2 = mock[IEMessage]
//      when(message2.getType).thenReturn(MessageTypes.IE801.value)
//      val message3 = mock[IEMessage]
//      when(message3.getType).thenReturn(MessageTypes.IE704.value)
//
//
//      when(movementService.getUniqueConsignorId)
//        .thenReturn(Future.successful(Seq(Movement("12", "1", None))))
//      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
//        .thenReturn(Future.successful(Some(newMessageResponse)))
//
//      when(shoeNewMessageParser.extractMessages(any))
//        .thenReturn(Seq(message1, message2, message3))
//      when(movementService.updateMovement(any, any)).thenReturn(Future.successful(true))
//
//      await(job.executeInMutex)
//
//      val orderForIE818 = Mockito.inOrder(movementService)
//      orderForIE818.verify(movementService).updateMovement(eqTo(message2), eqTo("1"))
//      orderForIE818.verify(movementService).updateMovement(eqTo(message3), eqTo("1"))
//      orderForIE818.verify(movementService).updateMovement(eqTo(message1), eqTo("1"))
//    }

    "not process any message if no pending message exist" in {

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."
      verifyZeroInteractions(newMessageService)
    }


    "not change status in the database if show new message API has errors" in {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(None))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."

      verify(movementService, never()).saveMovementMessage(any)
    }

    "carry on polling if GetNewMessage Api fails" in {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any)).thenReturn(Future.successful(None))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."
      verify(movementService, never()).saveMovementMessage(any)
    }

    "return an error if there is an exception" in {

      val result = await(job.executeInMutex)

      result.message mustBe "The execution of scheduled job polling-new-message failed with error 'error'. The next execution of the job will do retry."
      verify(movementService, never()).saveMovementMessage(any)
    }
  }
}
