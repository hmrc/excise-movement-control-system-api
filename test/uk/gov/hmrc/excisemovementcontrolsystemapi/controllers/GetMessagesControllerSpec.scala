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
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
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
  private val cc = stubControllerComponents()
  private val lrn = "LRN1234"
  private val dateTimeService = mock[DateTimeService]
  private val timeStamp = Instant.parse("2020-01-01T01:01:01.1Z")
  private val workItemService = mock[WorkItemService]
  private val messageCreateOn = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, dateTimeService, workItemService)

    when(movementService.getMatchingERN(any, any))
      .thenReturn(Future.successful(Some(ern)))

    when(dateTimeService.timestamp()).thenReturn(timeStamp)

    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(true))
  }

  "getMessagesForMovement" should {
    "return 200" in {
      val message = Message("message", "IE801", messageCreateOn)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "get all the new messages" in {
      val message = Message("message", "IE801", messageCreateOn)
      val message2 = Message("message2", "IE801", messageCreateOn)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message, message2))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message, message2))
      verify(movementService).getMovementByLRNAndERNIn(eqTo(lrn), eqTo(List(ern)))
    }

    "get all the new messages when there is a time query parameter provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", timeInFuture)
      val message2 = Message("message2", "IE801", timeInPast)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message, message2))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, Some(messageCreateOn.toString))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "get all the new messages including messages with a createdOn time of NOW when there is a time query parameter provided" in {
      val timeNowString = messageCreateOn.toString
      val timeInFuture = Instant.now.plusSeconds(1000)
      val timeInPast = Instant.now.minusSeconds(1000)
      val message = Message("message", "IE801", timeInFuture)
      val message2 = Message("message2", "IE801", timeInPast)
      val message3 = Message("message3", "IE801", messageCreateOn)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message, message2, message3))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, Some(timeNowString))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message, message3))
    }

    "succeed when a valid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", timeInFuture)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, Some("2020-11-15T17:02:34.00Z"))(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "fail when an invalid date format is provided" in {
      val timeInFuture = Instant.now.plusSeconds(1000)
      val message = Message("message", "IE801", timeInFuture)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, Some("invalid date"))(createRequest())

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe Json.toJson(
        ErrorResponse(timeStamp, "Invalid date format provided in the updatedSince query parameter", "Date format should be like '2020-11-15T17:02:34.00Z'")
      )
    }

    "create a Work Item if there is not one for the ERN already" in {
      val message = Message("message", "IE801", messageCreateOn)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      await(createWithSuccessfulAuth.getMessagesForMovement(lrn, None)(createRequest()))

      verify(workItemService).addWorkItemForErn(eqTo("testErn"), eqTo(false))

    }

    "return an empty array when no messages" in {
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(None))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, None)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe JsArray()
    }

    "return a bad request when no movement exists for LRN/ERNs combination" in {
      when(movementService.getMatchingERN(any, any)).thenReturn(Future.successful(None))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, None)(createRequest())

      status(result) mustBe BAD_REQUEST
    }

    "catch Future failure from Work Item service and log it but still process submission" in {
      val message = Message("message", "IE801", messageCreateOn)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn, None)(createRequest())

      status(result) mustBe OK

      verify(movementService).getMatchingERN(any, any)
    }

  }

  private def createWithSuccessfulAuth =
    new GetMessagesController(
      FakeSuccessAuthentication,
      movementService,
      workItemService,
      cc,
      dateTimeService
    )

  private def createRequest(): FakeRequest[AnyContent] = {
    FakeRequest("GET", "/foo")
  }
}
