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
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments, credentials, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{AuthorizedRequest, EnrolmentKey}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AuthActionImpl @Inject()
(
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  val parser: BodyParsers.Default
) (implicit val ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions with AuthAction with Logging{

  protected val fetch = authorisedEnrolments and affinityGroup and credentials and internalId
  protected def executionContext = ec

  override def invokeBlock[A](request: Request[A], block: AuthorizedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    implicit val req = request

      authorise.flatMap {
        case Right(authorisedRequest) =>
          logger.info(s"Authorised request for ${authorisedRequest.erns.mkString(",")}")
          block(authorisedRequest)
        case Left(error) if error.statusCode == FORBIDDEN =>
          logger.error(s"Forbidden: ${error.message}")
          Future.successful(Forbidden(error.message))
        case Left(error) =>
          logger.error(s"Problems with Authorisation: ${error.message}")
          Future.successful(Unauthorized(error.message))
      }
  }

  def authorise[A](implicit hc: HeaderCarrier, request: Request[A]): Future[Either[ErrorResponse, AuthorizedRequest[A]]] = {
    authorised(Enrolment(EnrolmentKey.EMCS_ENROLMENT))
      .retrieve(fetch) { retrievals =>

        retrievals match {
          case authorisedEnrolments ~ Some(Organisation) ~ Some(credentials) ~ Some(internalId) =>
            Future.successful(checkErns(authorisedEnrolments, internalId))
          case _ ~ None ~ _ ~ _ => handleAuthError("Could not retrieve affinity group from Auth")
          case _ ~ Some(affinityGroup) ~ _ ~ _ if(affinityGroup != Organisation) =>
            handleAuthError(s"Invalid affinity group $affinityGroup from Auth")
          case _ ~ _ ~ None ~ _ => handleAuthError("Could not retrieve credentials from Auth")
          case _ ~ _ ~ _ ~ None => handleAuthError("Could not retrieve internalId from Auth")
          case _ => handleAuthError("Invalid enrolment parameter from Auth")
        }
      }.recover {
      case error: AuthorisationException =>
        handleException(UNAUTHORIZED, s"Unauthorised Exception for ${request.uri} with error ${error.reason}")
      case ex: Throwable =>
        handleException(INTERNAL_SERVER_ERROR, s"Internal server error is ${ex.getMessage}")
    }
  }

  private def handleAuthError[A](message: String): Future[Left[ErrorResponse, Nothing]] = {
    Future.successful(Left(ErrorResponse(UNAUTHORIZED, message)))
  }

  private def handleException(status: Int, msg: String): Left[ErrorResponse, Nothing] = {
    logger.error(msg)
    Left(ErrorResponse(status, msg))
  }

  def checkErns[A](enrolments: Enrolments, internalId: String)(implicit request: Request[A]): Either[ErrorResponse, AuthorizedRequest[A]] = {

    val erns: Set[EnrolmentIdentifier] = enrolments.enrolments.flatMap(e => e.getIdentifier(EnrolmentKey.ERN))

    if(erns.isEmpty) {
      logger.error(s"Could not find ${EnrolmentKey.ERN}")
      Left(ErrorResponse(FORBIDDEN, s"Could not find ${EnrolmentKey.ERN}"))
    }
    else {
      Right(AuthorizedRequest(request, erns.map(_.value), internalId))
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthorizedRequest, AnyContent] with ActionFunction[Request, AuthorizedRequest]


