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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AdminDetailsControllerSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private val mockStubBehaviour      = mock[StubBehaviour]
  private val mockMovementRepository = mock[MovementRepository]

  val backendAuthComponentsStub: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents(), ExecutionContext.global)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[BackendAuthComponents].toInstance(backendAuthComponentsStub),
        bind[MovementRepository].toInstance(mockMovementRepository)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMovementRepository, mockStubBehaviour)
  }

  private val expectedPredicate = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("admin/movement")),
    IAAction("ADMIN")
  )

  "getMovementDetails" must {
    "return OK with a json payload of movement details as expected for an existing id" in {
      val testErn   = "apples"
      val timestamp = Instant.now()

      val movement = Movement(
        "testId",
        None,
        "lrn",
        testErn,
        Some(testErn),
        None,
        timestamp,
        Seq(Message("messageCode", "IE801", "message_id", testErn, Set.empty, timestamp))
      )

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
        .thenReturn(Future.successful(()))

      when(mockMovementRepository.getMovementById(any))
        .thenReturn(Future.successful(Some(movement)))

      val expectedResult = MovementDetails.createFromMovement(movement)

      val fakeRequest            =
        FakeRequest(routes.AdminDetailsController.getMovementDetails("testId"))
          .withHeaders("Authorization" -> "Token some-token")

      val result: Future[Result] = route(app, fakeRequest).value

      contentAsJson(result) mustBe Json.toJson(expectedResult)
      status(result) mustBe OK
    }

    "return NotFound for the movementId" in {

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
        .thenReturn(Future.successful(()))

      when(mockMovementRepository.getMovementById(any))
        .thenReturn(Future.successful(None))

      val fakeRequest            =
        FakeRequest(routes.AdminDetailsController.getMovementDetails("testId"))
          .withHeaders("Authorization" -> "Token some-token")

      val result: Future[Result] = route(app, fakeRequest).value

      contentAsString(result) mustBe s"No Movement Found with id: testId"
      status(result) mustBe NOT_FOUND
    }
  }

}
