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
    extends BackendController(cc) {
  def getMessageForMovement(movementId: String, messageId: String): Action[AnyContent] =
    (
      Action andThen
        validateAcceptHeaderAction andThen
        authAction
    ).async(parse.default) { implicit request =>
      //todo EMCS-529: do we need to validate messageId here? This is the messageIdentifier
      // of the message abd according to the xsd this is not a UUID and can be
      // of any char between 1 and 44 char length
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
  ): Action[AnyContent]                               =
    authAction.async(parse.default) { implicit request: EnrolmentRequest[AnyContent] =>
      val result: EitherT[Future, Result, Result] = for {
        validatedMovementId <- validateMovementId(movementId)
        updatedSince        <- validateUpdatedSince(updatedSince)
        traderType          <- validateTraderType(traderType)
        _                   <- EitherT.right(messageService.updateAllMessages(request.erns))
        movement            <- getMovement(validatedMovementId)
      } yield
        if (getErnsForMovement(movement).intersect(request.erns).isEmpty) {
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
              filterMessages(movement, updatedSince, traderType)
            )
          )
        }

      result.merge

    }

  private def filterMessages(movement: Movement, updatedSince: Option[Instant], traderType: Option[String]) = {
    val filteredByTraderType = filterMessagesByTraderType(movement, traderType)
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
  private def messageNotFoundError(messageId: String) =
    NotFound(
      Json.toJson(
        ErrorResponse(
          dateTimeService.timestamp(),
          "No message found for the MovementID provided",
          s"MessageId $messageId was not found in the database"
        )
      )
    )

  private def validateMovementId(movementId: String): EitherT[Future, Result, String] =
    EitherT.fromEither[Future](movementIdValidator.validateMovementId(movementId)).leftMap { x =>
      movementIdValidator.convertErrorToResponse(x, dateTimeService.timestamp())
    }

  private def validateUpdatedSince(updatedSince: Option[String]): EitherT[Future, Result, Option[Instant]] =
    EitherT.fromEither(
      Try(updatedSince.map(Instant.parse(_))).toEither.left.map(_ =>
        BadRequest(
          Json.toJson(
            ErrorResponse(
              dateTimeService.timestamp(),
              "Invalid date format provided in the updatedSince query parameter",
              "Date format should be like '2020-11-15T17:02:34.00Z'"
            )
          )
        )
      )
    )

  private def filterMessagesByTraderType(movement: Movement, traderType: Option[String]): Seq[Message] =
    traderType.fold[Seq[Message]](movement.messages)(trader =>
      if (trader.equalsIgnoreCase("consignor")) {
        movement.messages.filter(o => o.recipient.equals(movement.consignorId))
      } else {
        movement.messages.filter(o => movement.consigneeId.contains(o.recipient))
      }
    )

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
