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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connector


import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MovementMessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISRequest, EISResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class MovementMessageConnectorSpec extends PlaySpec with BeforeAndAfterEach {

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
  protected implicit val ec: ExecutionContext = ExecutionContext.global
  private val mockHttpClient = mock[HttpClient]
  private val eisUtils = mock[EisUtils]
  private val connector = new MovementMessageConnector(mockHttpClient, eisUtils)
  private val emcsCorrelationId = "1234566"
  private val message = "<IE815></IE815>"
  private val messageType = "IE815"
  private val eisResponse = EISResponse(
    "Ok", "Message successfully received by EMCS", emcsCorrelationId
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
    when(eisUtils.getCurrentDateTimeString).thenReturn("2023-09-17T09:32:50.345Z")
    when(eisUtils.generateCorrelationId).thenReturn(emcsCorrelationId)

  }

  "post" should {
    "return successful EISResponse" in {
      when(mockHttpClient.POST[JsValue, EISResponse](any, any, any)(any, any, any, any)).thenReturn(Future.successful(eisResponse))
      val result = await(connector.post(message, messageType))

      result mustBe Right(eisResponse)
    }

    "use the right request parameters in http client" in {
      when(mockHttpClient.POST[JsValue, EISResponse](any, any, any)(any, any, any, any)).thenReturn(Future.successful(eisResponse))
      val eisRequest = EISRequest(emcsCorrelationId, "2023-09-17T09:32:50.345Z", messageType, "APIP", "user1", message)
      await(connector.post(message, messageType))

      val jsonObj = Json.toJson(eisRequest)

      val headers = Seq(HeaderNames.ACCEPT -> ContentTypes.JSON,
        HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
        "dateTime" -> eisRequest.createdDateTime,
        "x-correlation-id" -> eisRequest.emcsCorrelationId,
        "x-forwarded-host" -> "",
        "source" -> eisRequest.source)

      verify(mockHttpClient).POST(eqTo("http://localhost:9000/emcs-api-eis-stub/eis/receiver/v1/messages"), eqTo(jsonObj), eqTo(headers))(any, any, any, any)
    }
  }

}
