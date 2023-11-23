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

import dispatch.Future
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberQueueWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.WorkItemService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, ToDo}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant
import scala.compat.java8.DurationConverters.FiniteDurationops
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, HOURS, MINUTES, SECONDS}

class WorkItemServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockWorkItemRepo = mock[ExciseNumberQueueWorkItemRepository]
  private val timestampSupport = mock[TimestampSupport]
  private val appConfig = mock[AppConfig]
  private val timestamp = Instant.parse("2023-11-30T18:35:24.00Z")
  private val timestampPlusFastInterval = timestamp.plusSeconds(3 * 60)

  private val workItemService = new WorkItemService(mockWorkItemRepo, appConfig, timestampSupport)

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(timestampSupport.timestamp()).thenReturn(Instant.from(timestamp))
    when(appConfig.workItemFastInterval).thenReturn(Duration.create(3, MINUTES))
    when(appConfig.fastIntervalRetryAttempts).thenReturn(6)

    reset(mockWorkItemRepo)
  }


  "add work item for ern" should {
    val ern = "ern123"

    "create and return the work item when none for that ern already in the database" in {

      val expectedExciseNumberWorkItem = ExciseNumberWorkItem(ern, 6)
      val expectedWorkItem = createTestWorkItem(expectedExciseNumberWorkItem, timestampPlusFastInterval)

      when(mockWorkItemRepo.pushNew(any, any, any))
        .thenReturn(Future.successful(expectedWorkItem))

      when(mockWorkItemRepo.getWorkItemForErn(any)).thenReturn(Future.successful(Seq.empty))

      val result = await(workItemService.addWorkItemForErn(ern))

      withClue("should check the database for duplicates") {
        verify(mockWorkItemRepo).getWorkItemForErn(eqTo(ern))
      }

      withClue("should save to db") {
        verify(mockWorkItemRepo).pushNew(eqTo(expectedExciseNumberWorkItem), eqTo(timestampPlusFastInterval), any)
      }

      withClue("should return the Work Item for this ern") {
        result.item.exciseNumber mustBe ern
      }

      withClue("create it with fast retries set to the app config value") {
        result.item.fastPollRetriesLeft mustBe 6
      }

      withClue("set the availableAt time to be currentTime + fast interval") {
        result.availableAt mustBe timestampPlusFastInterval
      }

      withClue("set the receivedAt / lastSubmitted to now") {
        result.receivedAt mustBe timestamp
      }

    }

    "update work item when entry for that ern already in the database and it is next scheduled further away than fast interval" in {

      // Work Item has already been processed so fast interval retries < app config value
      // And Work Item is on slow interval so has a further away availableAt
      val workItemAlreadyInDb = createTestWorkItem(
        exciseNumberWorkItem = ExciseNumberWorkItem(ern, 2),
        availableAt = timestamp.plus(Duration(1, HOURS).toJava),
        receivedAt = timestamp.minusSeconds(10),
        updatedAt = timestamp.minusSeconds(10),
        status = Failed,
        failureCount = 132
      )

      val expectedWorkItemAfterUpdate = workItemAlreadyInDb.copy(
        item = ExciseNumberWorkItem(ern, 6),
        availableAt = timestampPlusFastInterval,
        receivedAt = timestamp,
        status = ToDo,
        failureCount = 0
      )

      when(mockWorkItemRepo.getWorkItemForErn(any))
        .thenReturn(
          Future.successful(Seq(workItemAlreadyInDb)),
          Future.successful(Seq(expectedWorkItemAfterUpdate))
        )

      when(mockWorkItemRepo.saveUpdatedWorkItem(any)).thenReturn(Future.successful(true))

      val result = await(workItemService.addWorkItemForErn(ern))

      withClue("should check the database for duplicates") {
        verify(mockWorkItemRepo, times(2)).getWorkItemForErn(eqTo(ern))
      }

      withClue("should update the database with the right details") {
        // The details being that
        // * Fast retries reset to app config value
        // * availableAt is currentTime + fastInterval
        // * receivedAt/lastSubmitted is currentTime
        // * status is To Do
        // * failureCount is reset to 0
        verify(mockWorkItemRepo).saveUpdatedWorkItem(eqTo(expectedWorkItemAfterUpdate))
      }

      withClue("should return the work item that was saved into the db") {
        result mustBe expectedWorkItemAfterUpdate
      }

    }

    "update work item when entry for that ern already in the database and is next scheduled closer than fast interval" in {
      val thirtySecondsLater = timestamp.plus(Duration(30, SECONDS).toJava)

      // Work Item already fast interval so will run soon. So we don't want to update availableAt
      val workItemAlreadyInDb = createTestWorkItem(ExciseNumberWorkItem(ern, 6), thirtySecondsLater)
      val expectedWorkItem = workItemAlreadyInDb

      when(mockWorkItemRepo.getWorkItemForErn(any))
        .thenReturn(
          Future.successful(Seq(workItemAlreadyInDb)),
          Future.successful(Seq(expectedWorkItem))
        )

      when(mockWorkItemRepo.saveUpdatedWorkItem(any)).thenReturn(Future.successful(true))

      val result = await(workItemService.addWorkItemForErn(ern))

      withClue("set the availableAt time to be currentTime + fast interval") {
        result.availableAt mustBe thirtySecondsLater
      }

    }

  }

  private def createTestWorkItem(
                                  exciseNumberWorkItem: ExciseNumberWorkItem,
                                  availableAt: Instant,
                                  status: ProcessingStatus = ToDo,
                                  receivedAt: Instant = timestamp,
                                  updatedAt: Instant = timestamp,
                                  failureCount: Int = 0
                                ) = {
    WorkItem(
      id = new ObjectId(),
      receivedAt = receivedAt,
      updatedAt = updatedAt,
      availableAt = availableAt,
      status = status,
      failureCount = failureCount,
      item = exciseNumberWorkItem
    )
  }
}

