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
import org.mongodb.scala.model._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ErnSubmission
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, Mdc}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErnSubmissionRepository @Inject() (
  mongo: MongoComponent,
  configuration: Configuration,
  timeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ErnSubmission](
      collectionName = "ernsubmissions",
      mongoComponent = mongo,
      domainFormat = ErnSubmission.format,
      indexes = mongoIndexes(configuration.get[Duration]("mongodb.ernSubmission.TTL")),
      replaceIndexes = true
    )
    with Logging {

  def getErnsAndLastSubmitted: Future[Map[String, Instant]] = Mdc.preservingMdc {
    collection.find().toFuture().map(_.map(ernSubmission => ernSubmission.ern -> ernSubmission.lastSubmitted).toMap)
  }

  def save(ern: String): Future[Done] = Mdc.preservingMdc {
    collection
      .replaceOne(
        Filters.eq("ern", ern),
        ErnSubmission(ern, timeService.timestamp()),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => Done)
  }

  def removeErns(erns: Seq[String]): Future[Done] = Mdc.preservingMdc {
    collection
      .deleteMany(Filters.in("ern", erns: _*))
      .toFuture()
      .map(_ => Done)
  }

  def findErns(erns: Seq[String]): Future[Seq[String]] = Mdc.preservingMdc {
    collection
      .find(Filters.in("ern", erns: _*))
      .toFuture()
      .map(erns => erns.map(e => e.ern))
  }

}

object ErnSubmissionRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("lastSubmitted"),
        IndexOptions()
          .name("lastSubmitted_ttl_index")
          .expireAfter(ttl.length, ttl.unit)
      ),
      IndexModel(
        Indexes.ascending("ern"),
        IndexOptions()
          .name("ern_index")
          .unique(true)
      )
    )
}
