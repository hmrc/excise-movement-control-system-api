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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import cats.data.EitherT
import play.api.Logging
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector, appConfig: AppConfig, factory: AuditEventFactory)(implicit
  ec: ExecutionContext
) extends Auditing
    with Logging {

  def auditMessage(message: IEMessage)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit] =
    auditMessage(message, None)
  def auditMessage(message: IEMessage, failureReason: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit]                                                                =
    auditMessage(message, Some(failureReason))

  def messageSubmitted(
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {

      val event = factory.createMessageSubmitted(movement, submittedToCore, correlationId, request)

      auditConnector.sendExplicitAudit("MessageSubmitted", event)
    }

  def messageSubmittedNoMovement(
    message: IE815Message,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {

      val event = factory.createMessageSubmittedNoMovement(
        message,
        submittedToCore,
        correlationId,
        request
      )

      auditConnector.sendExplicitAudit("MessageSubmitted", event)
    }

  private def auditMessage(message: IEMessage, failureOpt: Option[String])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit] =
    EitherT {

      auditConnector.sendExtendedEvent(factory.createMessageAuditEvent(message, failureOpt)).map {
        case f: AuditResult.Failure => Right(logger.error(f.msg))
        case _                      => Right(())
      }
    }

  def getInformationForGetMovements(
    movementFilter: MovementFilter,
    movements: Seq[Movement],
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = factory.createGetMovementsDetails(
        movementFilter,
        movements,
        request
      )
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

  def getInformationForGetSpecificMovement(
    movementId: String,
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = factory.createGetSpecificMovementDetails(movementId, request)
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

  def getInformationForGetMessages(
    messages: Seq[Message],
    movement: Movement,
    updatedSince: Option[String],
    traderType: Option[String],
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = factory.createGetMessagesAuditInfo(messages, movement, updatedSince, traderType, request)
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

  def getInformationForGetSpecificMessage(
    movement: Movement,
    message: Message,
    request: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = factory.createGetSpecificMessageAuditInfo(movement, message, request)
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

  def messageProcessingSuccess(
    ern: String,
    response: GetMessagesResponse,
    batchId: String,
    jobId: Option[String]
  )(implicit hc: HeaderCarrier): Unit = {
    val messageProcessingSuccessAuditInfo = factory.createMessageProcessingSuccessAuditInfo(
      ern,
      response,
      batchId,
      jobId
    )
    auditConnector.sendExplicitAudit("MessageProcessing", messageProcessingSuccessAuditInfo)
  }

  def messageProcessingFailure(ern: String, failureReason: String, batchId: String, jobId: Option[String])(implicit
    hc: HeaderCarrier
  ): Unit = {
    val messageProcessingFailureAuditInfo =
      factory.createMessageProcessingFailureAuditInfo(ern, failureReason, batchId, jobId)

    auditConnector.sendExplicitAudit("MessageProcessing", messageProcessingFailureAuditInfo)

  }
  def messageAcknowledged(ern: String, batchId: String, jobId: Option[String], recordsAffected: Int)(implicit
    hc: HeaderCarrier
  ): Unit                             =
    if (appConfig.newAuditingEnabled) {
      val event = factory.createMessageAcknowledgedEvent(ern, batchId, jobId, recordsAffected)
      auditConnector.sendExplicitAudit("MessageAcknowledged", event)
    }

  def messageNotAcknowledged(ern: String, batchId: String, jobId: Option[String], failureReason: String)(implicit
    hc: HeaderCarrier
  ): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = factory.createMessageNotAcknowledgedEvent(ern, batchId, jobId, failureReason)
      auditConnector.sendExplicitAudit("MessageAcknowledged", event)
    }
}
