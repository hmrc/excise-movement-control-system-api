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
import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.mvc.Result
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.{AuditEventFactory, Auditing}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class AuditServiceImpl @Inject() (auditConnector: AuditConnector, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends Auditing
    with AuditService
    with Logging {

  def auditMessage(message: IEMessage)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit] =
    auditMessage(message, None)
  def auditMessage(message: IEMessage, failureReason: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit]                                                                =
    auditMessage(message, Some(failureReason))

  def messageSubmitted(
    message: IEMessage,
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: String,
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit] =
    if (appConfig.newAuditingEnabled) {
      val event =
        AuditEventFactory.createMessageSubmitted(message, movement, submittedToCore, correlationId, request)
      auditEvent(event)
    } else EitherT.fromEither(Right(()))

  def messageSubmittedWithoutMovement(
    message: IE815Message,
    submittedToCore: Boolean,
    correlationId: String,
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit] =
    if (appConfig.newAuditingEnabled) {
      val event =
        AuditEventFactory.createMessageSubmitted(message, submittedToCore, correlationId, request)
      auditEvent(event)
    } else EitherT.fromEither(Right(()))

  private def auditEvent(event: ExtendedDataEvent)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit] =
    EitherT {
      auditConnector.sendExtendedEvent(event).map {
        case f: AuditResult.Failure => Right(logger.error(f.msg))
        case _                      => Right(())
      }
    }

  private def auditMessage(message: IEMessage, failureOpt: Option[String])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit] =
    EitherT {
      auditConnector.sendExtendedEvent(AuditEventFactory.createMessageAuditEvent(message, failureOpt)).map {
        case f: AuditResult.Failure => Right(logger.error(f.msg))
        case _                      => Right(())
      }
    }
}

//TODO: Look at unwrapping the EitherT's
@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {
  def auditMessage(message: IEMessage)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit]
  def auditMessage(message: IEMessage, failureReason: String)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit]
  def messageSubmitted(
    message: IEMessage,
    movement: Movement,
    submittedToCore: Boolean,
    correlationId: String,
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit]
  def messageSubmittedWithoutMovement(
    message: IE815Message,
    submittedToCore: Boolean,
    correlationId: String,
    request: ParsedXmlRequest[NodeSeq]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit]
}
