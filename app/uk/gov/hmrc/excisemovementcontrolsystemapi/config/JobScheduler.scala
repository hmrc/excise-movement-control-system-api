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

import com.codahale.metrics.{Counter, Timer}
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling.{MetricsReportingJob, MiscodedMovementsCorrectingJob, MovementsCorrectingJob, NrsSubmissionJob, PollingNewMessagesJob, PushNotificationJob, ScheduledJob}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.{Clock, Duration}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class JobScheduler @Inject() (
  pollingNewMessagesJob: PollingNewMessagesJob,
  pushNotificationJob: PushNotificationJob,
  applicationLifecycle: ApplicationLifecycle,
  metricsJob: MetricsReportingJob,
  movementsCorrectingJob: MovementsCorrectingJob,
  miscodedMovementsCorrectingJob: MiscodedMovementsCorrectingJob,
  nrsSubmissionJob: NrsSubmissionJob,
  actorSystem: ActorSystem,
  metrics: Metrics,
  clock: Clock
)(implicit val ec: ExecutionContext)
    extends Logging {

  private val scheduledJobs: Seq[ScheduledJob] = Seq(
    pollingNewMessagesJob,
    pushNotificationJob,
    metricsJob,
    movementsCorrectingJob,
    miscodedMovementsCorrectingJob,
    nrsSubmissionJob
  ).filter(_.enabled)

  private val timers: Map[ScheduledJob, Timer] = scheduledJobs.map { job =>
    job -> metrics.defaultRegistry.timer(s"${job.name}.timer")
  }.toMap

  private val counters: Map[ScheduledJob, Counter] = scheduledJobs.map { job =>
    job -> metrics.defaultRegistry.counter(s"${job.name}.number-running")
  }.toMap

  private val cancellables = scheduledJobs.flatMap { job =>
    logger.info(s"Scheduling $job")

    (0 until job.numberOfInstances).map { _ =>
      actorSystem.scheduler.scheduleWithFixedDelay(job.initialDelay, job.interval) { () =>
        val startTime = clock.instant()
        val runId     = UUID.randomUUID()
        logger.info(s"Executing job ${job.name} with runID $runId")
        counters(job).inc()
        job.execute.onComplete { result =>
          val duration = Duration.between(startTime, clock.instant())
          counters(job).dec()
          timers(job).update(duration)
          result match {
            case Success(ScheduledJob.Result.Completed) =>
              logger.info(s"Completed job ${job.name} with runID $runId in ${duration.toSeconds}s")
            case Success(ScheduledJob.Result.Cancelled) =>
              logger.warn(s"Cancelled job ${job.name} with runID $runId after ${duration.toSeconds}")
            case Failure(throwable)                     =>
              logger.error(
                s"Exception running job ${job.name} with runID $runId after ${duration.toSeconds}s",
                throwable
              )
          }
        }
      }
    }
  }

  applicationLifecycle.addStopHook { () =>
    logger.info(s"Cancelling all scheduled jobs.")
    cancellables.foreach(_.cancel())
    Future.unit
  }
}
