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
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class MovementRepositoryItSpec extends PlaySpec
  with CleanMongoCollectionSupport
  with PlayMongoRepositorySupport[Movement]
  with IntegrationPatience
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with OptionValues
  with GuiceOneAppPerSuite {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val appConfig = app.injector.instanceOf[AppConfig]
  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")

  protected override val repository = new MovementRepository(
    mongoComponent,
    appConfig,
    dateTimeService
  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(dateTimeService.instant).thenReturn(timestamp)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    dropDatabase()
  }

  "saveMovement" should {
    "return insert a movement" in {
      val result = repository.saveMovement(Movement("123", "345", Some("789"), None)).futureValue

      val insertedRecord = find(
        Filters.and(
          Filters.equal("consignorId", "345"),
          Filters.equal("localReferenceNumber", "123")
        )
      ).futureValue
        .headOption
        .value

      result mustBe true
      insertedRecord.localReferenceNumber mustBe "123"
      insertedRecord.consignorId mustBe "345"
      insertedRecord.consigneeId mustBe Some("789")
      insertedRecord.administrativeReferenceCode mustBe None
    }
  }

  "updateMovement" should {
    "update a movement by lrn and consignorId" in {
      insertMovement(Movement("1", "345", Some("789"), None, timestamp))
      insertMovement(Movement("2", "897", Some("456"), None))

      val message =  Message("any, message", MessageTypes.IE801.value, dateTimeService)
      val result = repository.updateMovement(Movement("2", "897", Some("321"), Some("arc"), Instant.now, Seq(message))).futureValue

      val records = findAll().futureValue

      val expected = Seq(
        Movement("1", "345", Some("789"), None, timestamp),
        Movement("2", "897", Some("321"), Some("arc"), timestamp, Seq(message))
      )
      assertUpdateMovementResult(records, expected, result)
    }

    "update a movement by lrn and consigneeId" in {
      insertMovement(Movement("1", "345", Some("789"), None, timestamp))
      insertMovement(Movement("2", "897", Some("456"), None))

      val message =  Message("any, message", MessageTypes.IE801.value, dateTimeService)
      val result = repository.updateMovement(Movement("2", "78", Some("456"), Some("arc"), Instant.now, Seq(message))).futureValue

      val records = findAll().futureValue

      val expected = Seq(
        Movement("1", "345", Some("789"), None, timestamp),
        Movement("2", "897", Some("456"), Some("arc"), timestamp, Seq(message))
      )
      assertUpdateMovementResult(records, expected, result)
    }

    "not update the movement if record not found" in {
      val instant = Instant.now
      insertMovement(Movement("1", "345", Some("789"), None, instant))
      insertMovement(Movement("2", "897", Some("456"), None, instant))

      val message =  Message("any, message", MessageTypes.IE801.value, dateTimeService)
      val result = repository.updateMovement(Movement("4", "897", Some("321"), Some("arc"), Instant.now, Seq(message))).futureValue

      val records = findAll().futureValue

      val expected = Seq(
        Movement("1", "345", Some("789"), None, instant),
        Movement("2", "897", Some("456"), None, instant)
      )
      assertUpdateMovementResult(records, expected, result)
    }
  }

  private def assertUpdateMovementResult(actual: Seq[Movement], expected: Seq[Movement], result: Boolean) = {
    result mustBe true
    actual.size mustBe 2
    actual.sortBy(_.localReferenceNumber) mustBe expected
  }

  "getMovementByLRNAndERNIn" should {
    val lrn = "123"
    val consignorId = "Abc"
    val consigneeId = "def"
    val movement = Movement(lrn, consignorId, Some(consigneeId), None)


    "return movement with valid lrn and consignorId combination" in {
      insertMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId)).futureValue

      result mustBe Seq(movement)
    }

    "return movement with valid lrn and consigneeId combination" in {
      insertMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consigneeId)).futureValue

      result mustBe Seq(movement)
    }

    "return movement with valid lrn and list of consignor and consignee Ids combination" in {
      insertMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId, consigneeId)).futureValue

      result mustBe Seq(movement)
    }

    "return movement with valid lrn and only one valid ern combination in the list of erns" in {
      insertMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId, "hhh", "222", "mmm")).futureValue

      result mustBe Seq(movement)
    }

    "return one movement with valid lrn and ern combination when multiple movements are available" in {
      insertMovement(movement)
      insertMovement(Movement("Test3333", consignorId, Some(consigneeId), None))
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId)).futureValue

      result mustBe Seq(movement)
    }

    "return empty list with invalid lrn and ern combination" in {
      insertMovement(movement)
      val result = repository.getMovementByLRNAndERNIn("1111", List(consignorId)).futureValue

      result mustBe Seq.empty
    }
  }

  "getMovementByErn" should {
    "return a list of movement" when {
      "ern match the consignorId " in {
        val expectedMovement1 = Movement("1", "ern1", None, Some("arc1"))
        val expectedMovement2 = Movement("1", "ern2", None, Some("arc2"))
        val expectedMovement3 = Movement("2", "ern1", None, Some("arc3"))
        val expectedMovement4 = Movement("3", "ern4", None, Some("arc4"))
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val result = repository.getMovementByERN(Seq("ern1", "ern2")).futureValue

        result.sortBy(_.localReferenceNumber) mustBe Seq(expectedMovement1, expectedMovement2, expectedMovement3)
      }

      "ern match consignorId and consigneeId" in {
        val expectedMovement1 = Movement("1", "consignorId1", Some("ern1"), Some("arc1"))
        val expectedMovement2 = Movement("2", "ern1", Some("ern2"), Some("arc2"))
        val expectedMovement3 = Movement("3", "consignorId1", Some("ern1"), Some("arc3"))
        val expectedMovement4 = Movement("4", "ern4", None, Some("arc4"))
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val result = repository.getMovementByERN(Seq("ern1")).futureValue

        result.sortBy(_.localReferenceNumber) mustBe Seq(
          expectedMovement1, expectedMovement2, expectedMovement3
        )
      }
    }

    "return an empty list" in {
      val expectedMovement1 = Movement("lrn", "consignorId1", Some("ern1"), Some("arc1"))
      val expectedMovement2 = Movement("lrn", "consignorId2", Some("ern2"), Some("arc2"))
      val expectedMovement3 = Movement("lrn1", "consignorId1", Some("ern1"), Some("arc3"))
      insertMovement(expectedMovement1)
      insertMovement(expectedMovement2)
      insertMovement(expectedMovement3)

      val result = repository.getMovementByERN(Seq("ern3")).futureValue

      result mustBe Seq.empty
    }


  }

  "getMovementByArc" should {
    "return a list of movement" when {
      "arc matches the supplied arc " in {
        val expectedMovement1 = Movement("lrn", "ern1", None, Some("arc1"))
        val expectedMovement2 = Movement("lrn", "ern2", None, Some("arc2"))
        val expectedMovement3 = Movement("lrn1", "ern1", None, Some("arc3"))
        val expectedMovement4 = Movement("lrn4", "ern4", None, Some("arc4"))
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val result = repository.getMovementByARC("arc2").futureValue

        result mustBe Seq(expectedMovement2)
      }
    }
  }

  "getAll" should {
    "get all record for a consignorId" in {
      val instant = Instant.now
      insertMovement(Movement("1", "345", Some("789"), None, instant))
      insertMovement(Movement("2", "897", Some("456"), None, instant))
      insertMovement(Movement("6", "345", Some("523"), None, instant))

      val result = repository.getAllBy("345").futureValue

      result mustBe Seq(
        Movement("1", "345", Some("789"), None, instant),
        Movement("6", "345", Some("523"), None, instant)
      )
    }

    "get all record for a consignee" in {
      val instant = Instant.now
      insertMovement(Movement("1", "345", Some("789"), None, instant))
      insertMovement(Movement("2", "897", Some("456"), None, instant))
      insertMovement(Movement("6", "345", Some("523"), None, instant))
      insertMovement(Movement("1", "564", Some("456"), None, instant))

      val result = repository.getAllBy("456").futureValue

      result mustBe Seq(
        Movement("2", "897", Some("456"), None, instant),
        Movement("1", "564", Some("456"), None, instant)
      )
    }

    "return an empty list if there are no matching records" in {
      insertMovement(Movement("1", "345", Some("789"), None))
      insertMovement(Movement("2", "897", Some("456"), None))
      insertMovement(Movement("6", "345", Some("523"), None))

      val result = repository.getAllBy("896").futureValue

      result mustBe Seq.empty
    }
  }

  private def insertMovement(movement: Movement) = {
    insert(movement).futureValue
  }
}
