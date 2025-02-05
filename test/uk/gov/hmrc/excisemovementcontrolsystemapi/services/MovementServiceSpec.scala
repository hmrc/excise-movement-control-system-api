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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.mongodb.MongoCommandException
import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.mongodb.scala.ServerAddress
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.{MovementFilter, TraderType}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.IE704
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE704Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class MovementServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach with ScalaFutures {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val mockMovementRepository = mock[MovementRepository]
  private val auditService           = mock[AuditService]
  private val testDateTime: Instant  = Instant.parse("2023-11-15T17:02:34.123456Z")
  private val dateTimeService        = mock[DateTimeService]
  private val utils                  = new EmcsUtils

  when(dateTimeService.timestamp()).thenReturn(testDateTime)

  private val movementService = new MovementService(mockMovementRepository, dateTimeService, auditService)

  private val lrn         = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"
  private val newMessage  = mock[IEMessage]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockMovementRepository, newMessage, auditService)
  }

  private val ie704          = XmlMessageGeneratorFactory.generate(
    "ern",
    MessageParams(IE704, "XI000001", localReferenceNumber = Some("lrnie8158976912"))
  )
  private val ieMessage      = IE704Message.createFromXml(ie704)
  private val exampleMessage = Message(
    utils.encode(ieMessage.toXml.toString()),
    "IE704",
    "XI000001",
    "ern",
    Set("boxId1", "boxId2"),
    Instant.now()
  )

  private val exampleMovement: Movement =
    Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), messages = Seq(exampleMessage))

  "saveNewMovement" should {
    "return a Movement" in {
      val successMovement = exampleMovement
      when(mockMovementRepository.findDraftMovement(any))
        .thenReturn(Future.successful(None))

      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(Done))

      val result = await(movementService.saveNewMovement(successMovement))

      result mustBe Right(successMovement)
    }

    "return an internal server error when cannot save movement" in {
      when(mockMovementRepository.findDraftMovement(any))
        .thenReturn(Future.successful(None))

      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(movementService.saveNewMovement(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Database error", "error")
      result.left.value mustBe InternalServerError(Json.toJson(expectedError))
    }

    "return a bad request when there is a duplicate key exception" in {
      when(mockMovementRepository.findDraftMovement(any))
        .thenReturn(Future.successful(None))
      when(mockMovementRepository.saveMovement(any))
        .thenReturn(
          Future.failed(
            new MongoCommandException(BsonDocument(), ServerAddress())
          )
        )

      val result = await(movementService.saveNewMovement(exampleMovement))

      val expectedError = ErrorResponse(
        testDateTime,
        "Duplicate LRN error",
        "The local reference number 123 has already been used for another movement"
      )
      result.left.value mustBe BadRequest(Json.toJson(expectedError))
    }

    "return an internal server error if there is an issue finding the draft movement" in {
      when(mockMovementRepository.findDraftMovement(any))
        .thenReturn(Future.failed(new RuntimeException("Database error")))
      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(Done))

      val result = await(movementService.saveNewMovement(exampleMovement))

      val expectedError = ErrorResponse(testDateTime, "Database error", "Database error")
      result.left.value mustBe InternalServerError(Json.toJson(expectedError))
    }

    "return the database movement when LRN is already in database with no ARC for same consignor and same consignee" in {

      when(mockMovementRepository.findDraftMovement(any))
        .thenReturn(Future.successful(Some(exampleMovement)))
      when(mockMovementRepository.saveMovement(any))
        .thenReturn(Future.successful(Done))

      val result = await(movementService.saveNewMovement(exampleMovement))

      result mustBe Right(exampleMovement)
    }

  }

  "saveMovement" should {
    "if movementRepository.saveMovement() returns a success" should {
      "return success if movementRepository.saveMovement() returns a success" in {
        val successMovement = exampleMovement

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        val result = await(movementService.saveMovement(successMovement))

        result mustBe Done
      }
      "call an auditService.movementSavedSuccess if call to repository is a Success" in {
        val successMovement = exampleMovement

        //TODO: Take another look at batchId generation
        val batchId = "123"

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        await(movementService.saveMovement(successMovement))

        //TODO: Calculate movements properly, take another look at jobId as well
        verify(auditService, times(1)).movementSavedSuccess(1, 1, successMovement, batchId, None)
      }

      "pass jobId to audit service in success case" in {
        val successMovement = exampleMovement
        val batchId         = "123"
        val jobId           = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(1), eqTo(1), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)
      }

      "pass messagesAdded = 1 and totalMessages = 1 for a new movement with one message when a call to repository is a success" in {
        val successMovement = exampleMovement

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(1), eqTo(1), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }

      "pass messagesAdded = 2 and totalMessages = 2 for a new movement with two messages when a call to repository is a success" in {
        val successMovement = exampleMovement.copy(messages = Seq(exampleMessage, exampleMessage))

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(2), eqTo(2), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }

      "pass messagesAdded = 10 and totalMessages = 10 for a new movement with two messages when a call to repository is a success" in {
        val messages        = for {
          _ <- 1 to 10
        } yield exampleMessage
        val successMovement = exampleMovement.copy(messages = messages)

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(10), eqTo(10), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }

      "pass messagesAdded = 0 when the single message for a movement is a message that is already present in mongo when a call to repository is a success" in {

        val successMovement = exampleMovement.copy(messages = Seq(exampleMessage))

        val messagesAlreadyInMongo = Seq(exampleMessage)

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId, messagesAlreadyInMongo).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(0), eqTo(1), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }

      "pass messagesAdded = 0 when both messages for a movement are a message that is already present in mongo when a call to repository is a success" in {

        val exampleMessage2 = exampleMessage.copy(messageId = UUID.randomUUID().toString)

        val exampleMessagesAlreadyInMongo =
          Seq(exampleMessage, exampleMessage2)

        val successMovement = exampleMovement.copy(messages = exampleMessagesAlreadyInMongo)

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId, exampleMessagesAlreadyInMongo).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(0), eqTo(2), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }

      "pass messagesAdded = 1 when one of the two messages for a movement is a message that is already present in mongo when a call to repository is a success" in {

        val exampleMessage2 = exampleMessage.copy(messageId = UUID.randomUUID().toString)

        val exampleMessagesAlreadyInMongo = Seq(exampleMessage2)

        val successMovement = exampleMovement.copy(messages = Seq(exampleMessage, exampleMessage2))

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId, exampleMessagesAlreadyInMongo).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(1), eqTo(2), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }

      "pass messagesAdded = 2 when neither of the two messages for a movement is a message that is already present in mongo when a call to repository is a success" in {

        val exampleMessage2 = exampleMessage.copy(messageId = UUID.randomUUID().toString)

        val exampleMessage3 = exampleMessage.copy(messageId = UUID.randomUUID().toString)
        val exampleMessage4 = exampleMessage.copy(messageId = UUID.randomUUID().toString)

        val exampleMessagesAlreadyInMongo = Seq(exampleMessage, exampleMessage2)

        val successMovement = exampleMovement.copy(messages = Seq(exampleMessage3, exampleMessage4))

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId, exampleMessagesAlreadyInMongo).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(2), eqTo(2), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }

      "pass messagesAdded = 3 when five of the eight messages for a movement are a messages that are already present in mongo when a call to repository is a success" in {

        val exampleMessage2 = exampleMessage.copy(messageId = UUID.randomUUID().toString)
        val exampleMessage3 = exampleMessage.copy(messageId = UUID.randomUUID().toString)
        val exampleMessage4 = exampleMessage.copy(messageId = UUID.randomUUID().toString)
        val exampleMessage5 = exampleMessage.copy(messageId = UUID.randomUUID().toString)

        val exampleMessage6 = exampleMessage.copy(messageId = UUID.randomUUID().toString)
        val exampleMessage7 = exampleMessage.copy(messageId = UUID.randomUUID().toString)
        val exampleMessage8 = exampleMessage.copy(messageId = UUID.randomUUID().toString)

        val exampleMessagesAlreadyInMongo =
          Seq(exampleMessage, exampleMessage2, exampleMessage3, exampleMessage4, exampleMessage5)

        val successMovement = exampleMovement.copy(messages =
          exampleMessagesAlreadyInMongo ++ Seq(exampleMessage6, exampleMessage7, exampleMessage8)
        )

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.successful(Done))

        movementService.saveMovement(successMovement, jobId, exampleMessagesAlreadyInMongo).futureValue

        verify(auditService, times(1))
          .movementSavedSuccess(eqTo(3), eqTo(8), eqTo(successMovement), eqTo(batchId), eqTo(jobId))(any)

      }
    }

    "if movementRepository.saveMovement() returns a failure" should {
      "return failure if movementRepository.saveMovement() returns a failure" in {
        val successMovement = exampleMovement

        val exception = new MongoCommandException(new BsonDocument(), new ServerAddress())

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.failed(exception))

        the[MongoCommandException] thrownBy {
          await(movementService.saveMovement(successMovement))
        } must have message exception.getMessage
      }

      "call an auditService.movementSavedFailure if call to repository is a Failure" in {
        val movement = exampleMovement
        val batchId  = "123"

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.failed(new RuntimeException("Failed reason")))

        //TODO: Calculate movements properly
        val result = movementService.saveMovement(movement).failed.futureValue

        verify(auditService, times(1)).movementSavedFailure(1, 1, movement, result.getMessage, batchId, None)

      }

      "pass jobId to audit service in failure case" in {
        val movement = exampleMovement
        val batchId  = "123"
        val jobId    = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.failed(new RuntimeException("Failed reason")))

        val result = movementService.saveMovement(movement, jobId).failed.futureValue

        verify(auditService, times(1)).movementSavedFailure(1, 1, movement, result.getMessage, batchId, jobId)
      }

      "pass messagesToBeAdded = 2 and totalMessages = 2 for a new movement with two messages when a call to repository is a failure" in {
        val movement = exampleMovement.copy(messages = Seq(exampleMessage, exampleMessage))

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.failed(new RuntimeException("Failed reason")))

        val result = movementService.saveMovement(movement, jobId).failed.futureValue

        verify(auditService, times(1)).movementSavedFailure(2, 2, movement, result.getMessage, batchId, jobId)

      }

      "pass messagesToBeAdded = 10 and totalMessages = 10 for a new movement with two messages when a call to repository is a failure" in {
        val messages = for {
          _ <- 1 to 10
        } yield exampleMessage
        val movement = exampleMovement.copy(messages = messages)

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.failed(new RuntimeException("Failed reason")))

        val result = movementService.saveMovement(movement, jobId).failed.futureValue

        verify(auditService, times(1)).movementSavedFailure(10, 10, movement, result.getMessage, batchId, jobId)

      }

      "pass messagesToBeAdded = 1 and totalMessages = 1 for a new movement with one message when a call to repository is a failure" in {
        val successMovement = exampleMovement

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.failed(new RuntimeException("Failure reason")))

        val result = movementService.saveMovement(successMovement, jobId).failed.futureValue

        verify(auditService, times(1))
          .movementSavedFailure(
            eqTo(1),
            eqTo(1),
            eqTo(successMovement),
            eqTo(result.getMessage),
            eqTo(batchId),
            eqTo(jobId)
          )(any)

      }

      "pass messagesToBeAdded = 0 when the single message for a movement is a message that is already present in mongo when a call to repository is a failure" in {

        val successMovement = exampleMovement.copy(messages = Seq(exampleMessage))

        val messagesAlreadyInMongo = Seq(exampleMessage)

        val batchId = "123"
        val jobId   = Some(UUID.randomUUID().toString)

        when(mockMovementRepository.saveMovement(any))
          .thenReturn(Future.failed(new RuntimeException("Failure reason")))

        val result = movementService.saveMovement(successMovement, jobId, messagesAlreadyInMongo).failed.futureValue

        verify(auditService, times(1))
          .movementSavedFailure(
            eqTo(0),
            eqTo(1),
            eqTo(successMovement),
            eqTo(result.getMessage),
            eqTo(batchId),
            eqTo(jobId)
          )(any)

      }
    }

  }

  "getMovementByLRNAndERNIn with valid LRN and ERN combination" should {
    "return  a movement" in {
      val message1 = Message("123456", "IE801", "messageId1", "ern", Set.empty, testDateTime)
      val message2 = Message("ABCDE", "IE815", "messageId2", "ern", Set.empty, testDateTime)
      val movement =
        Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), None, Instant.now(), Seq(message1, message2))
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(movement)))

      val result = await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe Some(movement)
    }

    "throw an error if multiple movement found" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(
          Future.successful(
            Seq(
              Movement(Some("boxId"), "lrn1", "consignorId1", None),
              Movement(Some("boxId"), "lrn2", "consignorId2", None)
            )
          )
        )

      intercept[RuntimeException] {
        await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))
      }.getMessage mustBe s"[MovementService] - Multiple movement found for local reference number"
    }
  }

  "getMovementByLRNAndERNIn with no movement message for LRN and ERN combination" should {
    "return no movement" in {
      when(mockMovementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementService.getMovementByLRNAndERNIn(lrn, List(consignorId)))

      result mustBe None
    }
  }

  "getMovementByErn" should {

    val lrnToFilterBy = "lrn2"
    val ernToFilterBy = "ABC2"
    val arcToFilterBy = "arc2"

    "return all that movement for that ERN" in {
      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(expectedMovement1)))

      val result = await(movementService.getMovementByErn(Seq(consignorId)))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter ERN" in {
      val expectedMovement1 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))

      val filter = MovementFilter.emptyFilter.copy(ern = Some(consignorId))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId), filter))
        .thenReturn(Future.successful(Seq(expectedMovement1)))

      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement1)
    }

    "return only the movements that correspond to the filter LRN" in {
      val lrnToFilterBy     = "lrn2"
      val expectedMovement2 = Movement(Some("boxId"), lrnToFilterBy, consignorId, None, Some("arc1"))
      val filter            =
        MovementFilter(ern = None, lrn = Some(lrnToFilterBy), arc = None, updatedSince = None, traderType = None)

      when(mockMovementRepository.getMovementByERN(Seq(consignorId), filter))
        .thenReturn(Future.successful(Seq(expectedMovement2)))

      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }

    "return only the movements that correspond to the filter ARC" in {
      val expectedMovement2 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc2"))
      val filter            = MovementFilter.emptyFilter.copy(arc = Some(arcToFilterBy))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId), filter))
        .thenReturn(Future.successful(Seq(expectedMovement2)))

      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement2)
    }

    "return only the movements that correspond to the filter traderType equals consignor" in {
      val expectedMovement  = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc2"))
      val expectedMovement2 = Movement(Some("boxId"), "lrn1", "consignee", None, Some("arc2"))
      val filter            = MovementFilter.emptyFilter.copy(traderType = Some(TraderType("consignor", Seq(consignorId))))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId), filter))
        .thenReturn(Future.successful(Seq(expectedMovement, expectedMovement2)))

      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement)
    }

    "return only the movements that correspond to the filter traderType equals consignee" in {
      val expectedMovement  = Movement(Some("boxId"), "lrn1", "test-consignorId", Some("consigneeId"), Some("arc1"))
      val expectedMovement2 = Movement(Some("boxId"), "lrn1", consignorId, None, Some("arc1"))
      val filter            = MovementFilter.emptyFilter.copy(traderType = Some(TraderType("consignee", Seq("consigneeId"))))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId), filter))
        .thenReturn(Future.successful(Seq(expectedMovement, expectedMovement2)))

      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement)
    }

    "return only the movements that correspond to the filter LRN and ern" in {
      val expectedMovement4 = Movement(Some("boxId"), lrnToFilterBy, ernToFilterBy, None, Some("arc1"))
      val filter            = MovementFilter.emptyFilter.copy(ern = Some(ernToFilterBy), lrn = Some(lrnToFilterBy))

      when(mockMovementRepository.getMovementByERN(Seq(consignorId), filter))
        .thenReturn(Future.successful(Seq(expectedMovement4)))

      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement4)
    }

    "return only the movements that correspond to the filter LRN, ern and arc" in {
      val expectedMovement4 = Movement(Some("boxId"), lrnToFilterBy, ernToFilterBy, None, Some(arcToFilterBy))
      val filter            = MovementFilter.emptyFilter.copy(
        ern = Some(ernToFilterBy),
        lrn = Some(lrnToFilterBy),
        arc = Some(arcToFilterBy)
      )

      when(mockMovementRepository.getMovementByERN(Seq(consignorId), filter))
        .thenReturn(
          Future.successful(
            Seq(expectedMovement4)
          )
        )

      val result = await(movementService.getMovementByErn(Seq(consignorId), filter))

      result mustBe Seq(expectedMovement4)
    }

    "return an empty list" in {
      when(mockMovementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(movementService.getMovementByErn(Seq(consignorId)))

      result mustBe Seq.empty
    }
  }

  "getMovementById" should {

    "return the movement for the id if it is found" in {
      val expectedMovement1 =
        Movement("uuid1", Some("boxId"), "lrn1", consignorId, None, Some("arc1"), Instant.now, Seq.empty)

      when(mockMovementRepository.getMovementById("uuid1"))
        .thenReturn(Future.successful(Some(expectedMovement1)))

      val result = await(movementService.getMovementById("uuid1"))

      result mustBe Some(expectedMovement1)
    }

    "return None if no match" in {

      when(mockMovementRepository.getMovementById("uuid2"))
        .thenReturn(Future.successful(None))

      val result = await(movementService.getMovementById("uuid2"))

      result mustBe None
    }
  }
}
