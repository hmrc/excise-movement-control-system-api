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

import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnusedRepositoryRemover @Inject() (
  mongo: MongoComponent
)(implicit ec: ExecutionContext)
    extends Logging {
  val firstCollectionName  = "problem-movements-workItems"

  removeMiscodedMovements()

  def removeMiscodedMovements(): Future[Unit] =
    for {
      _ <- logCollectionExistence(mongo, firstCollectionName)
      _ <- mongo.database.getCollection(firstCollectionName).drop().toFuture()
      _  = logger.warn("Problem movements repositories dropped")
      _ <- logCollectionExistence(mongo, firstCollectionName)
    } yield ()
  private def logCollectionExistence(
    mongoComponent: MongoComponent,
    collectionName: String
  ): Future[Unit]                             =
    for {
      collections <- mongoComponent.database.listCollectionNames().toFuture()
    } yield {
      val containsCollection = collections.contains(collectionName)
      logger.info(s"$collectionName exists in mongo: $containsCollection")
      ()
    }

}
