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

import com.google.inject.Singleton
import org.bson.types.ObjectId
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberQueueWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.DurationConverters.ScalaDurationOps

@Singleton
class WorkItemService @Inject()
(
  workItemRepository: ExciseNumberQueueWorkItemRepository,
  appConfig: AppConfig,
  timestampService: DateTimeService
)(implicit val executionContext: ExecutionContext) extends Logging {

  def addWorkItemForErn(ern: String, fastMode: Boolean): Future[Boolean] = {

    workItemRepository.getWorkItemForErn(ern).flatMap {
      // Create a new one from a submission
      case None if fastMode => createWorkItem(ern, appConfig.fastIntervalRetryAttempts)

      // New one is coming from a GetMovement or GetMessages just let it slowly go off
      case None => createWorkItem(ern, 0)

      //New submission so turn us back to fast instead of slow
      case Some(workItem) if fastMode => updateWorkItemToRunOnFastIntervals(workItem)

      // GetMessages called for an existing Work Item. Nothing to do
      case Some(workItem) => Future.successful(true)
    }
      .recover {
        case ex: Throwable =>
          logger.error(s"[WorkItemService] - Failed to create Work Item for ERN $ern: ${ex.getMessage}")
          false
      }

  }

  def rescheduleWorkItem(workItem: WorkItem[ExciseNumberWorkItem]): Future[Boolean] = {

    val newFastPollRetriesLeft = Math.max(workItem.item.fastPollRetriesLeft - 1, 0)
    val updatedItem = workItem.item.copy(fastPollRetriesLeft = newFastPollRetriesLeft)
    val newAvailableAtTime = if (newFastPollRetriesLeft > 0) {
      workItem.availableAt.plus(appConfig.workItemFastInterval.toJava)
    } else {
      workItem.availableAt.plus(appConfig.workItemSlowInterval.toJava)
    }

    val updatedWorkItem = workItem.copy(
      item = updatedItem,
      availableAt = newAvailableAtTime,
      status = ToDo
    )

    workItemRepository.saveUpdatedWorkItem(updatedWorkItem)
  }

  def rescheduleWorkItemForceSlow(workItem: WorkItem[ExciseNumberWorkItem]): Future[Boolean] = {
    rescheduleWorkItem(workItem.copy(item = workItem.item.copy(fastPollRetriesLeft = 0)))
  }

  def markAs(id: ObjectId, status: ProcessingStatus, availableAt: Option[Instant] = None): Future[Boolean] =
    workItemRepository.markAs(id, status, availableAt)

  def pullOutstanding(failedBefore: Instant, availableBefore: Instant): Future[Option[WorkItem[ExciseNumberWorkItem]]] =
    workItemRepository.pullOutstanding(failedBefore, availableBefore)

  private def createWorkItem(ern: String, initialRetryAttempts: Int): Future[Boolean] = {

    val nextAvailableAt = timestampService.timestamp().plus(appConfig.workItemFastInterval.toJava)

    workItemRepository.pushNew(ExciseNumberWorkItem(ern, initialRetryAttempts), nextAvailableAt)
      .flatMap(_ => Future.successful(true))
  }

  private def updateWorkItemToRunOnFastIntervals(workItemToUpdate: WorkItem[ExciseNumberWorkItem]): Future[Boolean] = {

    val updatedItem = workItemToUpdate.item.copy(fastPollRetriesLeft = appConfig.fastIntervalRetryAttempts)

    val fastIntervalRunningTime = timestampService.timestamp().plus(appConfig.workItemFastInterval.toJava)

    val nextAvailableAt = if (fastIntervalRunningTime.isBefore(workItemToUpdate.availableAt)) {
      fastIntervalRunningTime
    } else {
      workItemToUpdate.availableAt
    }

    val updatedWorkItem = workItemToUpdate.copy(
      item = updatedItem,
      availableAt = nextAvailableAt,
      receivedAt = timestampService.timestamp(), //lastSubmitted
      status = ToDo,
      failureCount = 0
    )

    workItemRepository.saveUpdatedWorkItem(updatedWorkItem)

  }


}
