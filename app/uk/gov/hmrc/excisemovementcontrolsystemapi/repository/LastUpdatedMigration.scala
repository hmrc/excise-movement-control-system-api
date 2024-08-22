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
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.{Aggregates, Field, Filters, UpdateOptions, Updates}
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.CollectionFactory

import java.time.{Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class LastUpdatedMigration @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends Logging {

  private lazy val collection: MongoCollection[JsObject] =
    CollectionFactory.collection(mongoComponent.database, "movements", implicitly, Seq.empty)

  def migrate(): Future[Done] = {

    logger.info("Starting movement lastUpdated migration")
    val start = Instant.now

    val result = collection
      .updateMany(
        Filters.and(Filters.exists("messages"), Filters.not(Filters.size("messages", 0))),
        Seq(
          Aggregates.set(
            Field(
              "lastUpdated",
              Json.obj(
                "$max" -> "$messages.createdOn"
              )
            )
          )
        )
      )
      .toFuture()

    result.onComplete {
      case Success(_) =>
        val end = Instant.now
        logger.info(s"Movement lastUpdated migration successful after ${Duration.between(start, end)}")
      case Failure(e) =>
        logger.error("Movement lastUpdated migration failed", e)
    }

    result.map(_ => Done)
  }

  migrate()
}
