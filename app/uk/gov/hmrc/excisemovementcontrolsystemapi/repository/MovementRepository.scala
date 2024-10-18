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
import org.mongodb.scala.model.{InsertManyOptions, _}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{MiscodedMovement, Movement, ProblemMovement, Total}
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
class MovementRepository @Inject() (
  mongo: MongoComponent,
  appConfig: AppConfig,
  timeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[Movement](
      collectionName = "movements",
      mongoComponent = mongo,
      domainFormat = Movement.format,
      indexes = mongoIndexes(appConfig.movementTTL),
      extraCodecs = Seq(
        Codecs.playFormatCodec(ErnAndLastReceived.format),
        Codecs.playFormatCodec(MessageNotification.format),
        Codecs.playFormatCodec(ProblemMovement.format),
        Codecs.playFormatCodec(Total.format),
        Codecs.playFormatCodec(MiscodedMovement.format)
      ),
      replaceIndexes = false
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
      Some(not(exists("administrativeReferenceCode"))),
      movement.consigneeId.map(consignee => equal("consigneeId", consignee))
    )
    collection
      .find(
        filter = and(
          filters.flatten: _*
        )
      )
      .headOption()
  }

  def saveMovement(movement: Movement): Future[Boolean]       = Mdc.preservingMdc {
    collection
      .insertOne(movement.copy(lastUpdated = timeService.timestamp()))
      .toFuture()
      .map(_ => true)
  }
  def saveMovements(movement: Seq[Movement]): Future[Boolean] = Mdc.preservingMdc {
    collection
      .insertMany(
        movement.map(_.copy(lastUpdated = timeService.timestamp())),
        InsertManyOptions().ordered(false)
      )
      .toFuture()
      .map(_ => true)
  }

  def save(movement: Movement): Future[Done] = Mdc.preservingMdc {
    collection
      .findOneAndReplace(
        filter = byId(movement._id),
        replacement = movement,
        FindOneAndReplaceOptions().upsert(true)
      )
      .toFuture()
      .as(Done)
  }

  def updateMovement(movement: Movement): Future[Option[Movement]] = Mdc.preservingMdc {

    val updatedMovement = movement.copy(lastUpdated = timeService.timestamp())

    collection
      .findOneAndReplace(
        filter = byId(updatedMovement._id),
        replacement = updatedMovement,
        new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
      )
      .headOption()
  }

  def getMovementById(id: String): Future[Option[Movement]] = Mdc.preservingMdc {
    collection.find(byId(id)).headOption()
  }

  def getMovementByLRNAndERNIn(lrn: String, erns: List[String]): Future[Seq[Movement]] = Mdc.preservingMdc {
    collection.find(byLrnAndErns(lrn, erns)).toFuture()
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

  def getAllBy(ern: String): Future[Seq[Movement]] = Mdc.preservingMdc {
    getMovementByERN(Seq(ern), MovementFilter.emptyFilter)
  }

  def getByArc(arc: String): Future[Option[Movement]] = Mdc.preservingMdc {
    collection
      .find(
        filter = Filters.equal("administrativeReferenceCode", arc)
      )
      .headOption()
  }

  def migrateLastUpdated(ern: String): Future[Done] = Mdc.preservingMdc {
    val ernFilters = getErnFilters(Seq(ern))
    collection
      .updateMany(
        Filters.and(
          Filters.and(Filters.exists("messages"), Filters.not(Filters.size("messages", 0))),
          Filters.or(ernFilters: _*)
        ),
        Seq(
          Aggregates.set(
            Field(
              "lastUpdated",
              Json
                .obj(
                  "$max" -> "$messages.createdOn"
                )
                .toDocument()
            )
          )
        )
      )
      .toFuture()
      .as(Done)
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
              .toDocument()
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

  def getProblemMovements(): Future[Seq[ProblemMovement]] =
    collection
      .aggregate[ProblemMovement](
        Seq(
          Aggregates.project(Json.obj("messages.encodedMessage" -> 0).toDocument()),
          Aggregates.unwind("$messages"),
          Aggregates.`match`(Json.obj("messages.messageType" -> "IE801").toDocument()),
          Aggregates.group("$_id", Accumulators.sum("countOfIe801s", 1)),
          Aggregates.`match`(Filters.gt("countOfIe801s", 2))
        )
      )
      .toFuture()

  def getCountOfProblemMovements(): Future[Option[Total]] =
    collection
      .aggregate[Total](
        Seq(
          Aggregates.project(Json.obj("messages.encodedMessage" -> 0).toDocument()),
          Aggregates.unwind("$messages"),
          Aggregates.`match`(Json.obj("messages.messageType" -> "IE801").toDocument()),
          Aggregates.group("$_id", Accumulators.sum("countOfIe801s", 1)),
          Aggregates.`match`(Filters.gt("countOfIe801s", 2)),
          Aggregates.count("total")
        )
      )
      .headOption()

  def getMiscodedMovements(): Future[Seq[MiscodedMovement]] =
    collection
      .find[MiscodedMovement](
        Filters.and(
          Filters.exists("messages.recipient", exists = true),
          Filters.regex("messages.encodedMessage", "^PElFODAxVHlwZS")
        )
      )
      .projection(Projections.fields(Projections.include("_id")))
      .toFuture()

  def getCountOfMiscodedMovements(): Future[Long] =
    collection
      .countDocuments(
        Filters.and(
          Filters.exists("messages.recipient", exists = true),
          Filters.regex("messages.encodedMessage", "^PElFODAxVHlwZS")
        )
      )
      .toFuture()
}

object MovementRepository {
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

  final case class ErnAndLastReceived(_id: String, lastReceived: Instant)

  object ErnAndLastReceived extends MongoJavatimeFormats.Implicits {
    implicit lazy val format: OFormat[ErnAndLastReceived] = Json.format
  }

  final case class MessageNotification(
    movementId: String,
    messageId: String,
    messageType: String,
    consignor: String,
    consignee: Option[String],
    arc: Option[String],
    recipient: String,
    boxId: String
  )

  object MessageNotification {
    implicit lazy val format: OFormat[MessageNotification] = Json.format
  }
}
