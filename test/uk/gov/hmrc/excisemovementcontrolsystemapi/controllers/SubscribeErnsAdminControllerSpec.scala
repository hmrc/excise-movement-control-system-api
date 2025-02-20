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
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.SubscribeErnsAdminController.SubscribeErnsRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.{ExecutionContext, Future}

class SubscribeErnsAdminControllerSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private val mockStubBehaviour        = mock[StubBehaviour]
  private val mockNotificationsService = mock[NotificationsService]

  val backendAuthComponentsStub: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents(), ExecutionContext.global)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[BackendAuthComponents].toInstance(backendAuthComponentsStub),
        bind[NotificationsService].toInstance(mockNotificationsService)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNotificationsService, mockStubBehaviour)
  }

  private val expectedPredicate = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("admin/notifications/subscribe")),
    IAAction("ADMIN")
  )

  "subscribeErns" must {
    "subscribe the given erns to the clientId" in {

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
        .thenReturn(Future.successful(()))

      when(mockNotificationsService.subscribeErns(any, any)(any)).thenReturn(Future.successful("boxId"))

      val fakeRequest = FakeRequest(routes.SubscribeErnsAdminController.subscribeErns())
        .withBody(Json.toJson(SubscribeErnsRequest("clientId", Set("ern1"))))
        .withHeaders("Authorization" -> "Token some-token")

      val result      = route(app, fakeRequest).value
      status(result) mustBe OK
      contentAsString(result) mustBe "boxId"
      verify(mockStubBehaviour).stubAuth(any(), any())
      verify(mockNotificationsService).subscribeErns(eqTo("clientId"), eqTo(Seq("ern1")))(any)
    }

    "return error" when {

      "no header" in {
        val fakeRequest =
          FakeRequest(routes.SubscribeErnsAdminController.subscribeErns())
            .withBody(Json.obj())

        val result = route(app, fakeRequest).value.failed.futureValue
        result mustBe a[UpstreamErrorResponse]
      }

      "unauthorised" in {

        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
          .thenReturn(Future.failed(new RuntimeException("message")))

        val fakeRequest = FakeRequest(routes.SubscribeErnsAdminController.subscribeErns())
          .withBody(Json.parse("""{"some":"json"}""""))
          .withHeaders("Authorization" -> "Token some-token")

        val result      = route(app, fakeRequest).value.failed.futureValue
        result.getMessage mustEqual "message"

        verify(mockNotificationsService, times(0)).subscribeErns(any, any)(any)
      }

      "request body is malformed" in {
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
          .thenReturn(Future.successful(()))
        val fakeRequest            =
          FakeRequest(routes.SubscribeErnsAdminController.subscribeErns())
            .withBody(Json.obj())
            .withHeaders("Authorization" -> "Token some-token")

        val result: Future[Result] = route(app, fakeRequest).value
        status(result) mustBe BAD_REQUEST

        verify(mockNotificationsService, times(0)).subscribeErns(any, any)(any)
      }
    }
  }
}
