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
import org.apache.pekko.Done
import play.api.Configuration

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

@Singleton
class PollingNewMessagesJob @Inject
(
  configuration: Configuration,
) extends ScheduledJob {

  override def name: String = "polling-new-messages-job"

  override def execute(implicit ec: ExecutionContext): Future[Done] = ???

  override val enabled: Boolean = true

  override def initialDelay: FiniteDuration = configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.initialDelay")

  override def intervalBetweenJobRunning: FiniteDuration = configuration.get[FiniteDuration]("scheduler.pollingNewMessagesJob.interval")
}
