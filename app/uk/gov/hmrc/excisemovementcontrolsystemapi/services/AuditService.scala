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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.Auditing
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditServiceImpl @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext)
    extends Auditing
    with AuditService
    with Logging {

  def auditMessage(message: IEMessage)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit] =
    auditMessage(message, None)
  def auditMessage(message: IEMessage, failureReason: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit]                                                                = auditMessage(message, Some(failureReason))

  private def auditMessage(message: IEMessage, failureOpt: Option[String])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit] =
    EitherT {
      auditConnector.sendExtendedEvent(AuditEventFactory.createAuditEvent(message, failureOpt)).map {
        case f: AuditResult.Failure => Right(logger.error(f.msg))
        case _                      => Right(())
      }
    }
}

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {
  def auditMessage(message: IEMessage)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit]
  def auditMessage(message: IEMessage, failureReason: String)(implicit hc: HeaderCarrier): EitherT[Future, Result, Unit]
}
