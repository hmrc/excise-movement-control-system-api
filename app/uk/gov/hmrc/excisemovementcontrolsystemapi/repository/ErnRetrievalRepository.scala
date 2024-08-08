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

import com.mongodb.ReadConcern
import org.mongodb.scala.model._
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnRetrievalRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ErnRetrieval
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErnRetrievalRepository @Inject() (mongo: MongoComponent, appConfig: AppConfig, timeService: DateTimeService)(
  implicit ec: ExecutionContext
) extends PlayMongoRepository[ErnRetrieval](
      collectionName = "ernretrievals",
      mongoComponent = mongo,
      domainFormat = ErnRetrieval.format,
      indexes = mongoIndexes(appConfig.ernRetrievalTTL),
      replaceIndexes = false
    ) {

  def getErnsAndLastRetrieved: Future[Map[String, Instant]] = Mdc.preservingMdc {
    collection.find().toFuture().map(_.map(ernSubmission => ernSubmission.ern -> ernSubmission.lastRetrieved).toMap)
  }

  def getLastRetrieved(ern: String): Future[Option[Instant]] = Mdc.preservingMdc {
    collection
      .withReadConcern(ReadConcern.AVAILABLE)
      .find(Filters.eq("ern", ern))
      .headOption()
      .map(_.map(_.lastRetrieved))
  }

  def setLastRetrieved(ern: String, instant: Instant = timeService.timestamp()): Future[Option[Instant]] =
    Mdc.preservingMdc {
      collection
        .findOneAndReplace(
          Filters.eq("ern", ern),
          ErnRetrieval(ern, instant),
          FindOneAndReplaceOptions().upsert(true)
        )
        .headOption()
        .map(_.map(_.lastRetrieved))
    }
}

object ErnRetrievalRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("lastRetrieved"),
        IndexOptions()
          .name("lastRetrieved_ttl_index")
          .expireAfter(ttl.toSeconds, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.ascending("ern"),
        IndexOptions()
          .name("ern_index")
          .unique(true)
      )
    )
}
