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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ClientBoxIdRepository.mongoIndexes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ClientBoxId
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientBoxIdRepository @Inject()(
  mongo: MongoComponent,
  configuration: Configuration,
  timeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ClientBoxId](
      collectionName = "clientboxids",
      mongoComponent = mongo,
      domainFormat = ClientBoxId.format,
      indexes = mongoIndexes(configuration.get[Duration]("mongodb.clientBoxId.TTL")),
      replaceIndexes = false
    ) {

  def getBoxId(clientId: String): Future[Option[String]] = Mdc.preservingMdc {
    collection.find(
      Filters.eq("clientId", clientId)
    )
      .map(_.boxId)
      .headOption()
  }

  def save(clientId: String, boxId: String): Future[Done] = Mdc.preservingMdc {
    collection
      .insertOne(
        ClientBoxId(clientId, boxId, timeService.timestamp())
      )
      .toFuture()
      .map(_ => Done)
  }
}

object ClientBoxIdRepository {
  def mongoIndexes(ttl: Duration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("createdOn"),
        IndexOptions()
          .name("createdOn_ttl_idx")
          .expireAfter(ttl.length, ttl.unit)
      ),
      IndexModel(
        Indexes.ascending("clientId"),
        IndexOptions()
          .name("clientId_idx")
          .unique(true)
      ),
    )
}
