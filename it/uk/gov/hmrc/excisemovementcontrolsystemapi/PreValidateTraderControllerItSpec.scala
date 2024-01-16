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
import org.scalatest.{BeforeAndAfterAll, OptionValues}
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.WireMockServerSpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.PreValidateTraderResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils.{getPreValidateTraderRequest, getPreValidateTraderSuccessResponse}


class PreValidateTraderControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with WireMockServerSpec
  with BeforeAndAfterAll
  with OptionValues {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val url = s"http://localhost:$port/traders/pre-validate"
  private val eisUrl = "/emcs/pre-validate-trader/v1"
  private val authErn = "GBWK002281023"

  private val request = Json.toJson(getPreValidateTraderRequest)

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())
    GuiceApplicationBuilder()
      .configure(configureEisService)
      .overrides(
        bind[AuthConnector].to(authConnector),
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

    "return 200" in {
      withAuthorizedTrader(authErn)
      stubEISSuccessfulRequest()

      val result = postRequest(request)
      val expectedResult = getPreValidateTraderSuccessResponse.exciseTraderValidationResponse

      result.status mustBe OK

      withClue("return the json response") {
        val responseBody = Json.parse(result.body).as[PreValidateTraderResponse].exciseTraderValidationResponse
        responseBody.value.validationTimeStamp mustBe expectedResult.value.validationTimeStamp

        responseBody.value.exciseTraderResponse(0) mustBe expectedResult.value.exciseTraderResponse(0)
      }

    }

    "return not found if EIS returns not found" in {
      withAuthorizedTrader(authErn)
      stubEISErrorResponse(NOT_FOUND)

      val result = postRequest(request)

      result.status mustBe NOT_FOUND

      withClue("return the EIS error response") {
        result.body mustBe ""
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

    val response = getPreValidateTraderSuccessResponse
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
