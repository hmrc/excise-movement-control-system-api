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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mongodb.scala.MongoException
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.AnyContent
import play.api.mvc.Results.{BadRequest, NotFound}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.{MovementIdFormatInvalid, MovementIdNotFound, MovementIdValidation}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetMessagesControllerSpec extends PlaySpec
  with FakeAuthentication
  with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val movementService = mock[MovementService]
  private val movementIdValidator = mock[MovementIdValidation]
  private val cc = stubControllerComponents()
  private val validUUID = "cfdb20c7-d0b0-4b8b-a071-737d68dede5e"
  private val dateTimeService = mock[DateTimeService]
  private val timeStamp = Instant.parse("2020-01-01T01:01:01.1Z")
  private val workItemService = mock[WorkItemService]
  private val messageCreateOn = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, dateTimeService, workItemService, movementIdValidator)

    when(movementService.getMatchingERN(any, any))
      .thenReturn(Future.successful(Some(ern)))

    when(dateTimeService.timestamp()).thenReturn(timeStamp)

    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))
  }

  "getMessagesForMovement" should {
    "respond with BAD_REQUEST when the MovementID is an invalid UUID" in {
      val expectedError = Json.toJson(
        ErrorResponse(timeStamp, "Movement Id format error",
          s"Movement Id should be a valid UUID")
      )

      when(movementIdValidator.validateMovementId(eqTo("invalidUUID")))
        .thenReturn(Future.successful(Left(MovementIdFormatInvalid())))
      when(movementIdValidator.convertErrorToResponse(eqTo(MovementIdFormatInvalid()), eqTo(timeStamp))).thenReturn(
        BadRequest(expectedError)
      )

      val result = createWithSuccessfulAuth.getMessagesForMovement("invalidUUID", None)(createRequest())

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe expectedError
    }

    "return 200" in {
      val message = Message("message", "IE801", "messageId", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "get all the new messages" in {
      val message = Message("message", "IE801", "messageId1", messageCreateOn)
      val message2 = Message("message2", "IE801", "messageId2", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message, message2))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message, message2))

      verify(movementIdValidator).validateMovementId(eqTo(validUUID))
    }

    "get all the new messages when there is a time query parameter provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", "messageId1", timeInFuture)
      val message2 = Message("message2", "IE801", "messageId2", timeInPast)
      val movement = createMovementWithMessages(Seq(message, message2))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some(messageCreateOn.toString))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "get all the new messages including messages with a createdOn time of NOW when there is a time query parameter provided" in {
      val timeNowString = messageCreateOn.toString
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", "messageId1", timeInFuture)
      val message2 = Message("message2", "IE801", "messageId2", timeInPast)
      val message3 = Message("message3", "IE801", "messageId3", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message, message2, message3))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some(timeNowString))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message, message3))
    }

    "succeed when a valid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", "messageId", timeInFuture)
      val movement = createMovementWithMessages(Seq(message))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some("2020-11-15T17:02:34.00Z"))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "fail when an invalid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", "messageId", timeInFuture)
      val movement = createMovementWithMessages(Seq(message))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, Some("invalid date"))(createRequest())

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe Json.toJson(
        ErrorResponse(timeStamp, "Invalid date format provided in the updatedSince query parameter", "Date format should be like '2020-11-15T17:02:34.00Z'")
      )
    }

    "create a Work Item if there is not one for the ERN already" in {
      val message = Message("message", "IE801", "messageId", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      await(createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest()))

      verify(workItemService).addWorkItemForErn(eqTo("testErn"), eqTo(false))

    }

    "return an empty array when no messages" in {
      val movement = createMovementWithMessages(Seq.empty)
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe JsArray()
    }

    "return NOT_FOUND when no movement exists for given movementID" in {

      val expectedError = Json.toJson(
        ErrorResponse(timeStamp, "No movement found for the MovementID provided",
          s"MovementID $validUUID was not found in the database")
      )

      when(movementIdValidator.validateMovementId(eqTo(validUUID)))
        .thenReturn(Future.successful(Left(MovementIdNotFound(validUUID))))
      when(movementIdValidator.convertErrorToResponse(eqTo(MovementIdNotFound(validUUID)), eqTo(timeStamp))).thenReturn(
        NotFound(expectedError)
      )

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe expectedError
    }

    "return NOT_FOUND when movement is for a different ern " in {
      val message = Message("message", "IE801", "messageId", Instant.now)
      val movement = Movement(validUUID, "lrn", "boxId", "consignor", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson(
        ErrorResponse(timeStamp, "Invalid MovementID supplied for ERN",
          s"Movement $validUUID is not found within the data for ERNs testErn")
      )
    }

    "catch Future failure from Work Item service and log it but still process submission" in {
      val message = Message("message", "IE801", "messageId", messageCreateOn)
      val movement = createMovementWithMessages(Seq(message))
      when(movementIdValidator.validateMovementId(any))
        .thenReturn(Future.successful(Right(movement)))

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.getMessagesForMovement(validUUID, None)(createRequest())

      status(result) mustBe OK
    }

  }

  private def createMovementWithMessages(messages: Seq[Message]): Movement = {
    Movement(validUUID, "boxId", "lrn", "testErn", Some("consigneeId"), Some("arc"), Instant.now, messages)
  }

  private def createWithSuccessfulAuth =
    new GetMessagesController(
      FakeSuccessAuthentication,
      movementService,
      workItemService,
      movementIdValidator,
      cc,
      dateTimeService
    )

  private def createRequest(): FakeRequest[AnyContent] = {
    FakeRequest("GET", "/foo")
  }
}
