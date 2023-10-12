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

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumber
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class ExciseNumberRepository @Inject()
(
  mongo: MongoComponent,
  appConfig: AppConfig,
  timeService: DateTimeService
)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[ExciseNumber](
    collectionName = "excise-number-list",
    mongoComponent = mongo,
    domainFormat = Json.format[ExciseNumber],
    indexes = mongoIndexes(appConfig.getExciseNumberListTTL),
    replaceIndexes = true
  ) with Logging
{
  def save(exciseNumber: ExciseNumber): Future[Boolean] = {
    val updatedExciseNumber = exciseNumber copy (lastUpdated = timeService.now)

    collection
      .replaceOne(filter = Filters.and(
        equal("exciseNumber", exciseNumber.exciseNumber),
        equal("localReferenceNumber", exciseNumber.localReferenceNumber)),
        replacement = updatedExciseNumber,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }


  def getAll: Source[ExciseNumber, NotUsed] = {

    Source.fromPublisher(
      collection.find().toObservable()
    )
      .map(o => o)
      .collect { case c => c }
  }

}

object ExciseNumberRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdated_ttl_idx")
          .expireAfter(ttl.toSeconds, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.compoundIndex(
          Indexes.ascending("localReferenceNumber"),
          Indexes.ascending("exciseNumber")
        ),
        IndexOptions().name("lrn_ern_index")
          .background(true)
          .unique(true)
      )
    )
}
