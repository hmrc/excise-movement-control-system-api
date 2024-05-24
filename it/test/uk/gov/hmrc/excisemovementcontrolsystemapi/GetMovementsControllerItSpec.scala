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

package uk.gov.hmrc.excisemovementcontrolsystemapi

import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.MovementTestUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.ApplicationBuilderSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with ApplicationBuilderSupport
  with MovementTestUtils
  with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val consignorId = "GBWK002281023"
  private val consigneeId = "GBWK002281027"
  private val lrn = "token"
  private val baseUrl = s"http://localhost:$port/movements"
  private val timestampNow = Instant.now()
  private val timestampTwoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS)

  private val movement1 = Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), Some("arc1"), timestampNow)
  private val movement2 = Movement(Some("boxId"), "lrn1", consignorId, Some("consignee2"), Some("arc2"), timestampTwoDaysAgo)
  private val movement3 = Movement(Some("boxId"), "lrn2", "ern2", Some(consigneeId), Some("arc3"), timestampTwoDaysAgo)

  override lazy val app: Application = applicationBuilder.build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(dateTimeService)

    when(dateTimeService.timestamp()).thenReturn(Instant.now)
  }

  "Get Movements" should {
    "return 200 and movements when logged in as consignor" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2)))

      val result = getRequest(baseUrl)

      result.status mustBe OK
      withClue("return an EIS response") {
        result.json mustBe Json.toJson(Seq(
          createMovementResponseFromMovement(movement1),
          createMovementResponseFromMovement(movement2)
        ))
      }
    }

    "get filtered movement by ERN" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$baseUrl?ern=$consignorId")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1),
        createMovementResponseFromMovement(movement2)
      ))
    }

    "get filtered movement by ERN when you are the consignee" in {
      withAuthorizedTrader(consigneeId)
      when(movementRepository.getMovementByERN(Seq(consigneeId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$baseUrl?ern=$consigneeId")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1),
        createMovementResponseFromMovement(movement3)
      ))
    }

    "get filtered movement by LRN" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$baseUrl?lrn=$lrn")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "get filtered movement by arc" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$baseUrl?arc=arc1")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "get filtered movement by updatedSince" in {
      val timeFilter = Instant.now().minus(1, ChronoUnit.DAYS)

      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val urlToGoIn = s"$baseUrl?updatedSince=$timeFilter"

      val result = getRequest(urlToGoIn)

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "get filtered movement by ern, lrn and arc" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$baseUrl?arc=arc1&lrn=$lrn&ern=$consignorId")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "return an Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      getRequest(baseUrl).status mustBe UNAUTHORIZED
    }

    "return a Bad Request (400) when not logged in as the filtering trader" in {

      withAuthorizedTrader(consignorId)

      getRequest(s"$baseUrl?ern=GBWK002281024").status mustBe BAD_REQUEST

    }
  }

  "Get Movement" should {

    val movementId = "cfdb20c7-d0b0-4b8b-a071-737d68dede5b"
    val movement = Movement(
      movementId,
      Some("boxId"),
      "LRNQA20230909022221",
      consignorId,
      Some(consigneeId),
      Some("23GB00000000000377161"),
      timestampNow,
      Seq.empty
    )

    "return 200 Success and movement details when consignor" in {

      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader(consignorId)

      val result = getRequest(getUrl(movementId))

      result.status mustBe OK

      result.json mustBe Json.toJson(createMovementResponseFromMovement(movement))
    }

    "return 200 Success and movement details when consignee" in {

      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader(consigneeId)

      val result = getRequest(getUrl(movementId))

      result.status mustBe OK
      result.json mustBe Json.toJson(createMovementResponseFromMovement(movement))

    }

    "return 401 Unauthorised when not authorised trader" in {

      withUnauthorizedTrader(InternalError("A general auth failure"))

      val result = getRequest(getUrl(movementId))

      result.status mustBe UNAUTHORIZED
    }

    "return 404 Not Found when movement is not in database" in {
      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(None))

      withAuthorizedTrader(consigneeId)

      val result = getRequest(getUrl(movementId))

      result.status mustBe NOT_FOUND

    }

    "return 404 Not Found when movement is not valid for your login" in {
      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader("differentPerson")

      val result = getRequest(getUrl(movementId))

      result.status mustBe NOT_FOUND

    }

    "return 400 Bad Request when UUID is wrong format" in {
      withAuthorizedTrader(consignorId)

      val result = getRequest(getUrl("nfbfs78-432nfsd-4123"))

      result.status mustBe BAD_REQUEST

    }

    def getUrl(movementId: String) = s"$baseUrl/$movementId"

  }

  private def getRequest(url: String) = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN"
      ).get()
    )
  }
}
