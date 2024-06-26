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
import org.scalactic.source.Position
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.slf4j.MDC
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository.MessageNotification
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MovementRepositoryItSpec
    extends PlaySpec
    with CleanMongoCollectionSupport
    with DefaultPlayMongoRepositorySupport[Movement]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite {

  private lazy val dateTimeService = mock[DateTimeService]
  private lazy val timestamp       = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(dateTimeService)
    )
    .build()

  override protected lazy val repository: MovementRepository = app.injector.instanceOf[MovementRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  "saveMovement" should {

    val uuid     = UUID.randomUUID()
    val movement = Movement(uuid.toString, Some("boxId"), "123", "345", Some("789"), None, timestamp, Seq.empty)

    "return insert a movement" in {
      val result = repository.saveMovement(movement).futureValue

      val insertedRecord = find(
        Filters.and(
          Filters.equal("consignorId", "345"),
          Filters.equal("localReferenceNumber", "123")
        )
      ).futureValue.headOption.value

      result mustBe true
      insertedRecord mustBe movement
    }
  }

  "updateMovement" should {
    "update a movement by lrn and consignorId" in {
      val movementLRN1 = Movement(Some("boxId1"), "1", "345", Some("789"), None, timestamp)
      val movementLRN2 = Movement(Some("boxId2"), "2", "897", Some("456"), None)
      insertMovement(movementLRN1)
      insertMovement(movementLRN2)

      val message         = Message("any, message", MessageTypes.IE801.value, "messageId", "ern", Set.empty, timestamp)
      val updatedMovement = movementLRN2.copy(administrativeReferenceCode = Some("arc"), messages = Seq(message))
      val result          = repository.updateMovement(updatedMovement).futureValue

      val records = findAll().futureValue

      val expectedUpdated = updatedMovement.copy(lastUpdated = timestamp)
      result mustBe Some(expectedUpdated)
      records mustBe Seq(movementLRN1, expectedUpdated)
    }

    "update a movement by lrn and consigneeId" in {
      val movementLRN1 = Movement(Some("boxId1"), "1", "345", Some("789"), None, timestamp)
      val movementLRN2 = Movement(Some("boxId2"), "2", "897", Some("456"), None)
      insertMovement(movementLRN1)
      insertMovement(movementLRN2)

      val message        = Message("any, message", MessageTypes.IE801.value, "messageId", "ern", Set.empty, timestamp)
      val updateMovement = movementLRN2.copy(administrativeReferenceCode = Some("arc"), messages = Seq(message))
      val result         = repository.updateMovement(updateMovement).futureValue

      val records = findAll().futureValue

      result mustBe Some(updateMovement.copy(lastUpdated = timestamp))
      val expected = Seq(
        movementLRN1,
        movementLRN2.copy(administrativeReferenceCode = Some("arc"), lastUpdated = timestamp, messages = Seq(message))
      )
      records mustBe expected
    }

    "not update the movement if record not found" in {
      val movementLRN1 = Movement(Some("boxId1"), "1", "345", Some("789"), None, timestamp)
      val movementLRN2 = Movement(Some("boxId2"), "2", "897", Some("456"), None, timestamp)
      insertMovement(movementLRN1)
      insertMovement(movementLRN2)

      val message = Message("any, message", MessageTypes.IE801.value, "messageId", "ern", Set.empty, timestamp)
      val result  = repository
        .updateMovement(Movement(Some("boxId"), "4", "897", Some("321"), Some("arc"), Instant.now, Seq(message)))
        .futureValue

      val records = findAll().futureValue

      result mustBe None
      records mustBe Seq(movementLRN1, movementLRN2)
    }

    mustPreserveMdc(
      repository.updateMovement(Movement(Some("boxId"), "4", "897", Some("321"), Some("arc"), Instant.now, Seq.empty))
    )
  }

  "save" should {

    val movement =
      Movement(UUID.randomUUID().toString, Some("boxId"), "123", "345", Some("789"), None, timestamp, Seq.empty)

    "insert a movement if one does not exist" in {

      repository.save(movement).futureValue

      val records = find(Filters.empty()).futureValue
      records must contain only movement
    }

    "update a movement if one already exists" in {

      repository.save(movement).futureValue

      val updatedMovement = movement.copy(consigneeId = Some("678"))
      repository.save(updatedMovement).futureValue

      val records = find(Filters.empty()).futureValue
      records must contain only updatedMovement
    }

    "fail to insert a new movement if it has the same consignorId/lrn as another movement" in {

      repository.save(movement).futureValue

      val newMovement = movement.copy(_id = UUID.randomUUID().toString)
      repository.save(newMovement).failed.futureValue

      val records = find(Filters.empty()).futureValue
      records must contain only movement
    }

    mustPreserveMdc(repository.save(movement))
  }

  "getMovementById" should {
    "return the matching movement when it is there" in {
      val movementId1 = "49491927-aaa1-4835-b405-dd6e7fa3aaf0"
      val movementId2 = "8b43eb3b-3856-4f0c-b1ab-80355f70f6aa"
      val movement1   = Movement(movementId1, Some("boxId"), "lrn", "ern1", None, Some("arc1"), timestamp, Seq.empty)
      val movement2   = Movement(movementId2, Some("boxId"), "lrn", "ern2", None, Some("arc2"), timestamp, Seq.empty)
      insertMovement(movement1)
      insertMovement(movement2)

      val result = repository.getMovementById(movementId1).futureValue
      result mustBe Some(movement1)
    }

    "return None when no movement for given id" in {
      val movementId1 = "49491927-aaa1-4835-b405-dd6e7fa3aaf0"
      val movement1   = Movement(movementId1, Some("boxId"), "lrn", "ern1", None, Some("arc1"), timestamp, Seq.empty)
      insertMovement(movement1)

      val result = repository.getMovementById("23432343-2342342").futureValue

      result mustBe None
    }

    mustPreserveMdc(repository.getMovementById("someId"))
  }

  "getMovementByLRNAndERNIn" should {
    val lrn         = "123"
    val consignorId = "Abc"
    val consigneeId = "def"
    val movement    = Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), None, lastUpdated = timestamp)

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
      insertMovement(Movement(Some("boxId"), "Test3333", consignorId, Some(consigneeId), None))
      val result = repository.getMovementByLRNAndERNIn(lrn, List(consignorId)).futureValue

      result mustBe Seq(movement)
    }

    "return empty list with invalid lrn and ern combination" in {
      insertMovement(movement)
      val result = repository.getMovementByLRNAndERNIn("1111", List(consignorId)).futureValue

      result mustBe Seq.empty
    }

    mustPreserveMdc(repository.getMovementByLRNAndERNIn("someLrn", List("some ern")))
  }

  "getMovementByErn" should {
    "return a list of movement" when {
      "ern match the consignorId " in {
        val expectedMovement1 = Movement(Some("boxId"), "1", "ern1", None, Some("arc1"), lastUpdated = timestamp)
        val expectedMovement2 = Movement(Some("boxId"), "1", "ern2", None, Some("arc2"), lastUpdated = timestamp)
        val expectedMovement3 = Movement(Some("boxId"), "2", "ern1", None, Some("arc3"), lastUpdated = timestamp)
        val expectedMovement4 = Movement(Some("boxId"), "3", "ern4", None, Some("arc4"), lastUpdated = timestamp)
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val result = repository.getMovementByERN(Seq("ern1", "ern2")).futureValue

        result.sortBy(_.localReferenceNumber) mustBe Seq(expectedMovement1, expectedMovement2, expectedMovement3)
      }

      "ern match consignorId and consigneeId" in {
        val expectedMovement1 =
          Movement(Some("boxId"), "1", "consignorId1", Some("ern1"), Some("arc1"), lastUpdated = timestamp)
        val expectedMovement2 =
          Movement(Some("boxId"), "2", "ern1", Some("ern2"), Some("arc2"), lastUpdated = timestamp)
        val expectedMovement3 =
          Movement(Some("boxId"), "3", "consignorId1", Some("ern1"), Some("arc3"), lastUpdated = timestamp)
        val expectedMovement4 = Movement(Some("boxId"), "4", "ern4", None, Some("arc4"), lastUpdated = timestamp)
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val result = repository.getMovementByERN(Seq("ern1")).futureValue

        result.sortBy(_.localReferenceNumber) mustBe Seq(
          expectedMovement1,
          expectedMovement2,
          expectedMovement3
        )
      }
      "lrn from filter matches record from database" in {
        val expectedMovement1 =
          Movement(Some("boxId"), "1", "consignorId1", Some("ern1"), Some("arc1"), lastUpdated = timestamp)
        val expectedMovement2 =
          Movement(Some("boxId"), "2", "ern1", Some("ern2"), Some("arc2"), lastUpdated = timestamp)
        val expectedMovement3 =
          Movement(Some("boxId"), "3", "consignorId1", Some("ern1"), Some("arc3"), lastUpdated = timestamp)
        val expectedMovement4 = Movement(Some("boxId"), "4", "ern4", None, Some("arc4"), lastUpdated = timestamp)
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val result =
          repository.getMovementByERN(Seq("ern1"), MovementFilter.emptyFilter.copy(lrn = Some("1"))).futureValue

        result mustBe Seq(
          expectedMovement1
        )

      }

      "arc from filter matches record from database" in {
        val expectedMovement1 =
          Movement(Some("boxId"), "1", "consignorId1", Some("ern1"), Some("arc1"), lastUpdated = timestamp)
        val expectedMovement2 =
          Movement(Some("boxId"), "2", "ern1", Some("ern2"), Some("arc2"), lastUpdated = timestamp)
        val expectedMovement3 =
          Movement(Some("boxId"), "3", "consignorId1", Some("ern1"), Some("arc3"), lastUpdated = timestamp)
        val expectedMovement4 = Movement(Some("boxId"), "4", "ern4", None, Some("arc4"), lastUpdated = timestamp)
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val result =
          repository.getMovementByERN(Seq("ern1"), MovementFilter.emptyFilter.copy(arc = Some("arc1"))).futureValue

        result mustBe Seq(
          expectedMovement1
        )

      }

      "updatedSince from filter matches record from database" in {
        val expectedMovement1 =
          Movement(
            Some("boxId"),
            "1",
            "ern1",
            Some("ern1"),
            Some("arc1"),
            lastUpdated = timestamp.minus(5, ChronoUnit.MINUTES)
          )
        val expectedMovement2 =
          Movement(Some("boxId"), "2", "ern1", Some("ern2"), Some("arc2"), lastUpdated = timestamp)
        val expectedMovement3 =
          Movement(
            Some("boxId"),
            "3",
            "ern1",
            Some("ern1"),
            Some("arc3"),
            lastUpdated = timestamp.plus(5, ChronoUnit.MINUTES)
          )
        val expectedMovement4 =
          Movement(Some("boxId"), "4", "ern4", None, Some("arc4"), lastUpdated = timestamp)
        insertMovement(expectedMovement1)
        insertMovement(expectedMovement2)
        insertMovement(expectedMovement3)
        insertMovement(expectedMovement4)

        val filterTimestamp = timestamp

        val result =
          repository
            .getMovementByERN(Seq("ern1"), MovementFilter.emptyFilter.copy(updatedSince = Some(filterTimestamp)))
            .futureValue

        result.sortBy(_.localReferenceNumber) mustBe Seq(
          expectedMovement2,
          expectedMovement3
        )

      }
    }

    "arc and lrn from filter matches record from database" in {
      val expectedMovement1 =
        Movement(Some("boxId"), "1", "ern1", Some("ern1"), Some("arc1"), lastUpdated = timestamp)
      val expectedMovement2 =
        Movement(Some("boxId"), "2", "ern1", Some("ern2"), Some("arc1"), lastUpdated = timestamp)
      val expectedMovement3 =
        Movement(Some("boxId"), "3", "ern1", Some("ern1"), Some("arc2"), lastUpdated = timestamp)
      val expectedMovement4 = Movement(Some("boxId"), "4", "ern4", None, Some("arc4"), lastUpdated = timestamp)
      insertMovement(expectedMovement1)
      insertMovement(expectedMovement2)
      insertMovement(expectedMovement3)
      insertMovement(expectedMovement4)

      val result =
        repository
          .getMovementByERN(Seq("ern1"), MovementFilter.emptyFilter.copy(arc = Some("arc1"), lrn = Some("2")))
          .futureValue

      result mustBe Seq(
        expectedMovement2
      )

    }

    "arc and ern from filter matches record from database" in {
      val expectedMovement1 =
        Movement(Some("boxId"), "1", "ern1", Some("ern1"), Some("arc1"), lastUpdated = timestamp)
      val expectedMovement2 =
        Movement(
          Some("boxId"),
          "2",
          "ern1",
          Some("ern2"),
          Some("arc1"),
          lastUpdated = timestamp
        )
      val expectedMovement3 =
        Movement(Some("boxId"), "3", "ern2", Some("ern1"), Some("arc3"), lastUpdated = timestamp)
      val expectedMovement4 =
        Movement(Some("boxId"), "4", "ern4", None, Some("arc4"), lastUpdated = timestamp)
      insertMovement(expectedMovement1)
      insertMovement(expectedMovement2)
      insertMovement(expectedMovement3)
      insertMovement(expectedMovement4)

      val result =
        repository
          .getMovementByERN(
            Seq("ern1", "ern2"),
            MovementFilter.emptyFilter.copy(arc = Some("arc1"), ern = Some("ern1"))
          )
          .futureValue

      result.sortBy(_.localReferenceNumber) mustBe Seq(
        expectedMovement1,
        expectedMovement2
      )

    }

    "lrn and ern from filter matches record from database" in {
      val expectedMovement1 =
        Movement(Some("boxId"), "1", "ern1", Some("ern1"), Some("arc1"), lastUpdated = timestamp)
      val expectedMovement2 =
        Movement(
          Some("boxId"),
          "2",
          "ern1",
          Some("ern2"),
          Some("arc1"),
          lastUpdated = timestamp
        )
      val expectedMovement3 =
        Movement(Some("boxId"), "3", "ern2", Some("ern1"), Some("arc3"), lastUpdated = timestamp)
      val expectedMovement4 =
        Movement(Some("boxId"), "4", "ern4", None, Some("arc4"), lastUpdated = timestamp)
      insertMovement(expectedMovement1)
      insertMovement(expectedMovement2)
      insertMovement(expectedMovement3)
      insertMovement(expectedMovement4)

      val result =
        repository
          .getMovementByERN(
            Seq("ern1", "ern2"),
            MovementFilter.emptyFilter.copy(lrn = Some("1"), ern = Some("ern1"))
          )
          .futureValue

      result.sortBy(_.localReferenceNumber) mustBe Seq(expectedMovement1)

    }

    "return an empty list" in {
      val expectedMovement1 =
        Movement(Some("boxId"), "lrn", "consignorId1", Some("ern1"), Some("arc1"), lastUpdated = timestamp)
      val expectedMovement2 =
        Movement(Some("boxId"), "lrn", "consignorId2", Some("ern2"), Some("arc2"), lastUpdated = timestamp)
      val expectedMovement3 =
        Movement(Some("boxId"), "lrn1", "consignorId1", Some("ern1"), Some("arc3"), lastUpdated = timestamp)
      insertMovement(expectedMovement1)
      insertMovement(expectedMovement2)
      insertMovement(expectedMovement3)

      val result = repository.getMovementByERN(Seq("ern3")).futureValue

      result mustBe Seq.empty
    }

    mustPreserveMdc(repository.getMovementByERN(Seq("some ern")))
  }

  "getAll" should {
    "get all record for a consignorId" in {
      val movementLrn1 = Movement(Some("boxId"), "1", "345", Some("789"), None, timestamp)
      val movementLrn2 = Movement(Some("boxId"), "2", "897", Some("456"), None, timestamp)
      val movementLrn6 = Movement(Some("boxId"), "6", "345", Some("523"), None, timestamp)

      insertMovement(movementLrn1)
      insertMovement(movementLrn2)
      insertMovement(movementLrn6)

      val result = repository.getAllBy("345").futureValue

      result mustBe Seq(movementLrn1, movementLrn6)
    }

    "get all record for a consignee" in {
      val movementLrn1             = Movement(Some("boxId"), "1", "345", Some("789"), None, timestamp)
      val movementLrn2             = Movement(Some("boxId"), "2", "897", Some("456"), None, timestamp)
      val movementLrn6             = Movement(Some("boxId"), "6", "345", Some("523"), None, timestamp)
      val movementLrn1Consignor564 = Movement(Some("boxId"), "1", "564", Some("456"), None, timestamp)
      insertMovement(movementLrn1)
      insertMovement(movementLrn2)
      insertMovement(movementLrn6)
      insertMovement(movementLrn1Consignor564)

      val result = repository.getAllBy("456").futureValue

      result mustBe Seq(movementLrn2, movementLrn1Consignor564)
    }

    "return an empty list if there are no matching records" in {
      insertMovement(Movement(Some("boxId"), "1", "345", Some("789"), None))
      insertMovement(Movement(Some("boxId"), "2", "897", Some("456"), None))
      insertMovement(Movement(Some("boxId"), "6", "345", Some("523"), None))

      val result = repository.getAllBy("896").futureValue

      result mustBe Seq.empty
    }

    mustPreserveMdc(repository.getAllBy("ern"))
  }

  "getErnsAndLastReceived" should {

    "return a map of all of the ERNs that have received messages along with the latest time we received a message for them" in {

      val message1 =
        Message("encodedMessage", "type", "messageId", "recipient1", Set.empty, timestamp.minus(1, ChronoUnit.DAYS))
      val message2 = Message("encodedMessage", "type2", "messageId2", "recipient2", Set.empty, timestamp)
      val movement = Movement(
        UUID.randomUUID().toString,
        None,
        "123",
        "consignorId",
        Some("789"),
        None,
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message1, message2)
      )

      val message3  = Message("encodedMessage", "type", "messageId3", "recipient3", Set.empty, timestamp)
      val message4  = Message("encodedMessage", "type", "messageId4", "recipient1", Set.empty, timestamp)
      val movement2 = Movement(
        UUID.randomUUID().toString,
        None,
        "124",
        "consignorId",
        Some("789"),
        None,
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message3, message4)
      )

      repository.collection.insertMany(Seq(movement, movement2)).toFuture().futureValue

      val expected = Map(
        "recipient1" -> timestamp,
        "recipient2" -> timestamp,
        "recipient3" -> timestamp
      )

      repository.getErnsAndLastReceived.futureValue mustEqual expected
    }

    "must return an empty map when there are no movements" in {

      repository.getErnsAndLastReceived.futureValue mustEqual Map.empty
    }

    mustPreserveMdc(repository.getErnsAndLastReceived)
  }

  "getMessageNotifications" should {

    "return a list of MessageNotifications" in {

      val message1 =
        Message("encodedMessage", "type", "messageId", "recipient1", Set.empty, timestamp.minus(1, ChronoUnit.DAYS))
      val message2 = Message("encodedMessage", "type2", "messageId2", "recipient2", Set("boxId1"), timestamp)
      val movement = Movement(
        UUID.randomUUID().toString,
        None,
        "123",
        "consignorId",
        Some("789"),
        None,
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message1, message2)
      )

      val message3  = Message("encodedMessage", "type", "messageId3", "recipient3", Set("boxId1", "boxId2"), timestamp)
      val message4  = Message("encodedMessage", "type", "messageId4", "recipient1", Set.empty, timestamp)
      val movement2 = Movement(
        UUID.randomUUID().toString,
        None,
        "124",
        "consignorId",
        None,
        Some("arc"),
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message3, message4)
      )

      val message5  = Message("encodedMessage", "type", "messageId4", "recipient1", Set.empty, timestamp)
      val movement3 = Movement(
        UUID.randomUUID().toString,
        None,
        "125",
        "consignorId",
        None,
        Some("arc"),
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message5)
      )

      val movement4 = Movement(
        UUID.randomUUID().toString,
        None,
        "126",
        "consignorId",
        None,
        Some("arc"),
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq.empty
      )

      repository.collection.insertMany(Seq(movement, movement2, movement3, movement4)).toFuture().futureValue

      val expected = List(
        MessageNotification(
          movementId = movement._id,
          messageId = "messageId2",
          messageType = "type2",
          consignor = "consignorId",
          consignee = Some("789"),
          arc = None,
          recipient = "recipient2",
          boxId = "boxId1"
        ),
        MessageNotification(
          movementId = movement2._id,
          messageId = "messageId3",
          messageType = "type",
          consignor = "consignorId",
          consignee = None,
          arc = Some("arc"),
          recipient = "recipient3",
          boxId = "boxId1"
        ),
        MessageNotification(
          movementId = movement2._id,
          messageId = "messageId3",
          messageType = "type",
          consignor = "consignorId",
          consignee = None,
          arc = Some("arc"),
          recipient = "recipient3",
          boxId = "boxId2"
        )
      )

      val result = repository.getPendingMessageNotifications.futureValue

      result must contain theSameElementsAs expected
    }

    mustPreserveMdc(repository.getPendingMessageNotifications)
  }

  "confirmNotification" should {

    "update the relevant message to remove the relevant boxId from the list of boxesToNotify" in {

      val message1 = Message(
        "encodedMessage",
        "type",
        "messageId",
        "recipient1",
        Set("boxId1", "boxId2"),
        timestamp.minus(1, ChronoUnit.DAYS)
      )
      val message2 = Message("encodedMessage", "type2", "messageId2", "recipient2", Set("boxId1", "boxId2"), timestamp)
      val movement = Movement(
        UUID.randomUUID().toString,
        None,
        "123",
        "consignorId",
        Some("789"),
        None,
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message1, message2)
      )

      val message3  = Message("encodedMessage", "type", "messageId", "recipient3", Set("boxId1", "boxId2"), timestamp)
      val message4  = Message("encodedMessage", "type", "messageId4", "recipient1", Set("boxId1", "boxId2"), timestamp)
      val movement2 = Movement(
        UUID.randomUUID().toString,
        None,
        "124",
        "consignorId",
        None,
        Some("arc"),
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message3, message4)
      )

      repository.collection.insertMany(Seq(movement, movement2)).toFuture().futureValue

      val updatedMessage    = message1.copy(boxesToNotify = Set("boxId2"))
      val updatedMovement   = movement.copy(messages = Seq(updatedMessage, message2))
      val expectedMovements = Seq(updatedMovement, movement2)

      repository.confirmNotification(movement._id, message1.messageId, "boxId1").futureValue

      repository.collection.find().toFuture().futureValue must contain theSameElementsAs expectedMovements
    }

    "must not fail if there is no matching movement" in {
      repository.confirmNotification("movementId", "messageId", "boxId").futureValue
    }

    "must not fail if there is no matching message" in {

      val message1 = Message(
        "encodedMessage",
        "type",
        "messageId",
        "recipient1",
        Set("boxId1", "boxId2"),
        timestamp.minus(1, ChronoUnit.DAYS)
      )
      val message2 = Message("encodedMessage", "type2", "messageId2", "recipient2", Set("boxId1", "boxId2"), timestamp)
      val movement = Movement(
        UUID.randomUUID().toString,
        None,
        "123",
        "consignorId",
        Some("789"),
        None,
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message1, message2)
      )

      repository.collection.insertOne(movement)

      repository.confirmNotification(movement._id, "foo", "boxId").futureValue
    }

    "must not fail if there is no matching boxId" in {

      val message1 = Message(
        "encodedMessage",
        "type",
        "messageId",
        "recipient1",
        Set("boxId1", "boxId2"),
        timestamp.minus(1, ChronoUnit.DAYS)
      )
      val message2 = Message("encodedMessage", "type2", "messageId2", "recipient2", Set("boxId1", "boxId2"), timestamp)
      val movement = Movement(
        UUID.randomUUID().toString,
        None,
        "123",
        "consignorId",
        Some("789"),
        None,
        timestamp.truncatedTo(ChronoUnit.MILLIS),
        Seq(message1, message2)
      )

      repository.collection.insertOne(movement)

      repository.confirmNotification(movement._id, message1.messageId, "bar").futureValue
    }

    mustPreserveMdc(repository.confirmNotification("movementId", "messageId", "boxId"))
  }

  private def insertMovement(movement: Movement) =
    insert(movement).futureValue

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      val ec = app.injector.instanceOf[ExecutionContext]

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }(ec).futureValue
    }
}
