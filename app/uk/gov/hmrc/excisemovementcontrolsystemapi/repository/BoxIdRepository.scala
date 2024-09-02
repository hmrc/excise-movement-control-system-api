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

import org.apache.pekko.Done
import org.mongodb.scala.model._
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.BoxIdRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.BoxIdRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BoxIdRepository @Inject() (mongo: MongoComponent, configuration: Configuration, timeService: DateTimeService)(
  implicit ec: ExecutionContext
) extends PlayMongoRepository[BoxIdRecord](
      collectionName = "boxids",
      mongoComponent = mongo,
      domainFormat = BoxIdRecord.format,
      indexes = mongoIndexes(configuration.get[Duration]("mongodb.boxId.TTL")),
      replaceIndexes = false
    ) {

  def getBoxIds(ern: String): Future[Set[String]] = Mdc.preservingMdc {
    collection.find(Filters.eq("ern", ern)).map(_.boxId).toFuture().map(_.toSet)
  }

  def save(ern: String, boxId: String): Future[Done] = Mdc.preservingMdc {
    collection
      .replaceOne(
        Filters.and(
          Filters.eq("ern", ern),
          Filters.eq("boxId", boxId)
        ),
        BoxIdRecord(ern, boxId, timeService.timestamp()),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => Done)
  }

  def delete(ern: String, boxId: String): Future[Done] = Mdc.preservingMdc {
    collection
      .deleteOne(
        Filters.and(
          Filters.eq("ern", ern),
          Filters.eq("boxId", boxId)
        )
      )
      .toFuture()
      .map(_ => Done)
  }
}

object BoxIdRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdated_ttl_index")
          .expireAfter(ttl.toSeconds, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.compoundIndex(
          Indexes.ascending("ern"),
          Indexes.ascending("boxId")
        ),
        IndexOptions()
          .name("ern_boxId_index")
          .unique(true)
      )
    )
}
