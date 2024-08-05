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
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, ErnSubmissionRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.{Duration, Instant}
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PollingNewMessagesJob @Inject() (
  configuration: Configuration,
  movementRepository: MovementRepository,
  ernSubmissionRepository: ErnSubmissionRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  messageService: MessageService,
  dateTimeService: DateTimeService,
  metrics: Metrics
) extends ScheduledJob {

  override def name: String = "polling-new-messages-job"

  private val lockedMeter    = metrics.defaultRegistry.meter("polling-new-messages-job.locked-meter")
  private val updatedMeter   = metrics.defaultRegistry.meter("polling-new-messages-job.updated-meter")
  private val throttledMeter = metrics.defaultRegistry.meter("polling-new-messages-job.throttled-meter")

  override def execute(implicit ec: ExecutionContext): Future[ScheduledJob.Result] = {
    val deadline = dateTimeService.timestamp().plus(interval.toMillis, ChronoUnit.MILLIS)
    getLastActivity
      .flatMap { lastActivityMap =>
        lastActivityMap.toSeq.traverse { case (ern, lastActivity) =>
          val now = dateTimeService.timestamp()
          if (now.isBefore(deadline)) {
            ernRetrievalRepository.getLastRetrieved(ern).flatMap { lastRetrieved =>
              if (shouldUpdateMessages(now, lastActivity, lastRetrieved)) {
                messageService.updateMessages(ern).map { r =>
                  r match {
                    case MessageService.UpdateOutcome.Updated             => updatedMeter.mark()
                    case MessageService.UpdateOutcome.NotUpdatedThrottled => throttledMeter.mark()
                    case MessageService.UpdateOutcome.Locked              => lockedMeter.mark()
                  }
                  ScheduledJob.Result.Completed
                }
              } else {
                Future.successful(ScheduledJob.Result.Completed)
              }
            }
          } else {
            Future.successful(ScheduledJob.Result.Cancelled)
          }
        }
      }
      .map { results =>
        val numberOfUnprocessedJobs = results.count(_ == ScheduledJob.Result.Cancelled)
        if (numberOfUnprocessedJobs > 0) {
          ScheduledJob.Result.Cancelled
        } else {
          ScheduledJob.Result.Completed
        }
      }
  }

  private def shouldUpdateMessages(
    now: Instant,
    lastActivity: Instant,
    lastRetrievedOption: Option[Instant]
  ): Boolean = {
    val pollingThreshold = if (lastActivity.isBefore(timestampBeforeNow(now, fastPollingCutoff))) {
      slowPollingInterval
    } else {
      fastPollingInterval
    }

    lastRetrievedOption.forall { lastRetrieved =>
      val timeSinceLastPoll = Duration.between(lastRetrieved, now)
      timeSinceLastPoll.toMillis > pollingThreshold.toMillis
    }
  }

  private def timestampBeforeNow(now: Instant, duration: FiniteDuration): Instant =
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
