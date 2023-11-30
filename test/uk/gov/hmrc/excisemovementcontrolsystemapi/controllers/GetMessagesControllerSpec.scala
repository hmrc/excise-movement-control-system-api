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
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, OK}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateErnsAction, FakeXmlParsers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumberWorkItem, Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, WorkItemService}
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetMessagesControllerSpec extends PlaySpec
  with FakeAuthentication
  with FakeXmlParsers
  with FakeValidateErnsAction
  with TestXml
  with EitherValues
  with BeforeAndAfterEach
  with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val movementService = mock[MovementService]
  private val cc = stubControllerComponents()
  private val lrn = "LRN1234"
  private val dateTimeService = mock[TimestampSupport]
  private val timeStamp = Instant.parse("2018-11-30T18:35:24.00Z")
  private val workItemService = mock[WorkItemService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, dateTimeService, workItemService)

    when(movementService.getMatchingERN(any, any))
      .thenReturn(Future.successful(Some(ern)))

    when(dateTimeService.timestamp()).thenReturn(timeStamp)

    when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.successful(mock[WorkItem[ExciseNumberWorkItem]]))
  }

  "getMessagesForMovement" should {
    "return 200" in {
      val message = Message("message","IE801", dateTimeService)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any,any))
        .thenReturn(Future.successful(Some(movement)))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(message))
    }

    "get all the new messages" in {
      val message = Message("message","IE801", dateTimeService)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any,any))
        .thenReturn(Future.successful(Some(movement)))

        await(createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest()))

        verify(movementService).getMovementByLRNAndERNIn(eqTo(lrn), eqTo(List(ern)))
      }

    "create a Work Item if there is not one for the ERN already" in {
      val message = Message("message", "IE801", dateTimeService)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      await(createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest()))

      verify(workItemService).addWorkItemForErn(eqTo("testErn"), eqTo(false))

    }

    "return an empty array when no messages" in {
      when(movementService.getMovementByLRNAndERNIn(any,any))
        .thenReturn(Future.successful(None))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe JsArray()
    }

    "return a bad request when no movement exists for LRN/ERNs combination" in {
      when(movementService.getMatchingERN(any, any)).thenReturn(Future.successful(None))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe BAD_REQUEST
    }

    "catch the error thrown if Work Item service fails and continue" in {
      val message = Message("message", "IE801", dateTimeService)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      when(workItemService.addWorkItemForErn(any, any)).thenThrow(new RuntimeException("error"))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe OK

      verify(movementService).getMatchingERN(any,any)

    }

    "catch Future failure from Work Item service and log it but still process submission" in {
      val message = Message("message", "IE801", dateTimeService)
      val movement = Movement("lrn", "consignorId", Some("consigneeId"), Some("arc"), Instant.now, Seq(message))
      when(movementService.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(movement)))

      when(workItemService.addWorkItemForErn(any, any)).thenReturn(Future.failed(new MongoException("Oh no!")))

      val result = createWithSuccessfulAuth.getMessagesForMovement(lrn)(createRequest())

      status(result) mustBe OK

      verify(movementService).getMatchingERN(any, any)
    }

  }

  private def createWithSuccessfulAuth =
    new GetMessagesController(
      FakeSuccessAuthentication,
      movementService,
      workItemService,
      cc
    )

  private def createRequest(): FakeRequest[AnyContent] = {
    FakeRequest("GET", "/foo")
  }
}
