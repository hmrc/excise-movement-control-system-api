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

import com.mongodb.ErrorCategory
import org.mongodb.scala.MongoWriteException
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.Mdc
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MiscodedMovementArchiveRepository @Inject() (
  mongo: MongoComponent,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Movement](
      collectionName = "miscoded-movements-archive",
      mongoComponent = mongo,
      domainFormat = Movement.format,
      indexes = mongoIndexes(appConfig.movementArchiveTTL),
      replaceIndexes = false
    )
    with Logging {

  def saveMovement(movement: Movement): Future[Boolean] = Mdc.preservingMdc {
    this.collection
      .insertOne(movement)
      .toFuture()
      .map { _ =>
        logger.warn(s"Archived movement ${movement._id}")
        true
      }
      .recover {
        case e: MongoWriteException if e.getError.getCategory == ErrorCategory.DUPLICATE_KEY =>
          logger.warn(s"Duplicate movement with id ${movement._id} already exists in the miscoded archive.")
          true
      }
  }
}
