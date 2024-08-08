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

import cats.data.{EitherT, OptionT}
import cats.implicits._
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ValidateAcceptHeaderAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.MovementIdValidation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, MessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MessageService, MovementService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class GetMessagesController @Inject() (
  authAction: AuthAction,
  validateAcceptHeaderAction: ValidateAcceptHeaderAction,
  movementService: MovementService,
  messageService: MessageService,
  movementIdValidator: MovementIdValidation,
  cc: ControllerComponents,
  emcsUtil: EmcsUtils,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {
  def getMessageForMovement(movementId: String, messageId: String): Action[AnyContent] =
    (
      Action andThen
        validateAcceptHeaderAction andThen
        authAction
    ).async(parse.default) { implicit request =>
      val result = for {
        mvtId    <- validateMovementId(movementId)
        movement <- getMovement(mvtId)
      } yield movement.messages.filter(o => o.messageId.equals(messageId)).toList match {
        case Nil       => messageNotFoundError(messageId)
        case head :: _ =>
          val decodedXml = emcsUtil.decode(head.encodedMessage)
          Ok(xml.XML.loadString(decodedXml))
      }
      result.merge
    }

  def getMessagesForMovement(
    movementId: String,
    updatedSince: Option[String],
    traderType: Option[String]
  ): Action[AnyContent] =
    authAction.async(parse.default) { implicit request: EnrolmentRequest[AnyContent] =>
      val result: EitherT[Future, Result, Result] = for {
        validatedMovementId <- validateMovementId(movementId)
        updatedSince        <- validateUpdatedSince(updatedSince)
        traderType          <- validateTraderType(traderType)
        _                   <- EitherT.right(messageService.updateAllMessages(request.erns))
        movement            <- getMovement(validatedMovementId)
      } yield
        if (getErnsForMovement(movement).intersect(request.erns).isEmpty) {
          logger.warn(s"[GetMessagesController] - Invalid MovementID supplied for ERN")
          Forbidden(
            Json.toJson(
              ErrorResponse(
                dateTimeService.timestamp(),
                "Forbidden",
                "Invalid MovementID supplied for ERN"
              )
            )
          )
        } else {
          Ok(
            Json.toJson(
              filterMessages(request.erns.toSeq, movement, updatedSince, traderType)
            )
          )
        }

      result.merge

    }

  private def filterMessages(
    ern: Seq[String],
    movement: Movement,
    updatedSince: Option[Instant],
    traderType: Option[String]
  ) = {

    val byRecipient = filterMessagesByRecipient(ern, movement)

    val filteredByTraderType = filterMessagesByTraderType(byRecipient, movement, traderType)

    filterMessagesByTime(filteredByTraderType, updatedSince).map { filteredMessage =>
      MessageResponse(
        encodedMessage = filteredMessage.encodedMessage,
        messageType = filteredMessage.messageType,
        recipient = filteredMessage.recipient,
        messageId = filteredMessage.messageId,
        createdOn = filteredMessage.createdOn
      )
    }

  }
  private def messageNotFoundError(messageId: String) = {
    logger.warn(s"[GetMessagesController] - MessageId $messageId was not found in the database")
    NotFound(
      Json.toJson(
        ErrorResponse(
          dateTimeService.timestamp(),
          "No message found for the MovementID provided",
          s"MessageId $messageId was not found in the database"
        )
      )
    )
  }

  private def validateMovementId(movementId: String): EitherT[Future, Result, String] =
    EitherT.fromEither[Future](movementIdValidator.validateMovementId(movementId)).leftMap { x =>
      movementIdValidator.convertErrorToResponse(x, dateTimeService.timestamp())
    }

  private def validateUpdatedSince(updatedSince: Option[String]): EitherT[Future, Result, Option[Instant]] =
    EitherT.fromEither(
      Try(updatedSince.map(Instant.parse(_))).toEither.left.map { _ =>
        logger.warn(s"[GetMessagesController] - Invalid date format provided in the updatedSince query parameter")
        BadRequest(
          Json.toJson(
            ErrorResponse(
              dateTimeService.timestamp(),
              "Invalid date format provided in the updatedSince query parameter",
              "Date format should be like '2020-11-15T17:02:34.00Z'"
            )
          )
        )
      }
    )

  private def filterMessagesByTraderType(
    messages: Seq[Message],
    movement: Movement,
    traderType: Option[String]
  ): Seq[Message] =
    traderType.fold[Seq[Message]](messages)(trader =>
      if (trader.equalsIgnoreCase("consignor")) {
        messages.filter(o => o.recipient.equals(movement.consignorId))
      } else {
        messages.filter(o => movement.consigneeId.contains(o.recipient))
      }
    )

  private def filterMessagesByRecipient(recipient: Seq[String], movement: Movement): Seq[Message] =
    movement.messages.filter(message => recipient.contains(message.recipient))

  private def validateTraderType(traderType: Option[String]): EitherT[Future, Result, Option[String]] =
    EitherT {
      Future.successful(
        traderType match {
          case None        => Right(None)
          case Some(value) =>
            if (value.equalsIgnoreCase("consignor") || value.equalsIgnoreCase("consignee")) {
              Right(traderType)
            } else {
              Left {
                logger.warn(s"[GetMessagesController] - Invalid traderType passed in")
                BadRequest(
                  Json.toJson(
                    ErrorResponse(
                      dateTimeService.timestamp(),
                      "Invalid traderType passed in",
                      "traderType should be consignor or consignee"
                    )
                  )
                )
              }
            }
        }
      )
    }

  private def getMovement(id: String): EitherT[Future, Result, Movement] =
    OptionT(movementService.getMovementById(id)).toRightF(
      Future.successful(
        NotFound(
          Json.toJson(
            ErrorResponse(
              dateTimeService.timestamp(),
              "Movement not found",
              s"Movement $id could not be found"
            )
          )
        )
      )
    )

  private def filterMessagesByTime(messages: Seq[Message], updatedSince: Option[Instant]): Seq[Message] =
    updatedSince.fold[Seq[Message]](messages)(a =>
      messages.filter(o => o.createdOn.isAfter(a) || o.createdOn.equals(a))
    )

  private def getErnsForMovement(movement: Movement): Set[String] = {
    val messageRecipients = movement.messages.map(_.recipient)
    Set(Some(movement.consignorId), movement.consigneeId, messageRecipients).flatten
  }

}
