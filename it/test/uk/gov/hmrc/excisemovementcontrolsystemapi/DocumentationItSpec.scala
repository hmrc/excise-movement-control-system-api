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

package uk.gov.hmrc.excisemovementcontrolsystemapi;

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.ApplicationBuilderSupport;

class DocumentationItSpec extends PlaySpec with GuiceOneServerPerSuite with ApplicationBuilderSupport {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  override def fakeApplication(): Application = application

  "get" should {
    "return the definition specification" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/definition").get())

      response.status mustBe OK
      response.body must not be empty
    }

    "return an OpenAPi Specification (OAS)" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/application.yaml").get())

      response.status mustBe OK
      response.body must not be empty
    }

    "return a 404 if not specification found" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/111.0/application.yaml").get())

      response.status mustBe NOT_FOUND
    }
  }
}
