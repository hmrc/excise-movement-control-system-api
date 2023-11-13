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
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes, ReplaceOptions}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementRepository @Inject()
(
  mongo: MongoComponent,
  appConfig: AppConfig,
  timeService: DateTimeService
)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[Movement](
    collectionName = "movements",
    mongoComponent = mongo,
    domainFormat = Json.format[Movement],
    indexes = mongoIndexes(appConfig.getMovementTTL),
    replaceIndexes = true
  ) with Logging {

  private def filterBy(localReferenceNumber: String, consignorId: String, consigneeId: Option[String]): Bson = {
    val erns = Seq(consignorId) ++ consigneeId.fold[Seq[String]](Seq.empty)(o => Seq(o))

    and(
      equal("localReferenceNumber", localReferenceNumber),
      or(in("consignorId", erns: _*),
        in("consigneeId", erns: _*))
    )
  }

  def saveMovement(movement: Movement): Future[Boolean] = {
    collection.insertOne(movement.copy(createdOn = timeService.now))
      .toFuture()
      .map(_ => true)
  }

  def updateMovement(movementMessage: Movement): Future[Boolean] = {
    val updatedMovement = movementMessage copy(createdOn = timeService.now)

    collection.replaceOne(
      filter = filterBy(movementMessage.localReferenceNumber, movementMessage.consignorId, movementMessage.consigneeId),
      replacement = updatedMovement,
      options = ReplaceOptions().upsert(true)
    ).toFuture()
      .map(_ => true)
  }
  def getMovementByLRNAndERNIn(lrn: String, erns: List[String]): Future[Seq[Movement]] = {
    //TODO case where returns more than one (e.g. consignee has the same LRN for two different consignors)
    // IN this case would this be the same movement? So we are ok to get the head?
    collection.find(and(equal("localReferenceNumber", lrn),
      or(in("consignorId", erns: _*), in("consigneeId", erns: _*)))).toFuture()
  }

  def getMovementByERN(ern: Seq[String]): Future[Seq[Movement]] = {
    collection
      .find(or(
        in("consignorId", ern: _*),
        in("consigneeId", ern: _*)
      ))
      .toFuture()
  }

  def getMovementByARC(arc: String): Future[Seq[Movement]] = {
    collection.find(in("administrativeReferenceCode", arc)).toFuture()
  }

  def getAllBy(consignorId: String): Future[Seq[Movement]] = {
    collection
      .find(and(equal("consignorId", consignorId)))
      .toFuture()
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
      ),
      IndexModel(
        Indexes.ascending("administrativeReferenceCode"),
        IndexOptions().name("arc_index")
          .background(true)
      )
    )
}
