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

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

sealed trait MessageValidation {
  def errorMessage: String
}

object MessageValidation {
  val arc: String = "ARC"
  val consignor: String = Consignor.name
  val consignee: String = Consignee.name
  // TODO: Use these entry points in the DraftMovementController and SubmitMessageController
  def validateDraftMovement(authorisedErns: Set[String], ie815: IE815Message): Either[MessageValidation, String] = IE815MessageValidator(ie815).validate(authorisedErns)
  def validateSubmittedMessage(authorisedErns: Set[String], movement: Movement, message: IEMessage): Either[MessageValidation, String] = {
    val validator: MovementMessageValidator = message match {
      case m: IE810Message => IE810MessageValidator(m, movement)
      case m: IE813Message => IE813MessageValidator(m, movement)
      case m: IE818Message => IE818MessageValidator(m, movement)
      case m: IE819Message => IE819MessageValidator(m, movement)
      case m: IE837Message => IE837MessageValidator(m, movement)
      case m: IE871Message => IE871MessageValidator(m, movement)
    }
    validator.validate(authorisedErns)
  }
}

// TODO: When this is returned, convert to BAD_REQUEST in controllers
abstract sealed class MessageDoesNotMatchMovement(val field: String) extends MessageValidation {
  override def errorMessage: String = s"The $field in the message does not match the $field in the movement"
}

// TODO: When this is returned, convert to FORBIDDEN in controllers
abstract class MessageIdentifierIsUnauthorised(val identifier: String) extends MessageValidation {
  override def errorMessage: String = s"The $identifier is not authorised to submit this message for the movement"
}

// TODO: Remove this comment, but everything else in here is private as the functionality is provided in the public entry points and only what is needed externally is exposed
private case object ArcDoesNotMatch extends MessageDoesNotMatchMovement(MessageValidation.arc)

private case object ConsignorDoesNotMatch extends MessageDoesNotMatchMovement(MessageValidation.consignor)

private case object ConsigneeDoesNotMatch extends MessageDoesNotMatchMovement(MessageValidation.consignee)

private case object ConsignorIsUnauthorised extends MessageIdentifierIsUnauthorised(MessageValidation.consignor)

private case object ConsigneeIsUnauthorised extends MessageIdentifierIsUnauthorised(MessageValidation.consignee)

private sealed trait MessageValidator {
  val message: IEMessage

  def validate(authorisedErns: Set[String]): Either[MessageValidation, String]
}

private sealed trait MovementValidator {
  this: MessageValidator =>
  val movement: Movement
}

private abstract class MovementMessageValidator(override val message: IEMessage, override val movement: Movement) extends MovementValidator with MessageValidator

private case class IE815MessageValidator(override val message: IE815Message) extends MessageValidator {
  override def validate(authorisedErns: Set[String]): Either[MessageValidation, String] =
    Either.cond(authorisedErns.contains(message.consignorId), message.consignorId, ConsignorIsUnauthorised)
}

private case class IE810MessageValidator(override val message: IE810Message, override val movement: Movement) extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidation, String] = {
    if (authorisedErns.contains(movement.consignorId)) {
      Either.cond(message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
        movement.consignorId, ArcDoesNotMatch)
    } else {
      Left(ConsignorIsUnauthorised)
    }
  }
}

private case class IE813MessageValidator(override val message: IE813Message, override val movement: Movement) extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidation, String] = {
    if (authorisedErns.contains(movement.consignorId)) {
      Either.cond(message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
        movement.consignorId, ArcDoesNotMatch)
    } else {
      Left(ConsignorIsUnauthorised)
    }
  }
}

private case class IE818MessageValidator(override val message: IE818Message, override val movement: Movement) extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidation, String] = {
    message.consigneeId match {
      // TODO deal with the None case??
      case Some(consignee) =>
        if (authorisedErns.contains(consignee)) {
          if (movement.consigneeId.contains(consignee)) {
            Either.cond(message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              consignee, ArcDoesNotMatch)
          }
          else {
            Left(ConsigneeDoesNotMatch)
          }
        } else {
          Left(ConsigneeIsUnauthorised)
        }
    }
  }
}

private case class IE819MessageValidator(override val message: IE819Message, override val movement: Movement) extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidation, String] = {
    message.consigneeId match {
      // TODO deal with the None case??
      case Some(consignee) =>
        if (authorisedErns.contains(consignee)) {
          if (movement.consigneeId.contains(consignee)) {
            Either.cond(message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              consignee, ArcDoesNotMatch)
          }
          else {
            Left(ConsigneeDoesNotMatch)
          }
        } else {
          Left(ConsigneeIsUnauthorised)
        }
    }
  }
}
private case class SubmitterDetails(ern: Option[String], movementMatcher: (Movement, String) => Boolean, doesNotMatch: MessageDoesNotMatchMovement, notAuthorised: MessageIdentifierIsUnauthorised)

private case class IE837MessageValidator(override val message: IE837Message, override val movement: Movement) extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidation, String] = {
    getSubmitter match {
      // TODO deal with the None case??
      case SubmitterDetails(Some(ern), movementMatcher, noMatch, unauthorised) =>
        if (authorisedErns.contains(ern)) {
          if (movementMatcher(movement, ern)) {
            Either.cond(message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              ern, ArcDoesNotMatch)
          } else {
            Left(noMatch)
          }
        } else {
          Left(unauthorised)
        }

    }
  }

  private def getSubmitter = {
    message.submitter match {
      case Consignor => SubmitterDetails(message.consignorId, (movement, identifier) => movement.consignorId == identifier, ConsignorDoesNotMatch, ConsignorIsUnauthorised)
      case Consignee => SubmitterDetails(message.consigneeId, (movement, identifier) => movement.consigneeId.contains(identifier), ConsigneeDoesNotMatch, ConsigneeIsUnauthorised)
    }
  }
}

private case class IE871MessageValidator(override val message: IE871Message, override val movement: Movement) extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidation, String] = {
    getSubmitter match {
      // TODO deal with the None case??
      case SubmitterDetails(Some(ern), movementMatcher, noMatch, unauthorised) =>
        if (authorisedErns.contains(ern)) {
          if (movementMatcher(movement, ern)) {
            Either.cond(message.administrativeReferenceCode.contains(movement.administrativeReferenceCode),
              ern, ArcDoesNotMatch)
          } else {
            Left(noMatch)
          }
        } else {
          Left(unauthorised)
        }

    }
  }
  private def getSubmitter = {
    message.submitter match {
      case Consignor => SubmitterDetails(message.consignorId, (movement, identifier) => movement.consignorId == identifier, ConsignorDoesNotMatch, ConsignorIsUnauthorised)
      case Consignee => SubmitterDetails(message.consigneeId, (movement, identifier) => movement.consigneeId.contains(identifier), ConsigneeDoesNotMatch, ConsigneeIsUnauthorised)
    }
  }
}