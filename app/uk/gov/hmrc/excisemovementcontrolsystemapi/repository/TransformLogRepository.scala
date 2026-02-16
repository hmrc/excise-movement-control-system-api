/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mongodb.scala.model._
import org.mongodb.scala.model.Filters.equal
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.TransformLogRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Movement, TransformLog}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, Mdc}
import org.mongodb.scala.model.Filters._
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransformLogRepository @Inject() (
  mongo: MongoComponent,
  configuration: Configuration,
  timeService: DateTimeService,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[TransformLog](
      collectionName = "transform_log",
      mongoComponent = mongo,
      domainFormat = TransformLog.format,
      indexes = mongoIndexes(appConfig.transformLogTTL),
      replaceIndexes = true
    ) {

  def saveLog(log: Seq[TransformLog]): Future[Boolean] = Mdc.preservingMdc {
    collection
      .insertMany(
        log,
        InsertManyOptions().ordered(false)
      )
      .toFuture()
      .map(_ => true)
  }

  def saveLog(transformLog: TransformLog): Future[Boolean] = Mdc.preservingMdc {
    collection
      .replaceOne(
        Filters.eq("_id", transformLog._id),
        transformLog,
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  def findLog(movement: Movement) = Mdc.preservingMdc {
    val filter =
      and(equal("_id", movement._id), equal("isTransformSuccess", true), equal("messageCount", movement.messages.size))

    collection
      .find(filter)
      .first()
      .toFutureOption()
      .map {
        case Some(log) => true
        case None      => false
      }

  }

  def findLog(movement: String): Future[Option[TransformLog]] = Mdc.preservingMdc {
    val filter = equal("_id", movement)

    collection
      .find(filter)
      .first()
      .toFutureOption()

  }

}

object TransformLogRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdated_ttl_index")
          .expireAfter(ttl.toSeconds, TimeUnit.SECONDS)
      )
    )
}
