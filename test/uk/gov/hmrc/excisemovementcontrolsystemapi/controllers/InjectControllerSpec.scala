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

import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.InjectController.CsvRow
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class InjectControllerSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private val mockMovementRepository = mock[MovementRepository]
  private val mockStubBehaviour      = mock[StubBehaviour]

  private implicit val jsonWritesCsvRow: OWrites[CsvRow] = Json.writes[CsvRow]

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
    reset(mockMovementRepository, mockStubBehaviour)
    super.beforeEach()
  }

  private val expectedPredicate = Predicate.Permission(
    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("inject/submit")),
    IAAction("ADMIN")
  )

  "submitBatch" must {
    "saveMovements" in {
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
        .thenReturn(Future.successful(()))

      val csvrow = CsvRow(123, Some("arn"), "lrn", "consignor", 1, "status", Instant.now(), None)
      when(mockMovementRepository.saveMovements(any())).thenReturn(Future.successful(true))

      val fakeRequest = FakeRequest(routes.InjectController.submitBatch())
        .withBody(Json.toJson(List(csvrow, csvrow)))
        .withHeaders("Authorization" -> "Token some-token")

      val result      = route(app, fakeRequest).value
      status(result) mustBe ACCEPTED
      verify(mockMovementRepository, times(1)).saveMovements(any())
      verify(mockStubBehaviour).stubAuth(any(), any())
    }

    "return error" when {
      "no header" in {
        val fakeRequest =
          FakeRequest(routes.InjectController.submitBatch()).withBody(Json.parse("""{"some":"json"}""""))

        val ex = intercept[UpstreamErrorResponse](await(route(app, fakeRequest).value))
        ex.message mustBe "Unauthorized"
      }

      "unauthorised" in {
        object TestException extends Exception
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
          .thenReturn(Future.failed(TestException))
        val fakeRequest = FakeRequest(routes.InjectController.submitBatch())
          .withBody(Json.parse("""{"some":"json"}""""))
          .withHeaders("Authorization" -> "Token some-token")

        intercept[TestException.type](await(route(app, fakeRequest).value))
      }

      "csv row is malformed" in {
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval))
          .thenReturn(Future.successful(()))
        val fakeRequest            =
          FakeRequest(routes.InjectController.submitBatch())
            .withBody(Json.parse("""{"malformed":"json"}""""))
            .withHeaders("Authorization" -> "Token some-token")

        val result: Future[Result] = route(app, fakeRequest).value
        status(result) mustBe BAD_REQUEST
        verify(mockMovementRepository, never()).saveMovements(any())
      }
    }
  }

}
