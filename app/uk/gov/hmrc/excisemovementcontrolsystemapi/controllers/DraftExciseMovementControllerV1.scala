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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import cats.data.EitherT
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, CorrelationIdAction, ParseXmlAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.IE815MessageV1
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.BoxIdRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementControllerV1 @Inject() (
  authAction: AuthAction,
  xmlParser: ParseXmlAction,
  movementMessageService: MovementService,
  submissionMessageService: SubmissionMessageService,
  notificationService: PushNotificationService,
  messageValidator: MessageValidation,
  dateTimeService: DateTimeService,
  auditService: AuditService,
  boxIdRepository: BoxIdRepository,
  appConfig: AppConfig,
  cc: ControllerComponents,
  correlationIdAction: CorrelationIdAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging
    with DraftExciseMovementController {

  def submit: Action[NodeSeq] =
    (authAction andThen correlationIdAction andThen xmlParser).async(parse.xml) { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      (for {
        ie815MessageV1 <- getIE815MessageV1(request.ieMessage)
        authorisedErn  <- validateMessage(ie815MessageV1, request.erns)
        clientId       <- retrieveClientIdFromHeader(request)
        maybeBoxId     <- getBoxId(clientId)
        _              <- submitAndHandleError(request, authorisedErn, ie815MessageV1)
        movement       <- getDraftOrSaveNew(maybeBoxId, ie815MessageV1, request)
      } yield (movement, maybeBoxId, ie815MessageV1)).fold[Result](
        failResult => failResult,
        success => {
          val (movement, boxId, ie815MessageV1) = success

          if (appConfig.oldAuditingEnabled) auditService.auditMessage(request.ieMessage).value
          auditService.messageSubmitted(movement, true, ie815MessageV1.correlationId, request)

          Accepted(
            Json.toJson(
              ExciseMovementResponse(
                movement._id,
                boxId,
                movement.localReferenceNumber,
                movement.consignorId,
                movement.consigneeId,
                movement.administrativeReferenceCode,
                None
              )
            )
          )
        }
      )
    }

  private def submitAndHandleError(
    request: ParsedXmlRequest[NodeSeq],
    ern: String,
    message: IE815MessageV1
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, EISSubmissionResponse] =
    EitherT {
      submissionMessageService.submit(request, ern).map {
        case Left(error)     =>
          if (appConfig.oldAuditingEnabled) auditService.auditMessage(message, "Failed to submit") //OLD auditing
          auditService
            .messageSubmittedNoMovement(message, false, message.correlationId, request) //NEW auditing
          Left(
            Status(error.status)(
              Json.toJson(
                EisErrorResponsePresentation(
                  error.dateTime,
                  error.message,
                  error.debugMessage,
                  error.correlationId,
                  error.validatorResults
                )
              )
            )
          )
        case Right(response) =>
          Right(response)
      }
    }

  private def validateMessage(
    message: IE815MessageV1,
    authErns: Set[String]
  ): EitherT[Future, Result, String] =
    EitherT.fromEither(messageValidator.validateDraftMovement(authErns, message).left.map { x =>
      messageValidator.convertErrorToResponse(x, dateTimeService.timestamp())
    })

  private def getDraftOrSaveNew(
    maybeBoxId: Option[String],
    message: IE815MessageV1,
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Movement] =
    EitherT {

      //TODO cannot find by ID as a new ID is generate from the message here
      val unsavedMovement: Movement = createMovementFomMessage(message, maybeBoxId)
      maybeBoxId.foreach(boxIdRepository.save(unsavedMovement.consignorId, _))

      movementMessageService.getDraftMovementOrSaveNew(unsavedMovement).map {
        case Left(result)    =>
          if (appConfig.oldAuditingEnabled) auditService.auditMessage(message, "Failed to Save")
          auditService.messageSubmittedNoMovement(message, true, message.correlationId, request)
          Left(result)
        case Right(movement) =>
          Right(movement)
      }

    }

  private def createMovementFomMessage(message: IE815MessageV1, boxId: Option[String]): Movement = {
    val consignorId: String =
      message.consignorId.getOrElse(
        throw new Exception(s"No Consignor on IE815: ${message.messageIdentifier}")
      )
    Movement(
      boxId,
      message.localReferenceNumber,
      consignorId,
      message.consigneeId,
      None
    )
  }

  private def getBoxId(
    clientId: String
  )(implicit request: ParsedXmlRequest[_]): EitherT[Future, Result, Option[String]] =
    if (appConfig.pushNotificationsEnabled) {
      val clientBoxId = request.headers.get(Constants.XCallbackBoxId)
      EitherT(
        notificationService.getBoxId(clientId, clientBoxId).map(futureValue => futureValue.map(boxId => Some(boxId)))
      )
    } else {
      EitherT.fromEither(Right(None))
    }

  private def getIE815MessageV1(message: IEMessage): EitherT[Future, Result, IE815MessageV1] =
    EitherT.fromEither(message match {
      case x: IE815MessageV1 => Right(x)
      case _                 =>
        logger.warn(
          s"[DraftExciseMovementController] - Message type ${message.messageType} cannot be sent to the draft excise movement endpoint"
        )
        Left(
          BadRequest(
            Json.toJson(
              ErrorResponse(
                dateTimeService.timestamp(),
                "Invalid message type",
                s"Message type ${message.messageType} cannot be sent to the draft excise movement endpoint"
              )
            )
          )
        )
    })

  private def retrieveClientIdFromHeader(implicit request: ParsedXmlRequest[_]): EitherT[Future, Result, String] =
    EitherT.fromOption(
      request.headers.get(Constants.XClientIdHeader),
      BadRequest(
        Json.toJson(
          ErrorResponse(
            dateTimeService.timestamp(),
            s"ClientId error",
            s"Request header is missing ${Constants.XClientIdHeader}"
          )
        )
      )
    )
}
