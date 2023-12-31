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

import com.github.tomakehurst.wiremock.client.WireMock
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{AuthTestSupport, MovementTestUtils}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.mongo.TimestampSupport

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with WireMockServerSpec
  with RepositoryTestStub
  with MovementTestUtils
  with BeforeAndAfterAll {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val consignorId = "GBWK002281023"
  private val consigneeId = "GBWK002281027"
  private val lrn = "token"
  private val url = s"http://localhost:$port/movements"
  private lazy val dateTimeService: TimestampSupport = mock[TimestampSupport]
  private val timestampNow = LocalDateTime.now().toInstant(ZoneOffset.UTC)
  private val timestampTwoDaysAgo = LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC)

  private val movement1 = Movement(lrn, consignorId, Some(consigneeId), Some("arc1"), timestampNow)
  private val movement2 = Movement("lrn1", consignorId, Some("consignee2"), Some("arc2"), timestampTwoDaysAgo)
  private val movement3 = Movement("lrn2", "ern2", Some(consigneeId), Some("arc3"), timestampTwoDaysAgo)

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServer)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementRepository].to(movementRepository),
        bind[TimestampSupport].to(dateTimeService)
      )
      .build()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Get Movements" should {
    "return 200 and movements when logged in as consignor" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2)))

      val result = getRequest(url)

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

      val result = getRequest(s"$url?ern=$consignorId")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1),
        createMovementResponseFromMovement(movement2)
      ))
    }

    "get filtered movement by ERN when you are the consignee" in {
      withAuthorizedTrader(consigneeId)
      when(movementRepository.getMovementByERN(Seq(consigneeId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$url?ern=$consigneeId")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1),
        createMovementResponseFromMovement(movement3)
      ))
    }

    "get filtered movement by LRN" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$url?lrn=$lrn")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "get filtered movement by arc" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$url?arc=arc1")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "get filtered movement by updatedSince" in {
      val timeFilter = LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC)

      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val urlToGoIn = s"$url?updatedSince=$timeFilter"

      val result = getRequest(urlToGoIn)

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "get filtered movement by ern, lrn and arc" in {
      withAuthorizedTrader(consignorId)
      when(movementRepository.getMovementByERN(Seq(consignorId)))
        .thenReturn(Future.successful(Seq(movement1, movement2, movement3)))

      val result = getRequest(s"$url?arc=arc1&lrn=$lrn&ern=$consignorId")

      result.json mustBe Json.toJson(Seq(
        createMovementResponseFromMovement(movement1)
      ))
    }

    "return an Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      getRequest(url).status mustBe UNAUTHORIZED
    }

    "return a Bad Request (400) when not logged in as the filtering trader" in {

      withAuthorizedTrader(consignorId)

      getRequest(s"$url?ern=GBWK002281024").status mustBe BAD_REQUEST

    }
  }

  private def getRequest(url: String) = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN"
      ).get()
    )
  }
}
