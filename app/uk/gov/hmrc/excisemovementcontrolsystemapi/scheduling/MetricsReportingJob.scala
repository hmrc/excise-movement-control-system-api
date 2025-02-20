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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import play.api.Configuration
import uk.gov.hmrc.mongo.metrix.MetricOrchestrator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

@Singleton
class MetricsReportingJob @Inject() (
  configuration: Configuration,
  metricOrchestrator: MetricOrchestrator
) extends ScheduledJob {

  override def name: String = "metrics-reporting-job"

  override def execute(implicit ec: ExecutionContext): Future[ScheduledJob.Result] =
    metricOrchestrator.attemptMetricRefresh().map { result =>
      result.log()
      ScheduledJob.Result.Completed
    }

  override val initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.metricsJob.initialDelay")
  override val interval: FiniteDuration     = configuration.get[FiniteDuration]("scheduler.metricsJob.interval")
  override val enabled: Boolean             = configuration.get[Boolean]("metrics.enabled")
}
