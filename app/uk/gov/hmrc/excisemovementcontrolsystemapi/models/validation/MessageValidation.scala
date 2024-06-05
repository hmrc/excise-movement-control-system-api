/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, Forbidden}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import java.time.Instant

sealed trait MessageValidationResponse {
  def errorMessage: String
}

case class MessageValidation() {

  def validateDraftMovement(
    authorisedErns: Set[String],
    ie815: IE815Message
  ): Either[MessageValidationResponse, String] =
    IE815MessageValidator(ie815).validate(authorisedErns)

  def validateSubmittedMessage(
    authorisedErns: Set[String],
    movement: Movement,
    message: IEMessage
  ): Either[MessageValidationResponse, String] = {
    val validator: MovementMessageValidator = message match {
      case m: IE810Message => IE810MessageValidator(m, movement)
      case m: IE813Message => IE813MessageValidator(m, movement)
      case m: IE818Message => IE818MessageValidator(m, movement)
      case m: IE819Message => IE819MessageValidator(m, movement)
      case m: IE837Message => IE837MessageValidator(m, movement)
      case m: IE871Message => IE871MessageValidator(m, movement)
      case m: IEMessage    => InvalidMessageTypeValidator(m, movement)
    }
    validator.validate(authorisedErns)
  }

  def convertErrorToResponse(error: MessageValidationResponse, timestamp: Instant): Result =
    error match {
      case x: MessageDoesNotMatchMovement =>
        BadRequest(
          Json.toJson(
            ErrorResponse(timestamp, "Message does not match movement", x.errorMessage)
          )
        )

      case x: MessageMissingKeyInformation =>
        BadRequest(
          Json.toJson(
            ErrorResponse(timestamp, "Message missing key information", x.errorMessage)
          )
        )

      case x: MessageTypeInvalid =>
        BadRequest(
          Json.toJson(
            ErrorResponse(timestamp, "Message type is invalid", x.errorMessage)
          )
        )

      case x: MessageIdentifierIsUnauthorised =>
        Forbidden(
          Json.toJson(
            ErrorResponse(timestamp, "Message cannot be sent", x.errorMessage)
          )
        )
    }
}

object MessageValidation {
  val arc: String       = "ARC"
  val consignor: String = Consignor.name
  val consignee: String = Consignee.name
}

abstract class MessageDoesNotMatchMovement(val field: String) extends MessageValidationResponse {
  override def errorMessage: String = s"The $field in the message does not match the $field in the movement"
}

abstract class MessageMissingKeyInformation(val field: String) extends MessageValidationResponse {
  override def errorMessage: String = s"The $field in the message should not be empty"

}

case class MessageTypeInvalid(messageType: String) extends MessageValidationResponse {
  override def errorMessage: String = s"The supplied message type $messageType is not supported"
}

abstract class MessageIdentifierIsUnauthorised(val identifier: String) extends MessageValidationResponse {
  override def errorMessage: String = s"The $identifier is not authorised to submit this message for the movement"
}

private case object ArcDoesNotMatch extends MessageDoesNotMatchMovement(MessageValidation.arc)

private case object ConsignorDoesNotMatch extends MessageDoesNotMatchMovement(MessageValidation.consignor)

private case object ConsigneeDoesNotMatch extends MessageDoesNotMatchMovement(MessageValidation.consignee)

private case object ConsigneeIsMissing extends MessageMissingKeyInformation(MessageValidation.consignee)

private case object ConsignorIsMissing extends MessageMissingKeyInformation(MessageValidation.consignor)

private case object ConsignorIsUnauthorised extends MessageIdentifierIsUnauthorised(MessageValidation.consignor)

private case object ConsigneeIsUnauthorised extends MessageIdentifierIsUnauthorised(MessageValidation.consignee)

private sealed trait MessageValidator {
  val message: IEMessage

  def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String]
}

private sealed trait MovementValidator {
  this: MessageValidator =>
  val movement: Movement
}

private abstract class MovementMessageValidator(override val message: IEMessage, override val movement: Movement)
    extends MovementValidator
    with MessageValidator

private case class IE815MessageValidator(override val message: IE815Message) extends MessageValidator {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    Either.cond(authorisedErns.contains(message.consignorId), message.consignorId, ConsignorIsUnauthorised)
}

