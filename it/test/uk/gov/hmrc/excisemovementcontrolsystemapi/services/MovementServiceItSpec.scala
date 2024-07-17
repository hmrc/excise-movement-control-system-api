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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}

import java.time.Instant
import java.time.temporal.ChronoUnit

class MovementServiceItSpec
    extends PlaySpec
    with CleanMongoCollectionSupport
    with EitherValues
    with DefaultPlayMongoRepositorySupport[Movement]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite {

  private val dateTimeService = mock[DateTimeService]
  private val timestamp       = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(dateTimeService)
    )
    .build()

  override protected lazy val repository: MovementRepository = app.injector.instanceOf[MovementRepository]
  private lazy val movementService                           = app.injector.instanceOf[MovementService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  "saveNewMovement" should {
    val lrn            = "123"
    val consignorId    = "Abc"
    val consigneeId    = "def"
    val otherConsignor = "Jkl"
    val otherConsignee = "ghi"
    val arc            = "arc1"
    val movement       = Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), None, lastUpdated = timestamp)
    val diffConsignee  = Movement(Some("boxId"), lrn, consignorId, Some(otherConsignee), None, lastUpdated = timestamp)
    val diffConsignor  = Movement(Some("boxId"), lrn, otherConsignor, Some(consigneeId), None, lastUpdated = timestamp)
    val switched       = Movement(Some("boxId"), lrn, consigneeId, Some(consignorId), None, lastUpdated = timestamp)
    val withArc        = Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), Some(arc), lastUpdated = timestamp)

    "throw an error when LRN is already in database for consignor with an ARC" in {
      insert(withArc).futureValue

      val result = movementService.saveNewMovement(movement).futureValue

      val expectedError = ErrorResponse(
        timestamp,
        "Duplicate LRN error",
        "The local reference number 123 has already been used for another movement"
      )
      result.left.value mustBe BadRequest(Json.toJson(expectedError))
    }

    "throw an error when LRN is already in database with no ARC for same consignor but different consignee" in {
      insert(diffConsignee).futureValue

      val result = movementService.saveNewMovement(movement).futureValue

      val expectedError = ErrorResponse(
        timestamp,
        "Duplicate LRN error",
        "The local reference number 123 has already been used for another movement"
      )
      result.left.value mustBe BadRequest(Json.toJson(expectedError))
    }

    "return the newly saved movement when LRN is already in database for different consignor but same consignee" in {
      insert(diffConsignor).futureValue

      val result = movementService.saveNewMovement(movement).futureValue

      result mustBe Right(movement)
    }

    "return the newly saved movement when LRN is already in database as a consignee and it is submitted as a consignor" in {
      insert(switched).futureValue

      val result = movementService.saveNewMovement(movement).futureValue

      result mustBe Right(movement)
    }
  }
}
