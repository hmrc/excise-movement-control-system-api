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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import cats.data.EitherT
import play.api.Logging
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v1.IE815MessageV1
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2.IE815MessageV2
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.xml.NodeSeq

trait AuditService {

  // old auditing
  def auditMessage(message: IEMessage)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit]
  def auditMessage(message: IEMessage, failureReason: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit]

  def messageSubmitted(
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): Unit

  def messageSubmittedNoMovement(
    message: IE815MessageV1,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): Unit

  def messageSubmittedNoMovement(
    message: IE815MessageV2,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): Unit

  def getInformationForGetMovements(
    movementFilter: MovementFilter,
    movements: Seq[Movement],
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit

  def getInformationForGetSpecificMovement(
    movementId: String,
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit

  def getInformationForGetMessages(
    messages: Seq[Message],
    movement: Movement,
    updatedSince: Option[String],
    traderType: Option[String],
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit

  def getInformationForGetSpecificMessage(
    movement: Movement,
    message: Message,
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit

  def messageProcessingSuccess(
    ern: String,
    response: GetMessagesResponse,
    batchId: String,
    jobId: Option[String]
  )(implicit hc: HeaderCarrier): Unit

  def messageProcessingFailure(ern: String, failureReason: String, batchId: String, jobId: Option[String])(implicit
    hc: HeaderCarrier
  ): Unit

  def movementSavedSuccess(
    movement: Movement,
    batchId: Option[String],
    jobId: Option[String],
    newMessages: Seq[Message]
  )(implicit hc: HeaderCarrier): Unit

  def movementSavedFailure(
    movement: Movement,
    failureReason: String,
    batchId: Option[String],
    jobId: Option[String],
    messagesToBeAdded: Seq[Message]
  )(implicit hc: HeaderCarrier): Unit

  def messageAcknowledged(ern: String, batchId: String, jobId: Option[String], recordsAffected: Int)(implicit
    hc: HeaderCarrier
  ): Unit

  def messageNotAcknowledged(ern: String, batchId: String, jobId: Option[String], failureReason: String)(implicit
    hc: HeaderCarrier
  ): Unit
}
