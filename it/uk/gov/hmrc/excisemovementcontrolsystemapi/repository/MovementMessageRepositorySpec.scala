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

import org.mockito.MockitoSugar.when
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.DateTimeService
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class MovementMessageRepositorySpec extends PlaySpec
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with PlayMongoRepositorySupport[Movement]
  with CleanMongoCollectionSupport
  with IntegrationPatience
  with GuiceOneAppPerSuite {


  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val appConfig = app.injector.instanceOf[AppConfig]
  private lazy val timeService = mock[DateTimeService]

  protected override val repository = new MovementMessageRepository(
    mongoComponent,
    appConfig,
    timeService
  )

  protected def appBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(timeService.now).thenReturn(Instant.parse("2018-11-30T18:35:24.00Z"))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    dropDatabase()
  }

  "save" should {
    "update a movement"  when {
      "consigneeId is missing " in {
        val message1 = Message("any message", MessageTypes.IE704.value, timeService)
        val message2 = Message("any message 2", MessageTypes.IE801.value, timeService)
        val movement = Movement("123", "345", None, None, Seq(message1))

        insert(Movement("123", "345", None, None, Seq.empty)).futureValue
        insert(Movement("12", "789", None, None, Seq(message2))).futureValue

        val result = repository.save(movement).futureValue

        val insertedRecord= findRecordBy("345", "123")

        result mustEqual true
        verifyResults(insertedRecord, movement, Seq(message1))
      }

      "consigneeId is present " in {
        val message1 = Message("any message", MessageTypes.IE704.value, timeService)
        val message2 = Message("any message 2", MessageTypes.IE801.value, timeService)
        val movement = Movement("123", "345", Some("456"), None, Seq(message1))

        insert(Movement("123", "345", None, None, Seq.empty)).futureValue
        insert(Movement("12", "789", None, None, Seq(message2))).futureValue

        val result = repository.save(movement).futureValue

        val insertedRecord = findRecordBy("345", "123")

        result mustEqual true
        verifyResults(insertedRecord, movement, Seq(message1))
      }
    }

    "insert a new record" in {
      val movement = Movement("123", "345", Some("789"))
      val result = repository.save(movement).futureValue

      val insertedRecord = findRecordBy("345", "123")

      result mustEqual true
      verifyResults(insertedRecord, movement, Seq.empty)
    }
  }

  "getMovements" should {
    "return all available movements" in {
      val movement1 = Movement("lrn", "consignorId", Some("consigneeId"), None)
      val movement2 = Movement("lrn1", "consignorId_1", Some("consigneeId_1"), None)

      insert(movement1).futureValue
      insert(movement2).futureValue

      val result = repository.getMovements.futureValue

      result mustBe Seq(movement1, movement2)
    }
  }

  "getMovementMessagesByLRNAndERNIn" should {
    val lrn = "123"
    val consignorId = "Abc"
    val consigneeId = "def"
    val movement = Movement(lrn, consignorId, Some(consigneeId), None)


    "return movement message with valid lrn and consignorId combination" in {
      insert(movement).futureValue

      val result = repository.get(lrn, List(consignorId)).futureValue

      result mustBe Some(movement)
    }


    "return movement message with valid lrn and list of consignor and consignee Ids combination" in {
      insert(movement).futureValue

      val result = repository.get(lrn, List(consignorId, consigneeId)).futureValue

      result mustBe Some(movement)
    }

    "return movement message with valid lrn and only one valid ern combination in the list of erns" in {
      insert(movement).futureValue

      val result = repository.get(lrn, List(consignorId, "hhh", "222", "mmm")).futureValue

      result mustBe Some(movement)
    }

    "return one movement message with valid lrn and ern combination when multiple movements are available" in {
      insert(movement).futureValue
      insert(Movement("Test3333", consignorId, Some(consigneeId), None)).futureValue

      val result = repository.get(lrn, List(consignorId)).futureValue

      result mustBe Some(movement)
    }

    "return empty list with invalid lrn and ern combination" in {
      insert(movement).futureValue

      val result = repository.get("1111", List(consignorId)).futureValue

      result mustBe None
    }
  }

  private def verifyResults(
                             actual: Movement,
                             expected: Movement,
                             message: Seq[Message]
  ) = {
    actual.localReferenceNumber mustEqual expected.localReferenceNumber
    actual.consignorId mustEqual expected.consignorId
    actual.consigneeId mustEqual expected.consigneeId
    actual.administrativeReferenceCode mustEqual None
    actual.messages mustEqual message
  }

  private def findRecordBy(consignorId: String, localReferenceNumber: String): Movement = {
    find(
      Filters.and(
        Filters.equal("consignorId", consignorId),
        Filters.equal("localReferenceNumber", localReferenceNumber)
      )
    ).futureValue
      .headOption
      .value
  }
}
