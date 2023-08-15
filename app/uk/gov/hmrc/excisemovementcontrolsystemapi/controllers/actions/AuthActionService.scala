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

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentials, internalId}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{AuthorizedRequest, EnrolmentKey}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthActionService @Inject()(
  override val authConnector: AuthConnector,
  cc: ControllerComponents
)(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions with Authenticator with Logging {

  //todo: do we need affinityGroup and credentials? is Enrolments and the internal id sufficient?
  protected val fetch = allEnrolments and affinityGroup and credentials and internalId

  override def authorisedAction[A](bodyParser: BodyParser[A], ern: String)(body: AuthorizedRequest[A] => Future[Result]): Action[A] =
    Action.async(bodyParser) { implicit request =>
      authorisedWithErn(ern).flatMap {
        case Right(authorisedRequest) =>
          logger.info(s"Authorised request for ${authorisedRequest.ern}")
          body(authorisedRequest)
        case Left(error) =>
          logger.error(s"Problems with Authorisation: ${error.message}")
          Future.successful(Unauthorized(error.message))
      }
    }

  def authorisedWithErn[A](
    ern: String
  )(implicit hc: HeaderCarrier, request: Request[A]): Future[Either[ErrorResponse, AuthorizedRequest[A]]] = {
    authorised(Enrolment(EnrolmentKey.EMCS_ENROLMENT).withIdentifier(EnrolmentKey.ERN, ern))
      .retrieve(fetch) { retrievals =>

        /*todo:
          1. do we need to check for Organisation too?
          2. do we need more granular log error reporting?
        */
        val retrievalId = retrievals.b.getOrElse(throw new IllegalStateException("Internal server error is AuthActionService::authorisedWithErn -  internalId is required"))

        Future.successful(Right(AuthorizedRequest(request, ern, retrievalId)))
      }.recover {
      case error: AuthorisationException =>
        logger.error(s"Unauthorised Exception for ${request.uri} with error ${error.reason}")
        Left(ErrorResponse(UNAUTHORIZED, "Unauthorised user"))
      case ex: Throwable =>
        logger.error(ex.getMessage)
        Left(ErrorResponse(INTERNAL_SERVER_ERROR, ex.getMessage))

    }

  }
}

@ImplementedBy(classOf[AuthActionService])
trait Authenticator {
  def authorisedAction[A](bodyParser: BodyParser[A], ern: String)(
    body: AuthorizedRequest[A] => Future[Result]
  ): Action[A]
}
