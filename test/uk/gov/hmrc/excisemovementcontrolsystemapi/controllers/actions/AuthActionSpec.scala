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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec
  extends PlaySpec
    with AuthTestSupport
    with BeforeAndAfterEach
    with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val parser = mock[BodyParsers.Default]
  private val authenticator = new AuthActionImpl(authConnector, stubMessagesControllerComponents(), parser)(ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector)
  }

  private def block(authRequest: EnrolmentRequest[_]) =
    Future.successful(Results.Ok)

  "authorisation" should {

    "authorised with a enrolment" in {
      withAuthorizedTrader()

      val result = await(authenticator.invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-EMCS-ORG")), any)(any, any)
    }

    "return multiple erns for one enrolment" in {

      val block = (authRequest: EnrolmentRequest[_]) => {
        authRequest.erns.toSeq mustBe Seq("123", "963")
        Future.successful(Results.Ok)
      }

      val enrolment = Enrolment("HMRC-EMCS-ORG")
        .withIdentifier("ExciseNumber", "123")
        .withIdentifier("ExciseNumber", "963")

      withAuthorizedTrader(enrolment)

      await(authenticator.invokeBlock(FakeRequest(), block))
    }

    "return Unauthorized error" when {
      "have no internal id" in {
        withUnAuthorizedInternalId()

        val result = await(authenticator.invokeBlock(FakeRequest(), block))

        result mustBe Unauthorized("Could not retrieve internalId from Auth")
      }

      "affinity group is Individual" in {
        authorizeWithAffinityGroup(Some(Individual))

        val result = await(authenticator.invokeBlock(FakeRequest(), block))

        result mustBe Unauthorized(s"Invalid affinity group $Individual from Auth")
      }

      "affinity group is Agent" in {
        authorizeWithAffinityGroup(Some(Agent))

        val result = await(authenticator.invokeBlock(FakeRequest(), block))

        result mustBe Unauthorized(s"Invalid affinity group $Agent from Auth")
      }

      "has no affinity group" in {
        authorizeWithAffinityGroup(None)

        val result = await(authenticator.invokeBlock(FakeRequest(), block))

        result mustBe Unauthorized(s"Could not retrieve affinity group from Auth")
      }

      "has no credential" in {

        withUnauthorizedCredential()

        val result = await(authenticator.invokeBlock(FakeRequest(), block))

        result mustBe Unauthorized("Could not retrieve credentials from Auth")
      }


      "throwing" in {
        withUnauthorizedTrader(new RuntimeException("error"))

        val result = await(authenticator.invokeBlock(FakeRequest(), block))

        result mustBe Unauthorized("Internal server error is error")
      }

      "general failure" in {

        withUnauthorizedTrader(InternalError("A general auth failure"))

        val result = await(authenticator.invokeBlock(FakeRequest(GET, "/foo"), block))

        result mustBe Unauthorized("Unauthorised Exception for /foo with error A general auth failure")
      }

      "auth returns Insufficient enrolments" in {
        withUnauthorizedTrader(InsufficientEnrolments())

        val result = await(authenticator.invokeBlock(FakeRequest(GET, "/get"), block))

        result mustBe Unauthorized("Unauthorised Exception for /get with error Insufficient Enrolments")
      }
    }

    "auth return forbidden" in {
      withAnEmptyERN()

      val result = await(authenticator.invokeBlock(FakeRequest(GET, "/get"), block))

      result.header.status mustBe FORBIDDEN
    }
  }
}
