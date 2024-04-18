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

package uk.gov.hmrc.excisemovementcontrolsystemapi.config

import com.codahale.metrics.Timer
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling.{PollingNewMessagesWithWorkItemJob, ScheduledJob}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.{Clock, Duration}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class JobScheduler @Inject() (
                               pollingNewMessageWorkItemJob: PollingNewMessagesWithWorkItemJob,
                               applicationLifecycle: ApplicationLifecycle,
                               actorSystem: ActorSystem,
                               metrics: Metrics,
                               clock: Clock
                             )(implicit val ec: ExecutionContext) extends Logging {

  private val scheduledJobs: Seq[ScheduledJob] = Seq(
    pollingNewMessageWorkItemJob
  ).filter(_.enabled)

  private val timers: Map[ScheduledJob, Timer] = scheduledJobs.map { job =>
    job -> metrics.defaultRegistry.timer(s"${job.name}.timer")
  }.toMap

  private val cancellables = scheduledJobs.map { job =>
    actorSystem.scheduler.scheduleWithFixedDelay(job.initialDelay, job.intervalBetweenJobRunning) { () =>
      val startTime = clock.instant()
      logger.info(s"Executing job ${job.name}")
      job.execute.onComplete { result =>
        val duration = Duration.between(startTime, clock.instant())
        timers(job).update(duration)
        result match {
          case Success(job.Result(message)) =>
            logger.info(s"Completed job ${job.name} in ${duration.toSeconds}s: $message")
          case Failure(throwable) =>
            logger.error(s"Exception running job ${job.name} after ${duration.toSeconds}s", throwable)
        }
      }
    }
  }

  applicationLifecycle.addStopHook(() => {
    logger.info(s"Cancelling all scheduled jobs.")
    cancellables.foreach(_.cancel())
    Future.unit
  })
}
