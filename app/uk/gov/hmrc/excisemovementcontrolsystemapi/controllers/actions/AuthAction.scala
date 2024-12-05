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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentials, groupIdentifier, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentKey, EnrolmentRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse => EMCSErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  val parser: BodyParsers.Default,
  dateTimeService: DateTimeService
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with AuthAction
    with Logging {

  private val fetch = allEnrolments and affinityGroup and credentials and internalId and groupIdentifier

  protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: EnrolmentRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    implicit val req: Request[A]   = request

    authorise.flatMap {
      case Right(authorisedRequest) =>
        block(authorisedRequest)

      case Left(error) if error.statusCode == FORBIDDEN =>
        logger.warn(s"Forbidden: ${error.message}")

        Future.successful(
          Forbidden(
            Json.toJson(
              EMCSErrorResponse(
                dateTimeService.timestamp(),
                "Authorisation error",
                error.message
              )
            )
          )
        )

      case Left(error) =>
        logger.warn(s"Problems with Authorisation: ${error.message}")

        Future.successful(
          Unauthorized(
            Json.toJson(
              EMCSErrorResponse(
                dateTimeService.timestamp(),
                "Authorisation error",
                error.message
              )
            )
          )
        )
    }
  }

  def authorise[A](implicit
    hc: HeaderCarrier,
    request: Request[A]
  ): Future[Either[ErrorResponse, EnrolmentRequest[A]]] =
    authorised(Enrolment(EnrolmentKey.EMCS_ENROLMENT))
      .retrieve(fetch) {
        case enrolments ~ Some(Organisation) ~ Some(credentials) ~ Some(internalId) ~ Some(groupIdentifier) =>
          Future.successful(checkErns(enrolments, UserDetails(internalId, groupIdentifier)))
        case _ ~ None ~ _ ~ _ ~ _                                                                           => handleAuthError("Could not retrieve affinity group from Auth")
        case _ ~ Some(affinityGroup) ~ _ ~ _ ~ _ if affinityGroup != Organisation                           =>
          handleAuthError(s"Invalid affinity group $affinityGroup from Auth")
        case _ ~ _ ~ None ~ _ ~ _                                                                           => handleAuthError("Could not retrieve credentials from Auth")
        case _ ~ _ ~ _ ~ None ~ _                                                                           => handleAuthError("Could not retrieve internalId from Auth")
        case _                                                                                              => handleAuthError("Invalid enrolment parameter from Auth")
      }
      .recover {
        case error: AuthorisationException =>
          handleException(UNAUTHORIZED, s"Unauthorised Exception for ${request.uri} with error ${error.reason}")
        case NonFatal(ex)                  =>
          handleException(INTERNAL_SERVER_ERROR, s"Internal server error is ${ex.getMessage}")
      }

  private def handleAuthError[A](message: String): Future[Left[ErrorResponse, Nothing]] =
    Future.successful(Left(ErrorResponse(UNAUTHORIZED, message)))

  private def handleException(status: Int, msg: String): Left[ErrorResponse, Nothing] = {
    logger.error(msg)
    Left(ErrorResponse(status, msg))
  }

  private def checkErns[A](
    enrolments: Enrolments,
    userDetails: UserDetails
  )(implicit request: Request[A]): Either[ErrorResponse, EnrolmentRequest[A]] = {
    val erns = getAllErnsForEmcsEnrolment(enrolments)

    if (erns.isEmpty) {
      logger.warn(s"Could not find ${EnrolmentKey.ERN}")
      Left(ErrorResponse(FORBIDDEN, s"Could not find ${EnrolmentKey.ERN}"))
    } else {
      Right(EnrolmentRequest(request, erns, userDetails))
    }
  }

  private def getAllErnsForEmcsEnrolment(enrolments: Enrolments): Set[String] =
    enrolments.enrolments
      .filter(enrolments =>
        enrolments.key.equalsIgnoreCase(EnrolmentKey.EMCS_ENROLMENT) && enrolments.state.equalsIgnoreCase(
          EnrolmentKey.ACTIVATED
        )
      )
      .flatMap(_.identifiers.filter(i => i.key.equalsIgnoreCase(EnrolmentKey.ERN)))
      .map(_.value)
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[EnrolmentRequest, AnyContent] with ActionFunction[Request, EnrolmentRequest]
