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
import org.mongodb.scala.model.Filters.{and, equal, in, or}
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository.{ErnAndLastReceived, MessageNotification, mongoIndexes}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.play.http.logging.Mdc

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
        Codecs.playFormatCodec(MessageNotification.format)
      ),
      replaceIndexes = true
    )
    with Logging {

  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def byLrnAndErns(localReferenceNumber: String, erns: List[String]): Bson =
    and(
      equal("localReferenceNumber", localReferenceNumber),
      or(in("consignorId", erns: _*), in("consigneeId", erns: _*))
    )

  def saveMovement(movement: Movement): Future[Boolean] = Mdc.preservingMdc {
    collection
      .insertOne(movement.copy(lastUpdated = timeService.timestamp()))
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
    //TODO EMCS-527 -  case where returns more than one (e.g. consignee has the same LRN for two different consignors)
    // IN this case would this be the same movement? So we are ok to get the head?
    collection.find(byLrnAndErns(lrn, erns)).toFuture()
  }

  def getMovementByERN(
    ern: Seq[String],
    movementFilter: MovementFilter = MovementFilter.emptyFilter
  ): Future[Seq[Movement]] = Mdc.preservingMdc {

    val ernFilters = Seq(
      Filters.in("consignorId", ern: _*),
      Filters.in("consigneeId", ern: _*)
    )
    val filters    =
      Seq(
        movementFilter.updatedSince.map(Filters.gte("lastUpdated", _)),
        movementFilter.lrn.map(Filters.eq("localReferenceNumber", _)),
        movementFilter.arc.map(Filters.eq("administrativeReferenceCode", _)),
        movementFilter.traderType.map { traderType =>
          if (traderType.traderType.equalsIgnoreCase("consignor")) {
            Filters.in("consignorId", traderType.erns: _*)
          } else {
            Filters.in("consigneeId", traderType.erns: _*)
          }
        }
      ).flatten

    val filter = if (filters.nonEmpty) Filters.and(filters: _*) else Filters.empty

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

  def getAllBy(ern: String): Future[Seq[Movement]] = Mdc.preservingMdc {
    getMovementByERN(Seq(ern), MovementFilter.emptyFilter)
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
