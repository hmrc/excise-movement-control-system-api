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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, argThat, refEq}
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.http.Status.ACCEPTED
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.Result
import play.api.mvc.Results.ImATeapot
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.InjectController.CsvRow
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ErnSubmissionRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService

import java.time.Instant
import scala.concurrent.Future

class InjectControllerSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private val mockMovementService = mock[MovementService]
  private val mockErnSubmissionRepository = mock[ErnSubmissionRepository]

  private implicit val jsonWritesCsvRow: OWrites[CsvRow] = Json.writes[CsvRow]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[MovementService].toInstance(mockMovementService),
        bind[ErnSubmissionRepository].toInstance(mockErnSubmissionRepository),
      ).build()

  override def beforeEach(): Unit = {
    reset(mockMovementService, mockErnSubmissionRepository)
    super.beforeEach()
  }


  "submit" must {
    "saveNewMovement" when {
      "there is no consigneeId" in {
        val csvrow = CsvRow(123, Some("arn"), "lrn", "consignor", 1, "status", Instant.now(), None)
        when(mockMovementService.saveNewMovement(any())).thenReturn(Future.successful(Right(csvrow.toMovement)))

        val fakeRequest = FakeRequest(routes.InjectController.submit()).withBody(Json.toJson(csvrow))

        val result: Future[Result] = route(app, fakeRequest).value
        status(result) mustBe ACCEPTED
        verify(mockMovementService).saveNewMovement(refEq(csvrow.toMovement, "_id"))
        verify(mockErnSubmissionRepository, never()).save(any())
      }
      "there is a consigneeId" in {
        val csvrow = CsvRow(123, Some("arn"), "lrn", "consignor", 1, "status", Instant.now(), Some("expectedErn"))
        when(mockMovementService.saveNewMovement(any())).thenReturn(Future.successful(Right(csvrow.toMovement)))
        when(mockErnSubmissionRepository.save(any())).thenReturn(Future.successful(Done))

        val fakeRequest = FakeRequest(routes.InjectController.submit()).withBody(Json.toJson(csvrow))

        val result: Future[Result] = route(app, fakeRequest).value
        status(result) mustBe ACCEPTED
        verify(mockMovementService).saveNewMovement(refEq(csvrow.toMovement, "_id"))
        verify(mockErnSubmissionRepository).save("expectedErn")
      }
    }
    "return error" when {
      "csv row is malformed" in {
        val fakeRequest = FakeRequest(routes.InjectController.submit()).withBody(Json.parse("""{"malformed":"json"}""""))

        val result: Future[Result] = route(app, fakeRequest).value
        status(result) mustBe BAD_REQUEST
        verify(mockMovementService, never()).saveNewMovement(any())
        verify(mockErnSubmissionRepository, never()).save(any())
      }
      "saveNewMovement fails" in {
        val csvrow = CsvRow(123, Some("arn"), "lrn", "consignor", 1, "status", Instant.now(), Some("expectedErn"))
        when(mockMovementService.saveNewMovement(any())).thenReturn(Future.successful(Left(ImATeapot)))

        val fakeRequest = FakeRequest(routes.InjectController.submit()).withBody(Json.toJson(csvrow))

        val result: Future[Result] = route(app, fakeRequest).value
        status(result) mustBe IM_A_TEAPOT
        verify(mockErnSubmissionRepository, never()).save(any())
      }
      "ernSubmissionRepository fails" in {
        object TestException extends Exception
        val csvrow = CsvRow(123, Some("arn"), "lrn", "consignor", 1, "status", Instant.now(), Some("expectedErn"))
        when(mockMovementService.saveNewMovement(any())).thenReturn(Future.successful(Right(csvrow.toMovement)))
        when(mockErnSubmissionRepository.save(any())).thenReturn(Future.failed(TestException))
        val fakeRequest = FakeRequest(routes.InjectController.submit()).withBody(Json.toJson(csvrow))

        intercept[TestException.type](await(route(app, fakeRequest).value))
      }
    }
  }

}