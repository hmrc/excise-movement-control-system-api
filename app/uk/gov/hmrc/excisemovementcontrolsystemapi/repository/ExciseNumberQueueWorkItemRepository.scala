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

package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.mongo.workitem.{WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ExciseNumberQueueWorkItemRepository @Inject()
(
  appConfig: AppConfig,
  mongoComponent: MongoComponent,
  timeService: TimestampSupport
)(implicit ec: ExecutionContext) extends WorkItemRepository[ExciseNumberWorkItem](
  collectionName = "excise-number-work-item",
  mongoComponent = mongoComponent,
  itemFormat = ExciseNumberWorkItem.format,
  workItemFields = WorkItemFields.default.copy(availableAt = "availableAt",receivedAt = "lastSubmitted"),
  extraIndexes = Seq(
    IndexModel(
      Indexes.ascending("lastSubmitted"),
      IndexOptions().expireAfter(appConfig.getMovementTTL.toSeconds, TimeUnit.SECONDS)
    )
  )
) {

  override def now(): Instant = timeService.timestamp()

  override val inProgressRetryAfter: Duration = {
    appConfig.retryAfterMinutes
  }
}

