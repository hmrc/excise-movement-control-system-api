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
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISSubmissionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.BoxIdRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementController @Inject() (
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
  correlationIdService: CorrelationIdService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submit: Action[NodeSeq] =
    (authAction andThen xmlParser).async(parse.xml) { implicit request =>
      implicit val hc: HeaderCarrier = correlationIdService.getOrCreateCorrelationId(request)
      (for {
        ie815Message  <- getIe815Message(request.ieMessage)
        authorisedErn <- validateMessage(ie815Message, request.erns)
        clientId      <- retrieveClientIdFromHeader(request)
        boxId         <- getBoxId(clientId)
        result        <- submitAndHandleError(request, authorisedErn, ie815Message)
        movement      <- saveMovement(boxId, ie815Message, result, request)
      } yield (result, movement, boxId)).fold[Result](
        failResult => failResult,
        success => {
          val (response, movement, boxId) = success

          auditService.auditMessage(request.ieMessage).value
          auditService.messageSubmitted(request.ieMessage, movement, true, response.emcsCorrelationId, request)

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
    message: IE815Message
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, EISSubmissionResponse] =
    EitherT {
      submissionMessageService.submit(request, ern).map {
        case Left(error)     =>
          auditService.auditMessage(message, "Failed to submit") //OLD auditing
          auditService.messageSubmittedNoMovement(message, false, error.correlationId, request) //NEW auditing
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
    message: IE815Message,
    authErns: Set[String]
  ): EitherT[Future, Result, String] =
    EitherT.fromEither(messageValidator.validateDraftMovement(authErns, message).left.map { x =>
      messageValidator.convertErrorToResponse(x, dateTimeService.timestamp())
    })

  private def saveMovement(
    boxId: Option[String],
    message: IE815Message,
    submission: EISSubmissionResponse,
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Movement] = EitherT {

    val newMovement: Movement = createMovementFomMessage(message, boxId)
    boxId.map(boxIdRepository.save(newMovement.consignorId, _)) //TODO recover future in failure case

    movementMessageService.saveNewMovement(newMovement).map {
      case Left(result)    =>
        auditService.auditMessage(message, "Failed to Save")
        auditService.messageSubmittedNoMovement(message, true, submission.emcsCorrelationId, request)
        Left(result)
      case Right(movement) =>
        Right(movement)
    }

  }

  private def createMovementFomMessage(message: IE815Message, boxId: Option[String]): Movement =
    Movement(
      boxId,
      message.localReferenceNumber,
      message.consignorId,
      message.consigneeId,
      None
    )

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

  private def getIe815Message(message: IEMessage): EitherT[Future, Result, IE815Message] =
    EitherT.fromEither(message match {
      case x: IE815Message => Right(x)
      case _               =>
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
