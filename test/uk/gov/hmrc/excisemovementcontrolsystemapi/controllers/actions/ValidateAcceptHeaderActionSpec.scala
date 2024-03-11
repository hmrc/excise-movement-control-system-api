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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Results.NotAcceptable
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.ExecutionContext


class ValidateAcceptHeaderActionSpec extends PlaySpec {

  protected implicit val ec: ExecutionContext = ExecutionContext.global

  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.now
  when(dateTimeService.timestamp()).thenReturn(timestamp)

  private val sut = new ValidateAcceptHeaderAction(dateTimeService)

  "filter" should {

    "return None" when {
      "Accept header is xml with version 1.0" in {

        val request = createRequestWithAcceptHeader("application/vnd.hmrc.1.0+xml")
        val result = await(sut.filter(request))

        result mustBe None
      }
    }

    "return an error" when {

      "Accept header is empty" in {
        val result = await(sut.filter(FakeRequest()))

        result mustBe expectedError
      }

      "Accept header is json" in {
        val result = await(sut.filter(createRequestWithAcceptHeader("application/vnd.hmrc.1.0+json")))

        result mustBe expectedError
      }

      "Accept header is application/xml" in {

        val result = await(sut.filter(createRequestWithAcceptHeader("application/xml")))

        result mustBe expectedError
      }

      "Accept header is application/json" in {

        val result = await(sut.filter(createRequestWithAcceptHeader("application/json")))

        result mustBe expectedError
      }

      "accept header is the wrong version" in {
        val request = createRequestWithAcceptHeader("application/vnd.hmrc.3.0+xml")
        val result = await(sut.filter(request))

        result mustBe expectedError
      }
    }
  }

  private def expectedError: Option[Result] = {
    Some(NotAcceptable(Json.toJson(
      ErrorResponse(
        timestamp,
        "Invalid Accept header",
        "The accept header is missing or invalid"))))
  }

  private def createRequestWithAcceptHeader(header: String): FakeRequest[AnyContent] = {
    FakeRequest()
      .withHeaders(
        FakeHeaders(Seq(HeaderNames.ACCEPT -> header))
      )
  }
}
