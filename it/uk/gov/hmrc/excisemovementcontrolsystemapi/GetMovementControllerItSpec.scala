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

package uk.gov.hmrc.excisemovementcontrolsystemapi

import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{AuthTestSupport, MovementTestUtils}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.RepositoryTestStub
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.Future

class GetMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with RepositoryTestStub
  with MovementTestUtils {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port/movements"

  private lazy val dateTimeService: DateTimeService = mock[DateTimeService]
  private val timestamp = Instant.now
  when(dateTimeService.timestamp()).thenReturn(timestamp)

  private val consignorId = "GBWK240176600"
  private val consigneeId = "GBWK002281023"
  private val movementId = "cfdb20c7-d0b0-4b8b-a071-737d68dede5b"
  private val movement = Movement(
    movementId,
    "LRNQA20230909022221",
    consignorId,
    Some(consigneeId),
    Some("23GB00000000000377161"),
    timestamp,
    Seq.empty
  )

  override lazy val app: Application = {

    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementRepository].to(movementRepository),
        bind[DateTimeService].to(dateTimeService)
      )
      .build()
  }


  "Get Movement" should {

    "return 200 Success and movement details when consignor" in {

      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader(consignorId)

      val result = getRequest(movementId)

      result.status mustBe OK

      result.json mustBe Json.toJson(createMovementResponseFromMovement(movement))
    }

    "return 200 Success and movement details when consignee" in {

      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader(consigneeId)

      val result = getRequest(movementId)

      result.status mustBe OK
      result.json mustBe Json.toJson(createMovementResponseFromMovement(movement))

    }

    "return 401 Unauthorised when not authorised trader" in {

      withUnauthorizedTrader(InternalError("A general auth failure"))

      val result = getRequest(movementId)

      result.status mustBe UNAUTHORIZED
    }

    "return 404 Not Found when movement is not in database" in {
      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(None))

      withAuthorizedTrader(consigneeId)

      val result = getRequest(movementId)

      result.status mustBe NOT_FOUND

    }

    "return 404 Not Found when movement is not valid for your login" in {
      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader("differentPerson")

      val result = getRequest(movementId)

      result.status mustBe NOT_FOUND

    }

    "return 400 Bad Request when UUID is wrong format" in {
      withAuthorizedTrader(consignorId)

      val result = getRequest("nfbfs78-432nfsd-4123")

      result.status mustBe BAD_REQUEST

    }

  }

  private def getRequest(movementId: String) = {
    await(wsClient.url(s"$baseUrl/$movementId")
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN"
      ).get()
    )
  }
}
