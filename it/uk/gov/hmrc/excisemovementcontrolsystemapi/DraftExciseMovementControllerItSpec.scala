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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, ok, post, urlEqualTo}
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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ExciseMovementResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq


class DraftExciseMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with WireMockServerSpec
  with RepositoryTestStub
  with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val url = s"http://localhost:$port/customs/excise/movements"
  private val eisUrl = "/emcs/digital-submit-new-message/v1"
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

  "Draft Excise Movement" should {

    "return 202" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()
      when(movementMessageRepository.saveMovementMessage(any))
        .thenReturn(Future.successful(true))

      val result = postRequest(IE815)

      result.status mustBe ACCEPTED

      withClue("return the json response") {
        val responseBody = Json.parse(result.body).as[ExciseMovementResponse]
        responseBody mustBe ExciseMovementResponse("Accepted", "LRNQA20230909022221", consignorId)
      }
    }

    "return not found if EIS return not found" in {
      withAuthorizedTrader("GBWK002281023")
      val eisErrorResponse = createEISErrorResponseBodyAsJson("NOT_FOUND")
      stubEISErrorResponse(NOT_FOUND, eisErrorResponse.toString())

      val result = postRequest(IE815)

      result.status mustBe NOT_FOUND

      withClue("return the EIS error response") {
        result.json mustBe Json.toJson(eisErrorResponse)
      }
    }

    "return bad request if EIS return BAD_REQUEST" in {
      withAuthorizedTrader("GBWK002281023")
      stubEISErrorResponse(BAD_REQUEST, createEISErrorResponseBodyAsJson("BAD_REQUEST").toString())

      postRequest(IE815).status mustBe BAD_REQUEST
    }

    "return 500 if EIS return 500" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(INTERNAL_SERVER_ERROR, createEISErrorResponseBodyAsJson("INTERNAL_SERVER_ERROR").toString())

      postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 if EIS return bad json" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(INTERNAL_SERVER_ERROR, """"{"json": "is-bad"}""")

      postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
    }

    "return forbidden (403) when there are no authorized ERN" in {
      withUnAuthorizedERN()

      postRequest(IE815).status mustBe FORBIDDEN
    }

    "return a Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      postRequest(IE815).status mustBe UNAUTHORIZED
    }

    "return bad request (400) when xml cannot be parsed" in {
      withAuthorizedTrader("GBWK002281023")

      postRequest(<IE815></IE815>).status mustBe BAD_REQUEST
    }

    "return Unsupported Media Type (415)" in {
      withAuthorizedTrader("GBWK002281023")
      postRequest(contentType = """application/json""").status mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "return bad request (400) when body is not xml" in {
      withAuthorizedTrader("GBWK002281023")

      val result = await(wsClient.url(url)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN",
          HeaderNames.CONTENT_TYPE -> """application/vnd.hmrc.1.0+xml"""
        ).post("test")
      )

      result.status mustBe BAD_REQUEST
    }

    "return forbidden (403) when consignor id cannot be validate" in {
      withAuthorizedTrader("123")

      postRequest(IE815).status mustBe FORBIDDEN
    }
  }

  private def createEISErrorResponseBodyAsJson(message: String): JsValue = {
    Json.toJson(EISErrorResponse(
      LocalDateTime.of(2023, 12, 5, 12, 5, 6),
      message,
      s"debug $message",
      "123"
    ))
  }

  private def postRequest(xml: NodeSeq = IE815, contentType: String = """application/vnd.hmrc.1.0+xml""") = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> contentType
      ).post(xml)
    )
  }

  private def stubEISSuccessfulRequest() = {

    val response = EISResponse("OK", "message", "123")
    wireMock.stubFor(
      post(eisUrl)
        .willReturn(ok().withBody(Json.toJson(response).toString()))
    )
  }

  private def stubEISErrorResponse(status: Int, body: String): Any = {

    wireMock.stubFor(
      post(urlEqualTo(eisUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
            .withHeader("Content-Type", "application/json")
        )
    )
  }
}
