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

import org.apache.pekko.Done
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService.UpdateOutcome
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.Future

trait MessageService {

  def updateAllMessages(erns: Set[String])(implicit hc: HeaderCarrier): Future[Done]

  def updateMessages(ern: String, lastRetrieved: Option[Instant], jobId: Option[String] = None)(implicit
    hc: HeaderCarrier
  ): Future[UpdateOutcome]

}

object MessageService {
  sealed trait UpdateOutcome

  object UpdateOutcome {
    case object Updated extends UpdateOutcome
    case object Locked extends UpdateOutcome
    case object NotUpdatedThrottled extends UpdateOutcome
  }

  final case class EnrichedError(message: String, cause: Throwable) extends Throwable {
    override def getStackTrace: Array[StackTraceElement] = cause.getStackTrace
    override def getMessage: String                      = message
  }
}
