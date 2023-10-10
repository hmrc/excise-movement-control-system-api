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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal, in, or}
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessage
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


@Singleton
class MovementMessageRepository @Inject()
(
  mongo: MongoComponent,
  appConfig: AppConfig,
  clock: Clock
)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[MovementMessage](
    collectionName = "movements",
    mongoComponent = mongo,
    domainFormat = Json.format[MovementMessage],
    indexes = mongoIndexes(appConfig.getMovementTTL),
    replaceIndexes = true
  ) with Logging {

  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def filterBy(localReferenceNumber: String, consignorId: String, consigneeId: Option[String]): Bson = {
    val erns = Seq(consignorId) ++ consigneeId.fold[Seq[String]](Seq.empty)(o => Seq(o))

    and(
      equal("localReferenceNumber", localReferenceNumber),
      or(in("consignorId", erns: _*),
        in("consigneeId", erns: _*))
    )
  }

  private def filterBy(localReferenceNumber: String, erns: List[String]): Bson = {
    and(
      equal("localReferenceNumber", localReferenceNumber),
      or(in("consignorId", erns: _*),
        in("consigneeId", erns: _*))
    )
  }

  def keepAlive(id: String): Future[Boolean] =
    collection
      .updateOne(filter = byId(id), update = Updates.set("lastUpdated", Instant.now(clock)))
      .toFuture()
      .map(_ => true)


  def save(movementMessage: MovementMessage): Future[Boolean] = {
    val updatedMovement = movementMessage copy(lastUpdate = Instant.now(clock))

    collection.replaceOne(
      filter = filterBy(movementMessage.localReferenceNumber, movementMessage.consignorId, movementMessage.consigneeId),
      replacement = updatedMovement,
      options = ReplaceOptions().upsert(true)
    ).toFuture()
      .map(_ => true)
  }

  //todo: Check if this is right. Should the combinationof LRN and consigneeId or consignorId be unique?
  def getMovementMessagesByLRNAndERNIn(lrn: String, erns: List[String]): Future[Seq[MovementMessage]] = {
    collection.find(filterBy(lrn, erns)).toFuture()
  }

  def get(lrn: String, erns: List[String]): Future[Option[MovementMessage]] = {
    collection
      .find(filterBy(lrn, erns))
      .headOption()
  }

}

object MovementMessageRepository {
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
          Indexes.ascending("consignorId")
        ),
        IndexOptions().name("lrn_consignor_index")
          .background(true)
          .unique(true)
      )
    )
}
