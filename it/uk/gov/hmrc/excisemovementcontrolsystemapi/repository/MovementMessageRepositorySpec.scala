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

import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessage
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class MovementMessageRepositorySpec extends PlaySpec
  with CleanMongoCollectionSupport
  with PlayMongoRepositorySupport[MovementMessage]
  with IntegrationPatience
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with OptionValues
  with GuiceOneAppPerSuite {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val appConfig = app.injector.instanceOf[AppConfig]
  private val instant = Instant.now
  private val stubClock = Clock.fixed(instant, ZoneId.systemDefault)

  protected override val repository = new MovementMessageRepository(
    mongoComponent,
    appConfig,
    stubClock
  )

  protected def appBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    dropDatabase()
  }

  "saveMovementMessage" should {
    "return insert a movement message" in {
      val result = repository.saveMovementMessage(MovementMessage("123", "345", Some("789"), None)).futureValue
      val insertedRecord = find(
        Filters.and(
          Filters.equal("consignorId", "345"),
          Filters.equal("localReferenceNumber", "123")
        )
      ).futureValue
        .headOption
        .value

      result mustEqual true
      insertedRecord.localReferenceNumber mustEqual "123"
      insertedRecord.consignorId mustEqual "345"
      insertedRecord.consigneeId mustEqual Some("789")
      insertedRecord.administrativeReferenceCode mustEqual None
    }
  }

  "getMovementMessagesByLRNAndERNIn" should {
    val lrn = "123"
    val consignorId = "Abc"
    val consigneeId = "def"


    "return movement message with valid lrn and consignorId combination" in {
      saveMovementMessage(lrn, consignorId, consigneeId)

      val result = repository.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)).futureValue

      result.size mustBe 1
    }

    "return movement message with valid lrn and consigneeId combination" in {
      saveMovementMessage(lrn, consignorId, consigneeId)
      val result = repository.getMovementMessagesByLRNAndERNIn(lrn, List(consigneeId)).futureValue

      result.size mustBe 1
    }

    "return movement message with valid lrn and list of consignor and consignee Ids combination" in {
      saveMovementMessage(lrn, consignorId, consigneeId)
      val result = repository.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId, consigneeId)).futureValue

      result.size mustBe 1
    }

    "return movement message with valid lrn and only one valid ern combination in the list of erns" in {
      saveMovementMessage(lrn, consignorId, consigneeId)
      val result = repository.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId, "hhh", "222", "mmm")).futureValue

      result.size mustBe 1
    }

    "return one movement message with valid lrn and ern combination when multiple movements are available" in {
      saveMovementMessage(lrn, consignorId, consigneeId)
      saveMovementMessage("Test3333", consignorId, consigneeId)
      val result = repository.getMovementMessagesByLRNAndERNIn(lrn, List(consignorId)).futureValue

      result.size mustBe 1
    }

    "return empty list with invalid lrn and ern combination" in {
      saveMovementMessage(lrn, consignorId, consigneeId)
      val result = repository.getMovementMessagesByLRNAndERNIn("1111", List(consignorId)).futureValue

      result.isEmpty mustBe true
    }
  }

  private def saveMovementMessage(lrn: String, consignorId: String, consigneeId: String) = {
    await(repository.saveMovementMessage(MovementMessage(lrn, consignorId, Some(consigneeId), None)))
  }
}
