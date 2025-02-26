/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.CorrelationIdAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{HttpHeader, NotificationsService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class SubscribeErnsControllerSpec
    extends PlaySpec
    with FakeAuthentication
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val mockNotificationsService = mock[NotificationsService]
  private val mockDateTimeService      = mock[DateTimeService]
  private val mockAppConfig            = mock[AppConfig]
  private val timestamp                = Instant.parse("2024-05-06T15:30:15.12345612Z")

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[NotificationsService].toInstance(mockNotificationsService),
        bind[DateTimeService].toInstance(mockDateTimeService)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNotificationsService, mockDateTimeService)
  }

  "subscribeErn" must {
    "subscribe the given ern to the clientId" in {
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)
      when(mockNotificationsService.subscribeErns(any, any)(any)).thenReturn(Future.successful("boxId"))
      when(mockAppConfig.subscribeErnsEnabled).thenReturn(true)
      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn(ern))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json",
          "X-Client-Id"            -> "clientId"
        )

      val result = createWithSuccessfulAuth.subscribeErn(ern)(request)

      status(result) mustBe ACCEPTED
      contentAsString(result) mustBe "boxId"
      verify(mockNotificationsService).subscribeErns(eqTo("clientId"), eqTo(Seq(ern)))(any)
    }

    "subscribe return Forbidden due to mismatch between ern in request and one passed in" in {
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)
      when(mockNotificationsService.subscribeErns(any, any)(any)).thenReturn(Future.successful("boxId"))
      when(mockAppConfig.subscribeErnsEnabled).thenReturn(true)

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn(ern))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json",
          "X-Client-Id"            -> "clientId"
        )

      val result = createWithSuccessfulAuth.subscribeErn("ern1")(request)

      status(result) mustBe FORBIDDEN
    }

    "subscribe returns BadRequest due to missing ClientId in header" in {
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      when(mockNotificationsService.subscribeErns(any, any)(any)).thenReturn(Future.successful("boxId"))

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn(ern))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json"
        )

      val result = createWithSuccessfulAuth.subscribeErn(ern)(request)

      status(result) mustBe BAD_REQUEST
    }

    "return error" when {
      "unauthorised" in {

        val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
            "X-Client-Id"            -> "clientId"
          )

        val result = createWithFailingAuth.subscribeErn("ern1")(request)

        status(result) mustBe FORBIDDEN
        verify(mockNotificationsService, times(0)).subscribeErns(any, any)(any)
      }
    }
  }

  "unsubscribeErn" must {
    "unsubscribe the given ern from the clientId" in {
      when(mockNotificationsService.unsubscribeErns(any, any)(any)).thenReturn(Future.successful(Done))

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json",
          "X-Client-Id"            -> "clientId"
        )

      val result = createWithSuccessfulAuth.unsubscribeErn("ern1")(request)

      status(result) mustBe ACCEPTED
      contentAsString(result) mustBe ""
      verify(mockNotificationsService).unsubscribeErns(eqTo("clientId"), eqTo(Seq("ern1")))(any)
    }

    "unsubscribe returns BadRequest when ClientId is missing from header" in {
      when(mockNotificationsService.unsubscribeErns(any, any)(any)).thenReturn(Future.successful(Done))
      when(mockDateTimeService.timestamp()).thenReturn(timestamp)

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json"
        )

      val result = createWithSuccessfulAuth.unsubscribeErn("ern1")(request)

      status(result) mustBe BAD_REQUEST
    }

    "return error" when {
      "unauthorised" in {

        val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
            "X-Client-Id"            -> "clientId"
          )

        val result = createWithFailingAuth.unsubscribeErn("ern1")(request)

        status(result) mustBe FORBIDDEN
        verify(mockNotificationsService, times(0)).unsubscribeErns(any, any)(any)
      }
    }

  }

  private def createWithSuccessfulAuth =
    new SubscribeErnsController(
      FakeSuccessAuthentication(Set(ern)),
      new CorrelationIdAction,
      stubControllerComponents(),
      mockNotificationsService,
      mockDateTimeService,
      mockAppConfig
    )

  private def createWithFailingAuth =
    new SubscribeErnsController(
      FakeFailingAuthentication,
      new CorrelationIdAction,
      stubControllerComponents(),
      mockNotificationsService,
      mockDateTimeService,
      mockAppConfig
    )
}