private case class IE810MessageValidator(override val message: IE810Message, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    if (authorisedErns.contains(movement.consignorId)) {
      Either.cond(
        message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
        movement.consignorId,
        ArcDoesNotMatch
      )
    } else {
      Left(ConsignorIsUnauthorised)
    }
}

private case class IE813MessageValidator(override val message: IE813Message, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    if (authorisedErns.contains(movement.consignorId)) {
      Either.cond(
        message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
        movement.consignorId,
        ArcDoesNotMatch
      )
    } else {
      Left(ConsignorIsUnauthorised)
    }
}

private case class IE818MessageValidator(override val message: IE818Message, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    message.consigneeId match {

      case Some(consignee) =>
        if (authorisedErns.contains(consignee)) {
          if (movement.consigneeId.contains(consignee)) {
            Either.cond(
              message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              consignee,
              ArcDoesNotMatch
            )
          } else {
            Left(ConsigneeDoesNotMatch)
          }
        } else {
          Left(ConsigneeIsUnauthorised)
        }

      case _ => Left(ConsigneeIsMissing)
    }
}

private case class IE819MessageValidator(override val message: IE819Message, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    message.consigneeId match {

      case Some(consignee) =>
        if (authorisedErns.contains(consignee)) {
          if (movement.consigneeId.contains(consignee)) {
            Either.cond(
              message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              consignee,
              ArcDoesNotMatch
            )
          } else {
            Left(ConsigneeDoesNotMatch)
          }
        } else {
          Left(ConsigneeIsUnauthorised)
        }

      case _ => Left(ConsigneeIsMissing)
    }
}

private case class SubmitterDetails(
  ern: Option[String],
  movementMatcher: (Movement, String) => Boolean,
  doesNotMatch: MessageDoesNotMatchMovement,
  notAuthorised: MessageIdentifierIsUnauthorised
)

private case class IE837MessageValidator(override val message: IE837Message, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    getSubmitter match {
      case SubmitterDetails(Some(ern), movementMatcher, noMatch, unauthorised) if ern != "" =>
        if (authorisedErns.contains(ern)) {
          if (movementMatcher(movement, ern)) {
            Either.cond(
              message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              ern,
              ArcDoesNotMatch
            )
          } else {
            Left(noMatch)
          }
        } else {
          Left(unauthorised)
        }

      case _ =>
        //This is the case where SubmitterDetails(None, _, _, _) is returned
        message.submitter match {
          case Consignor => Left(ConsignorIsMissing)
          case Consignee => Left(ConsigneeIsMissing)
        }
    }

  private def getSubmitter =
    message.submitter match {
      case Consignor =>
        SubmitterDetails(
          message.consignorId,
          (movement, identifier) => movement.consignorId == identifier,
          ConsignorDoesNotMatch,
          ConsignorIsUnauthorised
        )

      case Consignee =>
        SubmitterDetails(
          message.consigneeId,
          (movement, identifier) => movement.consigneeId.contains(identifier),
          ConsigneeDoesNotMatch,
          ConsigneeIsUnauthorised
        )
    }
}

private case class IE871MessageValidator(override val message: IE871Message, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    getSubmitter match {
      case SubmitterDetails(Some(ern), movementMatcher, noMatch, unauthorised) if ern != "" =>
        if (authorisedErns.contains(ern)) {
          if (movementMatcher(movement, ern)) {
            Either.cond(
              message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              ern,
              ArcDoesNotMatch
            )
          } else {
            Left(noMatch)
          }
        } else {
          Left(unauthorised)
        }

      case _ =>
        //This is the case where SubmitterDetails(None, _, _, _) is returned
        message.submitter match {
          case Consignor => Left(ConsignorIsMissing)
          case Consignee => Left(ConsigneeIsMissing)
        }
    }

  private def getSubmitter =
    message.submitter match {
      case Consignor =>
        SubmitterDetails(
          message.consignorId,
          (movement, identifier) => movement.consignorId == identifier,
          ConsignorDoesNotMatch,
          ConsignorIsUnauthorised
        )

      case Consignee =>
        SubmitterDetails(
          message.consigneeId,
          (movement, identifier) => movement.consigneeId.contains(identifier),
          ConsigneeDoesNotMatch,
          ConsigneeIsUnauthorised
        )
    }
}

private case class InvalidMessageTypeValidator(override val message: IEMessage, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    Left(MessageTypeInvalid(message.messageType))

}
