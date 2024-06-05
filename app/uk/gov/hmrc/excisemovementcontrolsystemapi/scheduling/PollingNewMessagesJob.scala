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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import cats.syntax.all._
import org.apache.pekko.Done
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnSubmissionRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PollingNewMessagesJob @Inject() (
  configuration: Configuration,
  movementRepository: MovementRepository,
  ernSubmissionRepository: ErnSubmissionRepository,
  messageService: MessageService,
  dateTimeService: DateTimeService
) extends ScheduledJob {

  override def name: String = "polling-new-messages-job"

  override def execute(implicit ec: ExecutionContext): Future[Done] = {
    val now              = dateTimeService.timestamp()
    val fastIntervalTime = timestampBeforeNow(now, fastPollingInterval)
    val fastCutoffTime   = timestampBeforeNow(now, fastPollingCutoff)
    val slowIntervalTime = timestampBeforeNow(now, slowPollingInterval)
    getLastActivity
      .flatMap { lastActivityMap =>
        lastActivityMap.toSeq.traverse { case (ern, lastActivity) =>
          if (shouldUpdateMessages(lastActivity, fastIntervalTime, fastCutoffTime, slowIntervalTime)) {
            messageService.updateMessages(ern)
          } else {
            Future.successful(Done)
          }
        }
      }
      .as(Done)
  }

  private def shouldUpdateMessages(
    lastActivity: Instant,
    fastIntervalTime: Instant,
    fastCutoffTime: Instant,
    slowIntervalTime: Instant
  ): Boolean =
    (lastActivity.isBefore(fastIntervalTime) && lastActivity.isAfter(fastCutoffTime)) ||
      lastActivity.isBefore(slowIntervalTime)

  private def timestampBeforeNow(now: Instant, duration: Duration): Instant =
    now.minus(duration.length, duration.unit.toChronoUnit)

  private def getLastActivity(implicit ec: ExecutionContext): Future[Map[String, Instant]] =
    for {
      ernsAndLastReceived  <- movementRepository.getErnsAndLastReceived
      ernsAndLastSubmitted <- ernSubmissionRepository.getErnsAndLastSubmitted
    } yield ernsAndLastReceived.foldLeft(ernsAndLastSubmitted) { case (mergedMap, (k, v)) =>
      mergedMap.updated(k, Seq(mergedMap.get(k), Some(v)).flatten.max)
    }

  override val enabled: Boolean = true

  override def initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.initialDelay")

  override def interval: FiniteDuration = configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.interval")

  private val fastPollingInterval: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.fastPollingInterval")
  private val fastPollingCutoff: FiniteDuration   =
    configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.fastPollingCutoff")
  private val slowPollingInterval: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.slowPollingInterval")
  implicit val hc: HeaderCarrier                  = HeaderCarrier()
}
