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

import com.codahale.metrics.MetricRegistry
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, ErnSubmissionRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.lock.{LockRepository, LockService}
import uk.gov.hmrc.mongo.metrix.{MetricOrchestrator, MetricRepository, MetricSource}

import java.time.{Duration, Instant}
import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

@Singleton
class MetricsProvider @Inject() (
  configuration: Configuration,
  lockRepository: LockRepository,
  metricRepository: MetricRepository,
  metricRegistry: MetricRegistry,
  movementRepository: MovementRepository,
  ernSubmissionRepository: ErnSubmissionRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  dateTimeService: DateTimeService
) extends Provider[MetricOrchestrator] {

  private val lockTtl: FiniteDuration  = configuration.get[FiniteDuration]("scheduler.metricsJob.lock-ttl")
  private val lockService: LockService = LockService(lockRepository, lockId = "metrix-orchestrator", ttl = lockTtl)

  private val fastPollingInterval: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.fastPollingInterval")
  private val fastPollingCutoff: FiniteDuration   =
    configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.fastPollingCutoff")
  private val slowPollingInterval: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.slowPollingInterval")

  private val source = new MetricSource {
    override def metrics(implicit ec: ExecutionContext): Future[Map[String, Int]] = {
      val now = dateTimeService.timestamp()
      for {
        lastActivityMap      <- getLastActivity
        ernsAndLastRetrieved <- ernRetrievalRepository.getErnsAndLastRetrieved
      } yield {
        val count = lastActivityMap.count { case (ern, lastActivity) =>
          shouldUpdateMessages(now, lastActivity, ernsAndLastRetrieved.get(ern))
        }
        Map("polling-new-messages-job.backlog.count" -> count)
      }
    }
  }

  private def getLastActivity(implicit ec: ExecutionContext): Future[Map[String, Instant]] =
    for {
      ernsAndLastReceived  <- movementRepository.getErnsAndLastReceived
      ernsAndLastSubmitted <- ernSubmissionRepository.getErnsAndLastSubmitted
    } yield ernsAndLastReceived.foldLeft(ernsAndLastSubmitted) { case (mergedMap, (k, v)) =>
      mergedMap.updated(k, Seq(mergedMap.get(k), Some(v)).flatten.max)
    }

  private def shouldUpdateMessages(
    now: Instant,
    lastActivity: Instant,
    lastRetrievedOption: Option[Instant]
  ): Boolean = {
    val pollingThreshold =
      if (lastActivity.isBefore(now.minus(fastPollingCutoff.length, fastPollingCutoff.unit.toChronoUnit))) {
        slowPollingInterval
      } else {
        fastPollingInterval
      }

    lastRetrievedOption.forall { lastRetrieved =>
      val timeSinceLastPoll = Duration.between(lastRetrieved, now)
      timeSinceLastPoll.toMillis > pollingThreshold.toMillis
    }
  }

  override def get(): MetricOrchestrator = new MetricOrchestrator(
    metricSources = List(source),
    lockService = lockService,
    metricRepository = metricRepository,
    metricRegistry = metricRegistry
  )
}
