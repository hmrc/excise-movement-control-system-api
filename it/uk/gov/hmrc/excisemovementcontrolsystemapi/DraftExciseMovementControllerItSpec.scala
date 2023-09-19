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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ok, post}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import play.api.libs.json.Json
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISResponse

import scala.xml.NodeSeq


class DraftExciseMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with BeforeAndAfterAll {

  private val wireHost = "localhost"
  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  implicit lazy val wireMock: WireMockServer = new WireMockServer(options().dynamicPort())
  private val url = s"http://localhost:$port/customs/excise/movements"

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())
    GuiceApplicationBuilder()
      .configure(
        Map(
          "microservice.services.eis.host" -> wireHost,
          "microservice.services.eis.port" -> wireMock.port())
      )
      .overrides(
        bind[AuthConnector].to(authConnector),
      )
      .build()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    //wireMock.start()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Draft Excise Movement" should {
    "return 200" in {
      withAuthorizedTrader("GBWK002281023")
      stubEISRequest

      postRequest(IE815).status mustBe OK
    }

    "return forbidden (403) when there are no authorized ERN" in {
      withUnAuthorizedERN

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

    "return forbidden (403) when consignor id cannot be validate" in {
      withAuthorizedTrader("123")

      postRequest(IE815).status mustBe FORBIDDEN
    }
  }
  private def postRequest(xml: NodeSeq = IE815, contentType: String =  """application/vnd.hmrc.1.0+xml""") = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> contentType
      ).post(xml)
    )
  }

  private def stubEISRequest = {

    val response = EISResponse("ok", "message", "123")
    wireMock.stubFor(
      post("/emcs-api-eis-stub/eis/receiver/v1/messages")
        .willReturn(ok().withBody(Json.toJson(response).toString()))
    )
  }
}
