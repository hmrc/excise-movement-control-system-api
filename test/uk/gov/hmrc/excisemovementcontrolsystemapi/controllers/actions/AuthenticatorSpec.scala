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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentials, internalId}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, Enrolment, Enrolments, InsufficientEnrolments, InternalError}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatorSpec extends PlaySpec with BeforeAndAfterEach with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val hc            = HeaderCarrier()
  private val request       = FakeRequest()
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val mcc           = stubMessagesControllerComponents()
  private val authenticator = new AuthActionService(authConnector, mcc)(ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector)
  }

  "Authenticator" should {

    "authorised with a single enrolment" in {
      val enrolment = Enrolment("HMRC-EMCS-ORG").withIdentifier("ExciseNumber", "123")
      withAuthorizedTrader(Set(enrolment))

      await(authenticator.authorisedWithErn( "123")(hc, request))

      verify(authConnector).authorise(eqTo(enrolment), any)(any, any)
    }

    "authorised with a multiple enrolment" in {
      val enrolment = Enrolment("HMRC-EMCS-ORG").withIdentifier("ExciseNumber", "123")
      val enrolment1 = Enrolment("HMRC-EMCS-ORG").withIdentifier("ExciseNumber", "456")
      withAuthorizedTrader(Set(enrolment, enrolment1))

      await(authenticator.authorisedWithErn( "456")(hc, request))

      verify(authConnector).authorise(eqTo(enrolment), any)(any, any)
    }

    "return Unauthorized error" when {
      "have no internal id" in {
        val enrolment = Enrolment("HMRC-EMCS-ORG").withIdentifier("ExciseNumber", "123")
        withAuthorizedTrader(Set(enrolment), None)

        val result = await(authenticator.authorisedWithErn( "123")(hc, request))

        result.left.value.statusCode mustBe INTERNAL_SERVER_ERROR
        result.left.value.message mustBe "Internal server error is AuthActionService::authorisedWithErn -  internalId is required"

      }
      "throwing" in {
        withUnauthorizedTrader(new RuntimeException)

        val result = await(authenticator.authorisedWithErn("123")(hc, request))

        result.left.value.statusCode mustBe INTERNAL_SERVER_ERROR
      }

      "general failure" in {

        withUnauthorizedTrader(InternalError("A general auth failure"))

        val result = await(authenticator.authorisedWithErn("1")(hc, request))

        result.left.value.statusCode mustBe UNAUTHORIZED
      }

      "auth returns Insufficient enrolments" in {
        withUnauthorizedTrader(InsufficientEnrolments())

        val result = await(authenticator.authorisedWithErn("123")(hc, request))

        result.left.value.statusCode mustBe UNAUTHORIZED
      }
    }
  }

  private def withAuthorizedTrader(enrolments: Set[Enrolment], id: Option[String] = Some("123")): Unit = {
    val fetch = allEnrolments and affinityGroup and credentials and internalId
    val retrieval = Enrolments(enrolments) and
      Some(AffinityGroup.Organisation) and
      Some(Credentials("testProviderId", "testProviderType")) and
      id

    when(authConnector.authorise(ArgumentMatchers.argThat((p: Predicate) => true), eqTo(fetch))(any,any))
      .thenReturn(Future.successful(retrieval))
  }

  def withUnauthorizedTrader(error: Throwable): Unit =
    when(authConnector.authorise(any, any)(any, any)).thenReturn(Future.failed(error))
}
