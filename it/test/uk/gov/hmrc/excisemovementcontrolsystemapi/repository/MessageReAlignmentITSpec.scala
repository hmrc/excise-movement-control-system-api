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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement, ProblemMovement, Total}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}
import play.api.inject.bind
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MessageReAlignmentITSpec extends PlaySpec
                                  with CleanMongoCollectionSupport
                                  with PlayMongoRepositorySupport[Movement]
                                  with GuiceOneAppPerSuite
{
  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),

    )
    .build()

  override protected lazy val repository: MovementRepository = app.injector.instanceOf[MovementRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
  }


  "retrieval of movements with too many IE801 messages" in {
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()
    val timestamp = Instant.now.truncatedTo(ChronoUnit.MILLIS)

    val movement1 = Movement(uuid1.toString, Some("boxId"), "123", "345", Some("789"), None, timestamp, Seq.empty)

    val movement2 = Movement(uuid2.toString, Some("boxId"), "123", "6748", Some("789"), None, timestamp, Seq(
      Message("any, message", MessageTypes.IE801.value, "message01", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE801.value, "message02", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE818.value, "message03", "ern", Set.empty, timestamp)
    ))

    val movement3 = Movement(uuid3.toString, Some("boxId"), "123", "4359874", Some("789"), None, timestamp, Seq(
      Message("any, message", MessageTypes.IE801.value, "message11", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE801.value, "message12", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE801.value, "message13", "ern", Set.empty, timestamp)
    ))

    insert(movement1).futureValue
    insert(movement2).futureValue
    insert(movement3).futureValue

    val results = repository.getProblemMovements().futureValue
    results.size mustBe 1
    results.head  mustEqual ProblemMovement(uuid3.toString, 3)
  }

  "retrieving the count of movements with too many IE801 messages" in {
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()
    val timestamp = Instant.now.truncatedTo(ChronoUnit.MILLIS)

    val movement1 = Movement(uuid1.toString, Some("boxId"), "123", "345", Some("789"), None, timestamp, Seq.empty)

    val movement2 = Movement(uuid2.toString, Some("boxId"), "123", "6748", Some("789"), None, timestamp, Seq(
      Message("any, message", MessageTypes.IE801.value, "message01", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE801.value, "message02", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE818.value, "message03", "ern", Set.empty, timestamp)
    ))

    val movement3 = Movement(uuid3.toString, Some("boxId"), "123", "4359874", Some("789"), None, timestamp, Seq(
      Message("any, message", MessageTypes.IE801.value, "message11", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE801.value, "message12", "ern", Set.empty, timestamp),
      Message("any, message", MessageTypes.IE801.value, "message13", "ern", Set.empty, timestamp)
    ))

    insert(movement1).futureValue
    insert(movement2).futureValue
    insert(movement3).futureValue

    val results = repository.getCountOfProblemMovements().futureValue
    results mustBe Some(Total(1))
  }

  "retrieving the count of movements with too many IE801 messages, when there are none" in {
    val results = repository.getCountOfProblemMovements().futureValue
    results mustBe None
  }
}
