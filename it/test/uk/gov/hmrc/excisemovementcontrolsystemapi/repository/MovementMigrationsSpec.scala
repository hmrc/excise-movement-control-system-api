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

import org.bson.BsonType
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.{Aggregates, Field, Filters}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import java.util.UUID

class MovementMigrationsSpec
  extends AnyFreeSpec
    with Matchers
    with GuiceOneAppPerSuite
    with CleanMongoCollectionSupport
    with PlayMongoRepositorySupport[Movement]
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    ).build()

  override protected lazy val repository: MovementRepository = app.injector.instanceOf[MovementRepository]

  private lazy val collection: MongoCollection[JsObject] =
    CollectionFactory.collection(mongoComponent.database, collectionName, implicitly)
  private lazy val migration: MovementMigration = app.injector.instanceOf[MovementMigration]

  private val oldFormat: OFormat[Movement] = Json.format[Movement]
  private val timestamp = LocalDateTime.of(2024, 4, 3, 12, 30, 45, 123123).toInstant(ZoneOffset.UTC)
  private val movement = Movement(UUID.randomUUID().toString, Some("boxId"), "123", "345", Some("789"), None, timestamp, Seq.empty)
  private val newFormatMovement = Movement(UUID.randomUUID().toString, Some("boxId"), "124", "345", Some("789"), None, timestamp.truncatedTo(ChronoUnit.MILLIS), Seq.empty)

  "Must migrate old format movements to the new format" in {

    collection.insertOne(Json.toJsObject(movement)(oldFormat)).toFuture().futureValue
    collection.insertOne(Json.toJsObject(newFormatMovement)).toFuture().futureValue

    val oldFormatResult = collection.find(Filters.eq("_id", movement._id)).headOption.futureValue.value
    val oldFormatLastUpdated = (oldFormatResult \ "lastUpdated").as[Instant]
    oldFormatLastUpdated mustEqual movement.lastUpdated

    migration.migrate().futureValue

    val newFormatResult = collection.find(Filters.eq("_id", movement._id)).headOption().futureValue.value
    val newFormatLastUpdated = (newFormatResult \ "lastUpdated").as(MongoJavatimeFormats.instantReads)
    newFormatLastUpdated mustEqual movement.lastUpdated.truncatedTo(ChronoUnit.MILLIS)

    collection.find(Filters.eq("_id", newFormatMovement._id)).headOption().futureValue.value.as[Movement] mustEqual newFormatMovement
  }
}
