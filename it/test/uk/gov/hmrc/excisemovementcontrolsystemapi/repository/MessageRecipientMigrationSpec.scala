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
import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.CollectionFactory
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

class MessageRecipientMigrationSpec
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
  private lazy val migration: MessageRecipientMigration = app.injector.instanceOf[MessageRecipientMigration]

  private val timestamp = LocalDateTime.of(2024, 4, 3, 12, 30, 45, 123123).toInstant(ZoneOffset.UTC)

  private val message1 = Message("encodedMessage", "type", "messageId", "consignorId", timestamp)
  private val message2 = Message("encodedMessage2", "type2", "messageId2", "consignorId", timestamp)
  private val movement = Movement(UUID.randomUUID().toString, Some("boxId"), "123", "consignorId", Some("789"), None, timestamp.truncatedTo(ChronoUnit.MILLIS), Seq(message1, message2))

  private val message3 = Message("encodedMessage", "type", "messageId", "anotherRecipient", timestamp)
  private val movement2 = Movement(UUID.randomUUID().toString, Some("boxId"), "124", "consignorId", Some("789"), None, timestamp.truncatedTo(ChronoUnit.MILLIS), Seq(message3))

  "must add the recipient field to any messages which don't have one" in {

    import play.api.libs.json._

    val transformer = (__ \ "messages").json.update {
      Reads.of[JsArray].map { arr =>
        JsArray {
          arr.value.map { jsValue =>
            jsValue.transform((__ \ "recipient").json.prune).get
          }
        }
      }
    }

    val movementWithoutRecipient = Json.toJson(movement).transform(transformer).get

    collection.insertOne(movementWithoutRecipient).toFuture().futureValue
    repository.collection.insertOne(movement2).toFuture().futureValue

    migration.migrate().futureValue

    repository.collection.find(Filters.eq("_id", movement._id)).headOption().futureValue.value mustEqual movement
    repository.collection.find(Filters.eq("_id", movement2._id)).headOption().futureValue.value mustEqual movement2
  }
}
