/*
 * Copyright 2025 HM Revenue & Customs
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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.RemoveErnsAdminController.{ErnSummary, RemoveErnsRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ErnRetrievalRepository, ErnSubmissionRepository, MovementRepository}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RemoveErnsAdminControllerSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private val mockStubBehaviour       = mock[StubBehaviour]
  private val mockRepository          = mock[ErnSubmissionRepository]
  private val mockBoxIdRepository     = mock[BoxIdRepository]
  private val mockMovementRepository  = mock[MovementRepository]
  private val mockRetrievalRepository = mock[ErnRetrievalRepository]

  val backendAuthComponentsStub: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents(), ExecutionContext.global)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[BackendAuthComponents].toInstance(backendAuthComponentsStub),
        bind[ErnSubmissionRepository].toInstance(mockRepository),
        bind[BoxIdRepository].toInstance(mockBoxIdRepository),
        bind[MovementRepository].toInstance(mockMovementRepository),
        bind[ErnRetrievalRepository].toInstance(mockRetrievalRepository)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository, mockStubBehaviour)
  }

  private val expectedPredicate = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("admin/erns")),
    IAAction("ADMIN")
  )

  "removeErns" must {
    "remove the given erns" in {

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
        .thenReturn(Future.successful(()))

      when(mockRepository.removeErns(any)).thenReturn(Future.successful(Done))

      val fakeRequest = FakeRequest(routes.RemoveErnsAdminController.removeErns())
        .withBody(Json.toJson(RemoveErnsRequest(Set("ern1", "ern2"))))
        .withHeaders("Authorization" -> "Token some-token")

      val result      = route(app, fakeRequest).value
      status(result) mustBe NO_CONTENT
      verify(mockStubBehaviour).stubAuth(any(), any())
      verify(mockRepository).removeErns(eqTo(Seq("ern1", "ern2")))
    }

    "return error" when {

      "no header" in {
        val fakeRequest =
          FakeRequest(routes.RemoveErnsAdminController.removeErns())
            .withBody(Json.obj())

        val result = route(app, fakeRequest).value.failed.futureValue
        result mustBe a[UpstreamErrorResponse]
      }

      "unauthorised" in {

        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
          .thenReturn(Future.failed(new RuntimeException("message")))

        val fakeRequest = FakeRequest(routes.RemoveErnsAdminController.removeErns())
          .withBody(Json.parse("""{"some":"json"}""""))
          .withHeaders("Authorization" -> "Token some-token")

        val result      = route(app, fakeRequest).value.failed.futureValue
        result.getMessage mustEqual "message"

        verify(mockRepository, times(0)).removeErns(any)
      }

      "request body is malformed" in {
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
          .thenReturn(Future.successful(()))
        val fakeRequest            =
          FakeRequest(routes.RemoveErnsAdminController.removeErns())
            .withBody(Json.obj())
            .withHeaders("Authorization" -> "Token some-token")

        val result: Future[Result] = route(app, fakeRequest).value
        status(result) mustBe BAD_REQUEST

        verify(mockRepository, times(0)).removeErns(any)
      }
    }
  }

  "findAllReferencesToErn" must {
    "Return info filtered from the repositories" in {

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
        .thenReturn(Future.successful(()))

      val testErn   = "apples"
      val timestamp = Instant.now()

      val consignorMovement = Movement("id_1", None, "lrn", testErn, None, None, timestamp, Seq.empty)
      val consigneeMovement = Movement("id_2", None, "lrn", "consignor", Some(testErn), None, timestamp, Seq.empty)
      val recipientMovement = Movement(
        "id_3",
        None,
        "lrn",
        "consignor",
        None,
        None,
        timestamp,
        Seq(Message("messageCode", "IE801", "message_id", testErn, Set.empty, timestamp))
      )

      when(mockBoxIdRepository.getBoxIds(any)).thenReturn(Future.successful(Set("boxId1")))
      when(mockMovementRepository.getMovementByERN(any, any))
        .thenReturn(Future.successful(Seq(consigneeMovement, consignorMovement, recipientMovement)))
      when(mockRepository.findErns(any)).thenReturn(Future.successful(Seq(testErn)))
      when(mockRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))

      val fakeRequest            =
        FakeRequest(routes.RemoveErnsAdminController.findAllReferencesToErn(testErn))
          .withHeaders("Authorization" -> "Token some-token")

      val result: Future[Result] = route(app, fakeRequest).value

      contentAsJson(result) mustBe Json.toJson(
        ErnSummary(
          testErn,
          Seq(consignorMovement._id),
          Seq(consigneeMovement._id),
          Seq((recipientMovement._id, Seq(recipientMovement.messages.head.messageId))),
          Seq(testErn),
          Option.empty[Instant],
          Set("boxId1")
        )
      )
    }
  }
}
