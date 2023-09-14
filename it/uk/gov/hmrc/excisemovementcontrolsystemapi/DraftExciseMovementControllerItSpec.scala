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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{FORBIDDEN, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport


class DraftExciseMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  override lazy val app: Application = {
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(authConnector),
      )
      .build()
  }

  "Draft Excise Movement" should {
    "return 200" in {
      withAuthorizedTrader

      postRequest.status mustBe OK
    }

    "return 403 when there are no authorized ERN" in {
      withUnAuthorizedERN

      postRequest.status mustBe FORBIDDEN
    }

    "return a 401 when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      postRequest.status mustBe UNAUTHORIZED
    }
  }

  private def postRequest = {
    await(wsClient.url(s"http://localhost:$port/customs/excise/movements")
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> """application/vnd.hmrc.1.0+xml"""
      ).post(IE815)
    )
  }
}
