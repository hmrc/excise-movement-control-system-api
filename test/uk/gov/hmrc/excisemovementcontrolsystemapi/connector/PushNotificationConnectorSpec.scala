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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connector

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.BoxNotificationResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.typeOf

class PushNotificationConnectorSpec
  extends PlaySpec
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global;
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpClient = mock[HttpClient]
  private val appConfig = mock[AppConfig]
  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.now

  private val sut = new PushNotificationConnector(httpClient, appConfig, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpClient, appConfig)

    when(httpClient.GET[Any](any, any, any)(any,any,any))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(BoxNotificationResponse("123")).toString())))
    when(appConfig.pushPullNotificationHost).thenReturn("/notificationUrl")
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  "getBoxId" should {
    "return 200 status" in {
      val result = await(sut.getBoxId("clientId"))
      result mustBe Right(BoxNotificationResponse("123"))
    }

    "send the request to the notification service" in {
      val queryParams = Seq(
        "boxName"  -> "customs/excise##1.0##notificationUrl",
        "clientId" -> "clientId"
      )
      await(sut.getBoxId("clientId"))
      verify(httpClient).GET(eqTo("/notificationUrl/box"), eqTo(queryParams), any)(any,any,any)
    }

    "return an error" when {
      "Box Id not found" in {
        val errorJson = Json.obj( "code" -> "BOX_NOT_FOUND", "message" -> "Box does not exist")
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(404, errorJson.toString())))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe NotFound(Json.toJson(ErrorResponse(timestamp, "Push Notification Error", errorJson.toString())))
      }

      "is bad request" in {
        val errorJson = Json.obj( "code" -> "BAD_REQUEST", "message" -> "Box does not exist")
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(400, errorJson.toString())))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe BadRequest(Json.toJson(ErrorResponse(timestamp, "Push Notification Error", errorJson.toString())))
      }

      "is unknown error" in {
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(500, "unknown error")))

        val result = await(sut.getBoxId("clientId"))

        result.left.value mustBe InternalServerError(Json.toJson(ErrorResponse(timestamp, "Push Notification Error", "unknown error")))
      }

      "cannot parse json" in {
        val errorJson = Json.obj( "code" -> "UNKNOWN_ERROR", "message" -> "Box does not exist")
        when(httpClient.GET[Any](any, any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(200, errorJson.toString())))

        val result = await(sut.getBoxId("clientId"))

        val expectedError = Json.toJson(
          ErrorResponse(
            timestamp,
            "Push Notification Error",
            s"Response body could not be read as type ${typeOf[BoxNotificationResponse]}")
        )
        result.left.value mustBe InternalServerError(expectedError)
      }
    }
  }
}

