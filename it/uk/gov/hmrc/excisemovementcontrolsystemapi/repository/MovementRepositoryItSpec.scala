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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.{Clock, Instant, ZoneId}
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
    when(dateTimeService.now).thenReturn(timestamp)
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

  "getMovementByLRNAndERNIn" should {
    val lrn = "123"
    val consignorId = "Abc"
    val consigneeId = "def"
    val movement = Movement(lrn, consignorId, Some(consigneeId), None)


    "return movement with valid lrn and consignorId combination" in {
      saveMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId)).futureValue

      result mustBe Seq(movement)
    }

    "return movement with valid lrn and consigneeId combination" in {
      saveMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consigneeId)).futureValue

      result mustBe Seq(movement)
    }

    "return movement with valid lrn and list of consignor and consignee Ids combination" in {
      saveMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId, consigneeId)).futureValue

      result mustBe Seq(movement)
    }

    "return movement with valid lrn and only one valid ern combination in the list of erns" in {
      saveMovement(movement)
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId, "hhh", "222", "mmm")).futureValue

      result mustBe Seq(movement)
    }

    "return one movement with valid lrn and ern combination when multiple movements are available" in {
      saveMovement(movement)
      saveMovement(Movement("Test3333", consignorId, Some(consigneeId), None))
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId)).futureValue

      result mustBe Seq(movement)
    }

    "return empty list with invalid lrn and ern combination" in {
      saveMovement(movement)
      val result = repository.getMovementByLRNAndERNIn("1111", List(consignorId)).futureValue

      result mustBe Seq.empty
    }
  }

  "getMovementByErn" should {
    "return a list of movement" when {
      "ern match the consignorId " in {
        val expectedMovement1 = Movement("lrn", "ern1", None, Some("arc1"))
        val expectedMovement2 = Movement("lrn", "ern2", None, Some("arc2"))
        val expectedMovement3 = Movement("lrn1", "ern1", None, Some("arc3"))
        val expectedMovement4 = Movement("lrn4", "ern4", None, Some("arc4"))
        saveMovement(expectedMovement1)
        saveMovement(expectedMovement2)
        saveMovement(expectedMovement3)
        saveMovement(expectedMovement4)

        val result = repository.getMovementByERN(Seq("ern1", "ern2")).futureValue

        result mustBe Seq(expectedMovement1, expectedMovement2, expectedMovement3)
      }

      "ern match consignorId and consigneeId" in {
        val expectedMovement1 = Movement("lrn", "consignorId1", Some("ern1"), Some("arc1"))
        val expectedMovement2 = Movement("lrn3", "ern1", Some("ern2"), Some("arc2"))
        val expectedMovement3 = Movement("lrn1", "consignorId1", Some("ern1"), Some("arc3"))
        val expectedMovement4 = Movement("lrn4", "ern4", None, Some("arc4"))
        saveMovement(expectedMovement1)
        saveMovement(expectedMovement2)
        saveMovement(expectedMovement3)
        saveMovement(expectedMovement4)

        val result = repository.getMovementByERN(Seq("ern1")).futureValue

        result mustBe Seq(expectedMovement1, expectedMovement2, expectedMovement3)
      }
    }

    "return an empty list" in {
      val expectedMovement1 = Movement("lrn", "consignorId1", Some("ern1"), Some("arc1"))
      val expectedMovement2 = Movement("lrn", "consignorId2", Some("ern2"), Some("arc2"))
      val expectedMovement3 = Movement("lrn1", "consignorId1", Some("ern1"), Some("arc3"))
      saveMovement(expectedMovement1)
      saveMovement(expectedMovement2)
      saveMovement(expectedMovement3)

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
        saveMovement(expectedMovement1)
        saveMovement(expectedMovement2)
        saveMovement(expectedMovement3)
        saveMovement(expectedMovement4)

        val result = repository.getMovementByARC("arc2").futureValue

        result mustBe Seq(expectedMovement2)
      }
    }
  }

  private def saveMovement(movement: Movement) = {
    insert(movement).futureValue
  }
}
