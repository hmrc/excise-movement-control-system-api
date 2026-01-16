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

import play.api.mvc.Result
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2.IE815MessageV2
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import java.time.Instant

sealed trait MessageValidationResponse {
  def errorMessage: String
}

trait MessageValidation {

  def validateDraftMovement(
    authorisedErns: Set[String],
    ie815: IE815MessageV1
  ): Either[MessageValidationResponse, String] =
    IE815MessageV1Validator(ie815).validate(authorisedErns)

  def validateDraftMovement(
    authorisedErns: Set[String],
    ie815: IE815MessageV2
  ): Either[MessageValidationResponse, String] =
    IE815MessageV2Validator(ie815).validate(authorisedErns)

  def validateSubmittedMessage(
    authorisedErns: Set[String],
    movement: Movement,
    message: IEMessage
  ): Either[MessageValidationResponse, String]

  def convertErrorToResponse(error: MessageValidationResponse, timestamp: Instant): Result
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

private trait MessageValidator {
  val message: IEMessage

  def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String]
}

private[validation] sealed trait MovementValidator {
  this: MessageValidator =>
  val movement: Movement
}

private[validation] abstract class MovementMessageValidator(
  override val message: IEMessage,
  override val movement: Movement
) extends MovementValidator
    with MessageValidator

case class InvalidMessageTypeValidator(override val message: IEMessage, override val movement: Movement)
    extends MovementMessageValidator(message, movement) {
  override def validate(authorisedErns: Set[String]): Either[MessageValidationResponse, String] =
    Left(MessageTypeInvalid(message.messageType))

}

case class SubmitterDetails(
  ern: Option[String],
  movementMatcher: (Movement, String) => Boolean,
  doesNotMatch: MessageDoesNotMatchMovement,
  notAuthorised: MessageIdentifierIsUnauthorised
)
