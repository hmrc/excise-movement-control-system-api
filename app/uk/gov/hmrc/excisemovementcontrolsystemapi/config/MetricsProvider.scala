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
import uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling.PollingNewMessagesJob
import uk.gov.hmrc.mongo.lock.{LockRepository, LockService}
import uk.gov.hmrc.mongo.metrix.{MetricOrchestrator, MetricRepository, MetricSource}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetricsProvider @Inject() (
  configuration: Configuration,
  lockRepository: LockRepository,
  metricRepository: MetricRepository,
  metricRegistry: MetricRegistry,
  pollingNewMessagesJob: PollingNewMessagesJob
) extends Provider[MetricOrchestrator] {

  private val lockTtl: FiniteDuration  = configuration.get[FiniteDuration]("scheduler.metricsJob.lock-ttl")
  private val lockService: LockService = LockService(lockRepository, lockId = "metrix-orchestrator", ttl = lockTtl)

  private val source = new MetricSource {
    override def metrics(implicit ec: ExecutionContext): Future[Map[String, Int]] =
      pollingNewMessagesJob.getBacklog().map { count =>
        Map("polling-new-messages-job.backlog.count" -> count)
      }
  }

  override def get(): MetricOrchestrator = new MetricOrchestrator(
    metricSources = List(source),
    lockService = lockService,
    metricRepository = metricRepository,
    metricRegistry = metricRegistry
  )
}
