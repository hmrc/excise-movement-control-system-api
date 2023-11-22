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
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.Inject
import scala.concurrent.Future
import scala.jdk.DurationConverters.ScalaDurationOps

@Singleton
class WorkItemService @Inject()
(
  workItemRepository: ExciseNumberQueueWorkItemRepository,
  appConfig: AppConfig,
  timestampService: TimestampSupport
) {

  def createWorkItem(ern: String): Future[WorkItem[ExciseNumberWorkItem]] = {
    //TODO need to check if one exists before pushing a new one.

    //TODO if exists, update available time to MIN(available time, now + fast interval)
    //TODO set retries to 3
    //TODO update lastSubmitted timestamp
    workItemRepository.pushNew(ExciseNumberWorkItem(ern), timestampService.timestamp().plus(appConfig.runSubmissionWorkItemAfter.toJava))
  }

}
