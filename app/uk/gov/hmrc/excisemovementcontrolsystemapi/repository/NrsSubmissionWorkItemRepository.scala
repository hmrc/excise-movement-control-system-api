/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NrsSubmissionWorkItemRepository.mongoIndexes

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import java.time.{Duration => javaDuration}

@Singleton
class NrsSubmissionWorkItemRepository @Inject() (
  appConfig: AppConfig,
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends WorkItemRepository[NrsSubmissionWorkItem](
      collectionName = "nrsSubmissionWorkItems",
      mongoComponent = mongoComponent,
      itemFormat = NrsSubmissionWorkItem.format,
      workItemFields = WorkItemFields.default,
      extraIndexes = mongoIndexes(appConfig.nrsWorkItemRepoTTL)
    ) {

  override def inProgressRetryAfter: javaDuration = appConfig.nrsRetryAfter

  override def now(): Instant = Instant.now()
}

object NrsSubmissionWorkItemRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdated_ttl_idx")
          .expireAfter(ttl.toSeconds, TimeUnit.SECONDS)
      )
    )
}
