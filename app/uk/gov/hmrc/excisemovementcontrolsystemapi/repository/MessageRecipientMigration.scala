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

import org.apache.pekko.Done
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.{Aggregates, Field, Filters}
import play.api.Logging
import play.api.libs.json.{JsNull, JsObject, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.CollectionFactory

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MessageRecipientMigration @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends Logging {

  private lazy val collection: MongoCollection[JsObject] =
    CollectionFactory.collection(mongoComponent.database, "movements", implicitly, Seq.empty)

  def migrate(): Future[Done] = {

    logger.info("Starting message recipient migration")

    val result = collection
      .updateMany(
        Filters.exists("messages.$[].recipient", exists = false),
        Seq(
          Aggregates.set(
            Field(
              "messages",
              Json
                .obj(
                  "$map" -> Json.obj(
                    "input" -> "$messages",
                    "in"    -> Json.obj(
                      "$setField" -> Json.obj(
                        "field" -> "recipient",
                        "input" -> "$$this",
                        "value" -> Json.obj(
                          "$cond" -> Json.arr(
                            Json.obj("$gt" -> Json.arr("$$this.recipient", JsNull)),
                            "$$this.recipient",
                            "$$ROOT.consignorId"
                          )
                        )
                      )
                    )
                  )
                )
                .toDocument()
            )
          )
        )
      )
      .toFuture()

    result.onComplete {
      case Success(_) =>
        logger.info("migration successful")
      case Failure(e) =>
        logger.error("migration failed", e)
    }

    result.map(_ => Done)
  }

  migrate()
}
