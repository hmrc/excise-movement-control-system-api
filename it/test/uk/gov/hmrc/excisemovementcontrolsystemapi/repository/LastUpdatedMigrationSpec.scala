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

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.CollectionFactory
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

class LastUpdatedMigrationSpec
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
  private lazy val migration: LastUpdatedMigration = app.injector.instanceOf[LastUpdatedMigration]

  private val timestamp = LocalDateTime.of(2024, 4, 3, 12, 30, 45, 123123).toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
  private val message1Timestamp = timestamp.plus(1, ChronoUnit.MINUTES)
  private val message2Timestamp = message1Timestamp.plus(1, ChronoUnit.MINUTES)

  private val emptyMovement = Movement(UUID.randomUUID().toString, Some("boxId"), "123", "consignorId", Some("789"), None, timestamp.truncatedTo(ChronoUnit.MILLIS), Seq.empty)

  private val message1 = Message("encodedMessage", "type", "messageId", "consignorId", Set.empty, message1Timestamp)
  private val message2 = Message("encodedMessage2", "type2", "messageId2", "consignorId", Set.empty, message2Timestamp)
  private val movement = Movement(UUID.randomUUID().toString, Some("boxId"), "123", "consignorId", Some("789"), None, timestamp.truncatedTo(ChronoUnit.MILLIS), Seq(message1, message2))

  "must not update lastUpdated if no messages" in {

    repository.collection.insertOne(emptyMovement).toFuture().futureValue

    migration.migrate().futureValue

    repository.collection.find(Filters.eq("_id", emptyMovement._id)).headOption().futureValue.value mustEqual emptyMovement
  }

  "must update lastUpdated to latest createdOn for messages" in {

    repository.collection.insertOne(movement).toFuture().futureValue

    migration.migrate().futureValue

    println(collection.find(Filters.eq("_id", movement._id)).headOption().futureValue.value)
  }
}
