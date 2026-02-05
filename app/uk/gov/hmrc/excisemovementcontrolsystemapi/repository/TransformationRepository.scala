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
import play.api.{Configuration, Logging}
import play.api.libs.Files.logger
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model._
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
import scala.util.control.NonFatal

@Singleton
class TransformationRepository @Inject() (
  mongo: MongoComponent,
  appConfig: AppConfig,
  timeService: DateTimeService,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Movement](
      collectionName = "movements_v2",
      mongoComponent = mongo,
      domainFormat = Movement.format,
      indexes = mongoIndexes(appConfig.movementTTL),
      extraCodecs = Seq(
        Codecs.playFormatCodec(ErnAndLastReceived.format),
        Codecs.playFormatCodec(MessageNotification.format)
      ),
      replaceIndexes = true
    ) {

  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def byLrnAndErns(localReferenceNumber: String, erns: List[String]): Bson =
    and(
      equal("localReferenceNumber", localReferenceNumber),
      or(in("consignorId", erns: _*), in("consigneeId", erns: _*))
    )

  def findDraftMovement(movement: Movement): Future[Option[Movement]] = Mdc.preservingMdc {
    val filters = Seq(
      Some(equal("consignorId", movement.consignorId)),
      Some(equal("localReferenceNumber", movement.localReferenceNumber)),
      Some(not(exists("administrativeReferenceCode")))
      // consigneeId is not included here
      // This should make it match the duplication checking logic in Core so we
      // don't have the problem where movements are saved successfully by Core
      // and then rejected as being duplicates by EMCS API - for more info see CS-6769
    )
    collection
      .find(
        filter = and(
          filters.flatten: _*
        )
      )
      .headOption()
  }

  def saveMovement(movement: Movement): Future[Done] = Mdc.preservingMdc {
    collection
      .findOneAndReplace(
        filter = byId(movement._id),
        replacement = movement.copy(lastUpdated = timeService.timestamp()),
        FindOneAndReplaceOptions().upsert(true)
      )
      .toFuture()
      .as(Done)
  }

  def saveMovements(movement: Seq[Movement]): Future[Boolean] = Mdc.preservingMdc {
    collection
      .insertMany(
        movement,
        InsertManyOptions().ordered(false)
      )
      .toFuture()
      .map(_ => true)

  }

  def getMovementById(id: String): Future[Option[Movement]] = Mdc.preservingMdc {
    collection.find(byId(id)).headOption()
  }

  def getMovementByLRNAndERNIn(lrn: String, erns: List[String]): Future[Seq[Movement]] = Mdc.preservingMdc {
    collection.find(byLrnAndErns(lrn, erns)).toFuture()
  }

  private def getFilteredMovementByERN(
    ern: String,
    localReferenceNumbers: Seq[String],
    administrativeReferenceCodes: Seq[String]
  ): Future[Seq[Movement]] = Mdc.preservingMdc {

    val ernFilters = or(getErnFilters(Seq(ern)): _*)

    val idFilter: Bson = Filters.or(
      Filters.in("localReferenceNumber", localReferenceNumbers: _*),
      Filters.in("administrativeReferenceCode", administrativeReferenceCodes: _*)
    )

    collection
      .find(
        and(
          idFilter,
          ernFilters
        )
      )
      .toFuture()
  }

  def getMovementByERN(
    ern: Seq[String],
    movementFilter: MovementFilter = MovementFilter.emptyFilter
  ): Future[Seq[Movement]] = Mdc.preservingMdc {

    val ernFilters = getErnFilters(ern)

    val filters =
      Seq(
        movementFilter.updatedSince.map(Filters.gte("lastUpdated", _)),
        movementFilter.lrn.map(Filters.eq("localReferenceNumber", _)),
        movementFilter.arc.map(Filters.eq("administrativeReferenceCode", _)),
        movementFilter.ern.map(ern =>
          Filters
            .or(Filters.eq("consignorId", ern), Filters.eq("consigneeId", ern), Filters.eq("messages.recipient", ern))
        )
      ).flatten

    val filter = if (filters.nonEmpty) Filters.and(filters: _*) else Filters.empty()

    collection
      .find(
        and(
          filter,
          or(
            ernFilters: _*
          )
        )
      )
      .toFuture()
  }

  private def getErnFilters(ern: Seq[String]) =
    Seq(
      Filters.in("consignorId", ern: _*),
      Filters.in("consigneeId", ern: _*),
      Filters.in("messages.recipient", ern: _*)
    )

  object MovementFilterThresholds {
    sealed trait Threshold

    case object Normal extends Threshold

    case object Filtered extends Threshold

    case object Failure extends Threshold
  }

  def protectionFilter(ern: String): Future[MovementFilterThresholds.Threshold] = {
    val movementFilter: MovementFilter = MovementFilter.emptyFilter
    val filters                        =
      Seq(
        movementFilter.updatedSince.map(Filters.gte("lastUpdated", _)),
        movementFilter.lrn.map(Filters.eq("localReferenceNumber", _)),
        movementFilter.arc.map(Filters.eq("administrativeReferenceCode", _)),
        movementFilter.ern.map(ern =>
          Filters
            .or(Filters.eq("consignorId", ern), Filters.eq("consigneeId", ern), Filters.eq("messages.recipient", ern))
        )
      ).flatten

    val movementThreshold        = configuration.get[Int]("movement.threshold")
    val movementFailureThreshold =
      configuration.getOptional[Int]("movement.failureThreshold").getOrElse(movementThreshold)

    val filter = if (filters.nonEmpty) Filters.and(filters: _*) else Filters.empty()

    val ernFilters = getErnFilters(Seq(ern))

    collection
      .countDocuments(
        and(
          filter,
          or(
            ernFilters: _*
          )
        )
      )
      .toFuture()
      .map { count =>
        if (count > movementThreshold && count <= movementFailureThreshold) {
          logger.warn(s"Protection filter filtering to occur for ERN: $ern - Count is $count")
          MovementFilterThresholds.Filtered
        } else if (count > movementFailureThreshold) {
          logger.warn(s"Protection filter responded with an error for ERN: $ern - Count is $count")
          MovementFilterThresholds.Failure
        } else {
          MovementFilterThresholds.Normal
        }
      }
  }

  def getAllBy(
    ern: String,
    localReferenceNumbers: Seq[String],
    administrativeReferenceCodes: Seq[String]
  ): Future[Seq[Movement]] = Mdc.preservingMdc {
    val protectFilter: Future[MovementFilterThresholds.Threshold] =
      if (appConfig.protectionFilterEnabled) {
        protectionFilter(ern)
      } else {
        Future.successful(MovementFilterThresholds.Filtered)
      }

    protectFilter.flatMap {
      case MovementFilterThresholds.Filtered =>
        getFilteredMovementByERN(ern, localReferenceNumbers, administrativeReferenceCodes)
      case MovementFilterThresholds.Normal   =>
        getMovementByERN(Seq(ern), MovementFilter.emptyFilter)
      case MovementFilterThresholds.Failure  =>
        throw new Exception(s"Protection filter responded with an error for ERN: $ern")
    }
  }

  def getByArc(arc: String): Future[Option[Movement]] = Mdc.preservingMdc {
    collection
      .find(
        filter = Filters.equal("administrativeReferenceCode", arc)
      )
      .headOption()
  }

  def getErnsAndLastReceived: Future[Map[String, Instant]] = Mdc.preservingMdc {
    collection
      .aggregate[ErnAndLastReceived](
        Seq(
          Aggregates.unwind("$messages"),
          Aggregates.group("$messages.recipient", Accumulators.max("lastReceived", "$messages.createdOn"))
        )
      )
      .toFuture()
      .map {
        _.map { field =>
          field._id -> field.lastReceived
        }.toMap
      }
  }

  def getPendingMessageNotifications: Future[Seq[MessageNotification]] = Mdc.preservingMdc {
    collection
      .aggregate[MessageNotification](
        Seq(
          // This match is to do an initial filter which filters out all movements that have no
          // messages which need to notify.
          // `Filters.gt("boxesToNotify", "")` is the best way I've found to do this filter which
          // also uses the index on the "boxesToNotify" field
          Aggregates.`match`(Filters.elemMatch("messages", Filters.gt("boxesToNotify", ""))),
          Aggregates.unwind("$messages"),
          Aggregates.unwind("$messages.boxesToNotify"),
          Aggregates.replaceRoot(
            Json
              .obj(
                "movementId"  -> "$_id",
                "messageId"   -> "$messages.messageId",
                "messageType" -> "$messages.messageType",
                "consignor"   -> "$consignorId",
                "consignee"   -> "$consigneeId",
                "arc"         -> "$administrativeReferenceCode",
                "recipient"   -> "$messages.recipient",
                "boxId"       -> "$messages.boxesToNotify"
              )
              .toDocument
          )
        )
      )
      .toFuture()
  }

  def confirmNotification(movementId: String, messageId: String, boxId: String): Future[Done] = Mdc.preservingMdc {
    collection
      .updateOne(
        Filters.eq("_id", movementId),
        Updates.pull("messages.$[m].boxesToNotify", boxId),
        UpdateOptions().arrayFilters(List(Filters.eq("m.messageId", messageId)).asJava)
      )
      .toFuture()
      .as(Done)
  }

  def addBoxIdToMessages(recipient: String, boxId: String): Future[Done] = Mdc.preservingMdc {
    collection
      .updateMany(
        Filters.eq("messages.recipient", recipient),
        Updates.addToSet("messages.$[m].boxesToNotify", boxId),
        UpdateOptions().arrayFilters(List(Filters.eq("m.recipient", recipient)).asJava)
      )
      .toFuture()
      .as(Done)
  }

  def removeBoxIdFromMessages(recipient: String, boxId: String): Future[Done] = Mdc.preservingMdc {
    collection
      .updateMany(
        Filters.eq("messages.recipient", recipient),
        Updates.pull("messages.$[m].boxesToNotify", boxId),
        UpdateOptions().arrayFilters(List(Filters.eq("m.recipient", recipient)).asJava)
      )
      .toFuture()
      .as(Done)
  }

}
