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
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterAll
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, MovementMessage}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetMessagesControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with WireMockServerSpec
  with RepositoryTestStub
  with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val lrn = "LRN00001"
  private val url = s"http://localhost:$port/customs/excise/movements/$lrn/messages"

  private val consignorId = "GBWK002281023"

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())
    GuiceApplicationBuilder()
      .configure(configureServer)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementMessageRepository].to(movementMessageRepository)
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

  "Get Messages" should {

    "return 200" in {
      withAuthorizedTrader(consignorId)

      val now = Instant.now
      when(movementMessageRepository.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq(MovementMessage("", "", None, None, now, Some(Seq(Message("", "", now)))))))

      val result = getRequest()

      result.status mustBe OK

      withClue("return the json response") {
        val responseBody = Json.parse(result.body).as[Seq[Message]]
        responseBody mustBe Seq(Message("", "", now))
      }
    }

    "return 404 when no movement message is found" in {
      withAuthorizedTrader(consignorId)

      when(movementMessageRepository.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = getRequest()

      result.status mustBe NOT_FOUND
    }

    "return 500 when mongo db fails to fetch details" in {
      withAuthorizedTrader(consignorId)

      when(movementMessageRepository.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = getRequest()

      result.status mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 when multiple movements messages are found" in {
      withAuthorizedTrader(consignorId)

      val now = Instant.now()

      val movementMessage = MovementMessage("", "", None, None, now, Some(Seq(Message("", "", now))))
      val list = Seq(movementMessage, movementMessage)
      when(movementMessageRepository.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(list))

      val result = getRequest()

      result.status mustBe INTERNAL_SERVER_ERROR
    }

    "return forbidden (403) when there are no authorized ERN" in {
      withUnAuthorizedERN()

      getRequest().status mustBe FORBIDDEN
    }

    "return a Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      getRequest().status mustBe UNAUTHORIZED
    }

  }

  private def getRequest() = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN"
      ).get()
    )
  }
}
