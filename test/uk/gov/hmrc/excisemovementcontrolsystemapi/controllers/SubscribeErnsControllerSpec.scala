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
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.SubscribeErnsController.{SubscribeErnRequest, UnsubscribeErnRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService

import scala.concurrent.{ExecutionContext, Future}

class SubscribeErnsControllerSpec
    extends PlaySpec
    with FakeAuthentication
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val mockNotificationsService = mock[NotificationsService]

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[NotificationsService].toInstance(mockNotificationsService)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNotificationsService)
  }

  "subscribeErn" must {
    "subscribe the given ern to the clientId" in {

      when(mockNotificationsService.subscribeErns(any, any)(any)).thenReturn(Future.successful(Done))

      val request = FakeRequest(routes.SubscribeErnsController.subscribeErn("ern1"))
        .withHeaders(
          FakeHeaders(
            Seq(
              HeaderNames.CONTENT_TYPE -> "application/json",
              "X-Client-Id"            -> "clientId",
              "X-Callback-Box-Id"      -> "clientBoxId"
            )
          )
        )
        .withBody(Json.toJson(SubscribeErnRequest("clientId", "ern1")))

      val result = createWithSuccessfulAuth.subscribeErn("ern1")(request)

      status(result) mustBe OK
      verify(mockNotificationsService).subscribeErns(eqTo("clientId"), eqTo(Seq("ern1")))(any)
    }

    "return error" when {
      "unauthorised" in {

        val request = FakeRequest()
          .withHeaders(
            FakeHeaders(
              Seq(
                HeaderNames.CONTENT_TYPE -> "application/json",
                "X-Client-Id"            -> "clientId",
                "X-Callback-Box-Id"      -> "clientBoxId"
              )
            )
          )
          .withBody(Json.toJson(SubscribeErnRequest("clientId", "ern1")))

        val result = createWithFailingAuth.subscribeErn("ern1")(request)

        status(result) mustBe FORBIDDEN
        verify(mockNotificationsService, times(0)).subscribeErns(any, any)(any)
      }

      "request body is malformed" in {

        val request = FakeRequest()
          .withHeaders(
            FakeHeaders(
              Seq(
                HeaderNames.CONTENT_TYPE -> "application/json",
                "X-Client-Id"            -> "clientId",
                "X-Callback-Box-Id"      -> "clientBoxId"
              )
            )
          )
          .withBody(Json.obj())

        val result = createWithSuccessfulAuth.subscribeErn("ern1")(request)

        status(result) mustBe BAD_REQUEST
        verify(mockNotificationsService, times(0)).subscribeErns(any, any)(any)
      }
    }
  }

  "unsubscribeErn" must {
    "unsubscribe the given ern from the clientId" in {

      when(mockNotificationsService.unsubscribeErns(any, any)(any)).thenReturn(Future.successful(Done))

      val request = FakeRequest(routes.SubscribeErnsController.unsubscribeErn("ern1"))
        .withHeaders(
          FakeHeaders(
            Seq(
              HeaderNames.CONTENT_TYPE -> "application/json",
              "X-Client-Id"            -> "clientId",
              "X-Callback-Box-Id"      -> "clientBoxId"
            )
          )
        )
        .withBody(Json.toJson(UnsubscribeErnRequest("clientId", "ern1")))

      val result = createWithSuccessfulAuth.unsubscribeErn("ern1")(request)

      status(result) mustBe OK
      verify(mockNotificationsService).unsubscribeErns(eqTo("clientId"), eqTo(Seq("ern1")))(any)
    }

    "return error" when {
      "unauthorised" in {

        val request = FakeRequest()
          .withHeaders(
            FakeHeaders(
              Seq(
                HeaderNames.CONTENT_TYPE -> "application/json",
                "X-Client-Id"            -> "clientId",
                "X-Callback-Box-Id"      -> "clientBoxId"
              )
            )
          )
          .withBody(Json.toJson(UnsubscribeErnRequest("clientId", "ern1")))

        val result = createWithFailingAuth.unsubscribeErn("ern1")(request)

        status(result) mustBe FORBIDDEN
        verify(mockNotificationsService, times(0)).unsubscribeErns(any, any)(any)
      }

      "request body is malformed" in {

        val request = FakeRequest()
          .withHeaders(
            FakeHeaders(
              Seq(
                HeaderNames.CONTENT_TYPE -> "application/json",
                "X-Client-Id"            -> "clientId",
                "X-Callback-Box-Id"      -> "clientBoxId"
              )
            )
          )
          .withBody(Json.obj())

        val result = createWithSuccessfulAuth.unsubscribeErn("ern1")(request)

        status(result) mustBe BAD_REQUEST
        verify(mockNotificationsService, times(0)).unsubscribeErns(any, any)(any)
      }
    }

  }

  private def createWithSuccessfulAuth =
    new SubscribeErnsController(
      FakeSuccessAuthentication(Set(ern)),
      stubControllerComponents(),
      mockNotificationsService
    )

  private def createWithFailingAuth =
    new SubscribeErnsController(
      FakeFailingAuthentication,
      stubControllerComponents(),
      mockNotificationsService
    )

}
