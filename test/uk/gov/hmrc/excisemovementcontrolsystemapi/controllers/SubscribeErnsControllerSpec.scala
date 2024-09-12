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
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import scala.concurrent.{ExecutionContext, Future}

class SubscribeErnsControllerSpec
    extends PlaySpec
    with FakeAuthentication
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val mockNotificationsService = mock[NotificationsService]
  private val mockDateTimeService      = mock[DateTimeService]
  private val mockAppConfig            = mock[AppConfig]

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
    reset(mockNotificationsService, mockDateTimeService, mockAppConfig)
  }

  "subscribeErn" must {
    "subscribe the given ern to the clientId" in {
      when(mockAppConfig.subscribeErnsEnabled).thenReturn(true)
      when(mockNotificationsService.subscribeErns(any, any)(any)).thenReturn(Future.successful("boxId"))

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json",
          "X-Client-Id"            -> "clientId"
        )

      val result = createWithSuccessfulAuth.subscribeErn("ern1")(request)

      status(result) mustBe ACCEPTED
      contentAsString(result) mustBe "boxId"
      verify(mockNotificationsService).subscribeErns(eqTo("clientId"), eqTo(Seq("ern1")))(any)
    }

    "subscribe the given ern should return NotFound when feature flag is off" in {
      when(mockAppConfig.subscribeErnsEnabled).thenReturn(false)

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json",
          "X-Client-Id"            -> "clientId"
        )

      val result = createWithSuccessfulAuth.subscribeErn("ern1")(request)

      status(result) mustBe NOT_FOUND
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
      when(mockAppConfig.subscribeErnsEnabled).thenReturn(true)
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

    "unsubscribe the given ern returns NotFound when feature flag is off" in {
      when(mockAppConfig.subscribeErnsEnabled).thenReturn(false)

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json",
          "X-Client-Id"            -> "clientId"
        )

      val result = createWithSuccessfulAuth.unsubscribeErn("ern1")(request)

      status(result) mustBe NOT_FOUND
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
      stubControllerComponents(),
      mockNotificationsService,
      mockDateTimeService,
      mockAppConfig
    )

  private def createWithFailingAuth =
    new SubscribeErnsController(
      FakeFailingAuthentication,
      stubControllerComponents(),
      mockNotificationsService,
      mockDateTimeService,
      mockAppConfig
    )
}
