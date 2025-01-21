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

import cats.data.{EitherT, NonEmptySeq}
import play.api.Logging
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends Auditing
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

      val event = AuditEventFactory.createMessageSubmitted(movement, submittedToCore, correlationId, request)

      auditConnector.sendExplicitAudit("MessageSubmitted", event)
    }

  def messageSubmittedNoMovement(
    message: IE815Message,
    submittedToCore: Boolean,
    correlationId: Option[String],
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {

      val event = AuditEventFactory.createMessageSubmittedNoMovement(
        message,
        submittedToCore,
        correlationId,
        request
      )

      auditConnector.sendExplicitAudit("MessageSubmitted", event)
    }

  private def auditMessage(message: IEMessage, failureOpt: Option[String])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit]    =
    EitherT {

      auditConnector.sendExtendedEvent(AuditEventFactory.createMessageAuditEvent(message, failureOpt)).map {
        case f: AuditResult.Failure => Right(logger.error(f.msg))
        case _                      => Right(())
      }
    }
  def getInformationForGetMovements(
    request: GetMovementsParametersAuditInfo,
    response: GetMovementsResponseAuditInfo,
    enrolmentRequest: EnrolmentRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = AuditEventFactory.createGetMovementsDetails(
        request,
        response,
        enrolmentRequest.userDetails,
        NonEmptySeq(enrolmentRequest.erns.head, enrolmentRequest.erns.tail.toList)
      )
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

  def getInformationForGetSpecificMovement(
    request: GetSpecificMovementRequestAuditInfo,
    userDetails: UserDetails,
    authExciseNumber: NonEmptySeq[String]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = AuditEventFactory.createGetSpecificMovementDetails(request, userDetails, authExciseNumber)
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

  def getInformationForGetMessages(
    request: GetMessagesRequestAuditInfo,
    response: GetMessagesResponseAuditInfo,
    userDetails: UserDetails,
    authExciseNumber: NonEmptySeq[String]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      val event = AuditEventFactory.createGetMessagesAuditInfo(request, response, userDetails, authExciseNumber)
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

  def getInformationForGetSpecificMessage(
    request: GetSpecificMessageRequestAuditInfo,
    response: GetSpecificMessageResponseAuditInfo,
    userDetails: UserDetails,
    authExciseNumber: NonEmptySeq[String]
  )(implicit hc: HeaderCarrier): Unit =
    if (appConfig.newAuditingEnabled) {
      //TODO: Lets revisit the getInformationFor... to use the AuditFactory, but in a way that removes the complex construction out of the controlllers (see the submittedMessage methods)
      val event = GetSpecificMessageAuditInfo(
        request = request,
        response = response,
        userDetails = userDetails,
        authExciseNumber = authExciseNumber
      )
      auditConnector.sendExplicitAudit("GetInformation", event)
    }

}
