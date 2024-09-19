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

import cats.implicits.toFunctorOps
import org.apache.pekko.Done
import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Movement, ProblemMovement, Total}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, Mdc}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava

@Singleton
class MovementArchiveRepository @Inject() (
  mongo: MongoComponent,
  appConfig: AppConfig,
  timeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Movement](
      collectionName = "movements-archive",
      mongoComponent = mongo,
      domainFormat = Movement.format,
      indexes = mongoIndexes(appConfig.movementArchiveTTL),
      replaceIndexes = false
    ) {

  def saveMovement(movement: Movement): Future[Boolean] = Mdc.preservingMdc {
    this.collection
      .insertOne(movement.copy(lastUpdated = timeService.timestamp()))
      .toFuture()
      .map(_ => true)
  }
}

object MovementArchiveRepository {
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
        IndexOptions()
          .name("lrn_consignor_idx")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("administrativeReferenceCode"),
        IndexOptions().name("arc_idx")
      ),
      IndexModel(
        Indexes.ascending("consignorId"),
        IndexOptions().name("consignorId_idx")
      ),
      IndexModel(
        Indexes.ascending("consigneeId"),
        IndexOptions().name("consigneeId_idx")
      ),
      IndexModel(
        Indexes.ascending("messages.boxesToNotify"),
        IndexOptions()
          .name("boxesToNotify_idx")
      ),
      IndexModel(
        Indexes.ascending("messages.recipient"),
        IndexOptions()
          .name("recipient_idx")
      )
    )
}
