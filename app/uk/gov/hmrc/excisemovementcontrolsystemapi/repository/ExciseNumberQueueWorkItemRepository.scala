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

import org.mongodb.scala.model.Filters.{equal, in}
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem, WorkItemFields, WorkItemRepository}

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.DurationConverters.ScalaDurationOps

class ExciseNumberQueueWorkItemRepository @Inject()
(
  appConfig: AppConfig,
  mongoComponent: MongoComponent,
  timeService: DateTimeService
)(implicit ec: ExecutionContext) extends WorkItemRepository[ExciseNumberWorkItem](
  collectionName = "excise-number-work-item",
  mongoComponent = mongoComponent,
  itemFormat = ExciseNumberWorkItem.format,
  workItemFields = WorkItemFields.default.copy(availableAt = "availableAt", receivedAt = "lastSubmitted"),
  extraIndexes = Seq(
    IndexModel(
      Indexes.ascending("lastSubmitted"),
      IndexOptions().expireAfter(appConfig.workItemTTL.toSeconds, TimeUnit.SECONDS)
    ),
    IndexModel(
      Indexes.ascending("item.exciseNumber"),
      IndexOptions()
        .name("item_ern_index")
        .unique(true)
    )
  )
) {

  override def now(): Instant = timeService.timestamp()

  override val inProgressRetryAfter: Duration = {
    appConfig.workItemInProgressTimeOut.toJava
  }

  def getWorkItemForErn(ern: String): Future[Option[WorkItem[ExciseNumberWorkItem]]] = {

    collection
      .find(
        in("item.exciseNumber", ern),
      )
      .toFuture().map(x => x.headOption)
  }

  def saveUpdatedWorkItem(updatedWI: WorkItem[ExciseNumberWorkItem]): Future[Boolean] = {
    val update = combine(
      set("item", Codecs.toBson(updatedWI.item)),
      set("availableAt", updatedWI.availableAt),
      set("lastSubmitted", updatedWI.receivedAt),
      set("status", ProcessingStatus.toBson(updatedWI.status)),
      set("failureCount", updatedWI.failureCount),
      set("updatedAt", timeService.timestamp())
    )

    collection.updateOne(filter = equal("_id", updatedWI.id), update).toFuture().map(_ => true)
  }
}

