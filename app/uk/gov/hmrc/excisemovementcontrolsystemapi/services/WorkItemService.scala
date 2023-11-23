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
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberQueueWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.DurationConverters.ScalaDurationOps

@Singleton
class WorkItemService @Inject()
(
  workItemRepository: ExciseNumberQueueWorkItemRepository,
  appConfig: AppConfig,
  timestampService: TimestampSupport
)(implicit val executionContext: ExecutionContext) {

  def addWorkItemForErn(ern: String): Future[WorkItem[ExciseNumberWorkItem]] = {

    workItemRepository.getWorkItemForErn(ern).flatMap {
      case seq if seq == Seq.empty =>     createWorkItem(ern)
      case seq: Seq[WorkItem[ExciseNumberWorkItem]] => updateWorkItemToRunOnFastIntervals(seq.head)
    }

  }

  private def createWorkItem(ern: String): Future[WorkItem[ExciseNumberWorkItem]] = {

    val nextAvailableAt = timestampService.timestamp().plus(appConfig.workItemFastInterval.toJava)

    workItemRepository.pushNew(ExciseNumberWorkItem(ern, appConfig.fastIntervalRetryAttempts), nextAvailableAt)
  }

  private def updateWorkItemToRunOnFastIntervals(workItemToUpdate: WorkItem[ExciseNumberWorkItem]): Future[WorkItem[ExciseNumberWorkItem]] = {

    val itemToUpdate = workItemToUpdate.item
    val ern = itemToUpdate.exciseNumber
    val updatedItem = itemToUpdate.copy(fastPollRetriesLeft = appConfig.fastIntervalRetryAttempts)

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

    //TODO what happens if Work Item is locked in the db because the job is running at the same time as we get a submission???

    workItemRepository.saveUpdatedWorkItem(updatedWorkItem).flatMap(_ => workItemRepository.getWorkItemForErn(ern))
      .map { case seq if seq == Seq.empty => throw new RuntimeException("TODO")//TODO exception
      case seq: Seq[WorkItem[ExciseNumberWorkItem]] => seq.head //Only one entry expected
      }
  }

}
