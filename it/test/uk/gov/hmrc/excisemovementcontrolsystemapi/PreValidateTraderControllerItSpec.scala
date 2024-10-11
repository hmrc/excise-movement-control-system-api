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
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{ApplicationBuilderSupport, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisErrorResponsePresentation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.PreValidateTraderMessageResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderRequest, getPreValidateTraderSuccessEISResponse, getPreValidateTraderSuccessResponse}

import java.time.Instant
import java.util.UUID


class PreValidateTraderControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with ApplicationBuilderSupport
  with WireMockServerSpec
  with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val url = s"http://localhost:$port/traders/pre-validate"
  private val eisUrl = "/emcs/pre-validate-trader/v1"
  private val authErn = "GBWK002281023"
  private val timestamp = Instant.parse("2024-06-06T12:30:12.12345678Z")

  private val request = Json.toJson(getPreValidateTraderRequest)

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())
    applicationBuilder(configureEisService).configure(
      "featureFlags.etdsPreValidateTraderEnabled" -> false,
    ).build()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Draft Excise Movement" should {

    "return 200" in {
      withAuthorizedTrader(authErn)
      stubEISSuccessfulRequest()

      val result = postRequest(request)
      val expectedResult = getPreValidateTraderSuccessResponse

      result.status mustBe OK

      withClue("return the json response") {
        val responseBody = Json.parse(result.body).as[PreValidateTraderMessageResponse]
        responseBody.exciseRegistrationNumber mustBe expectedResult.exciseRegistrationNumber
        responseBody.entityGroup mustBe expectedResult.entityGroup
        responseBody.validTrader mustBe expectedResult.validTrader
        responseBody.errorCode mustBe expectedResult.errorCode
        responseBody.errorText mustBe expectedResult.errorText
        responseBody.traderType mustBe expectedResult.traderType
        responseBody.validateProductAuthorisationResponse mustBe expectedResult.validateProductAuthorisationResponse
      }

    }

    "return not found if EIS returns not found" in {
      withAuthorizedTrader(authErn)
      stubEISErrorResponse(NOT_FOUND)

      val result = postRequest(request)

      result.status mustBe NOT_FOUND


      withClue("return the error response") {
        val body = Json.parse(result.body).as[EisErrorResponsePresentation]
        body.dateTime mustBe Instant.parse("2024-06-06T12:30:12.123Z")
        body.message mustBe "PreValidateTrader error"
        body.debugMessage mustBe "Error occurred during PreValidateTrader request"
        UUID.fromString(body.correlationId).toString must not be empty
      }
    }

    "return bad request if EIS returns BAD_REQUEST" in {
      withAuthorizedTrader(authErn)
      stubEISErrorResponse(BAD_REQUEST)

      postRequest(request).status mustBe BAD_REQUEST
    }

    "return 500 if EIS returns 500" in {
      withAuthorizedTrader(authErn)
      stubEISErrorResponse(INTERNAL_SERVER_ERROR)

      postRequest(request).status mustBe INTERNAL_SERVER_ERROR
    }

    "return a Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      postRequest(request).status mustBe UNAUTHORIZED
    }

    "return bad request (400) when json cannot be parsed" in {
      withAuthorizedTrader(authErn)

      postRequest(Json.toJson("{'blah': 'badJson'}")).status mustBe BAD_REQUEST
    }

    "return Unsupported Media Type (415)" in {
      withAuthorizedTrader(authErn)
      postRequest(Json.toJson(""), contentType = """application/xml""").status mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "return bad request (400) when body is not json" in {
      withAuthorizedTrader(authErn)

      val result = await(wsClient.url(url)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN",
          HeaderNames.CONTENT_TYPE -> """application/json"""
        ).post("test")
      )

      result.status mustBe BAD_REQUEST
    }

  }

  private def postRequest(json: JsValue, contentType: String = """application/json""") = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> contentType
      ).post(json)
    )
  }

  private def stubEISSuccessfulRequest() = {

    val response = getPreValidateTraderSuccessEISResponse
    wireMock.stubFor(
      post(eisUrl)
        .willReturn(ok().withBody(Json.toJson(response).toString()))
    )
  }

  private def stubEISErrorResponse(status: Int): Any = {

    wireMock.stubFor(
      post(urlEqualTo(eisUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody("")
            .withHeader("Content-Type", "application/json")
        )
    )
  }

}
