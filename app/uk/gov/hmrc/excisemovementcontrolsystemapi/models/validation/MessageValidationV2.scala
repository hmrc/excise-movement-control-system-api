/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2.{IE837MessageV2, IE871MessageV2}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import java.time.Instant

case class MessageValidationV2() extends MessageValidation {

  def validateSubmittedMessage(
    authorisedErns: Set[String],
    movement: Movement,
    message: IEMessage
  ): Either[MessageValidationResponse, String] = {
    val validator: MovementMessageValidator = message match {
      case m: IE810MessageV2 => IE810MessageV2Validator(m, movement)
      case m: IE813MessageV2 => IE813MessageV2Validator(m, movement)
      case m: IE818MessageV2 => IE818MessageV2Validator(m, movement)
      case m: IE819MessageV2 => IE819MessageV2Validator(m, movement)
      case m: IE837MessageV2 => IE837MessageV2Validator(m, movement)
      case m: IE871MessageV2 => IE871MessageV2Validator(m, movement)
      case m: IEMessage      => InvalidMessageTypeValidator(m, movement)
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

private case class IE815MessageV2Validator(override val message: IE815MessageV2) extends MessageValidator {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] = {
    val consignorId: String =
      message.consignorId.getOrElse(
        throw new Exception(s"No Consignor on IE815: ${message.messageIdentifier}")
      )
    Either.cond(authorisedErns.contains(consignorId), consignorId, ConsignorIsUnauthorised)
  }
}

private case class IE810MessageV2Validator(override val message: IE810MessageV2, override val movement: Movement)
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

private case class IE813MessageV2Validator(override val message: IE813MessageV2, override val movement: Movement)
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

private case class IE818MessageV2Validator(override val message: IE818MessageV2, override val movement: Movement)
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

private case class IE819MessageV2Validator(override val message: IE819MessageV2, override val movement: Movement)
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

private case class IE837MessageV2Validator(override val message: IE837MessageV2, override val movement: Movement)
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

private case class IE871MessageV2Validator(override val message: IE871MessageV2, override val movement: Movement)
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
