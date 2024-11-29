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

import cats.implicits.toFunctorOps
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsServiceNew
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class NrsSubmissionJob @Inject() (
  nrsWorkItemRepository: NRSWorkItemRepository,
  nrsService: NrsServiceNew,
  configuration: Configuration,
  dateTimeService: DateTimeService
) extends ScheduledJob
    with Logging {

  override def execute(implicit ec: ExecutionContext): Future[ScheduledJob.Result] = {
    // locking here. uses mongo to do it. because this then shares between instances.
    val now                     = dateTimeService.timestamp()
    val result: Future[Boolean] = nrsWorkItemRepository
      .pullOutstanding(
        now,
        now
      ) // currently one at a time. need to process everything it can. Probably move into service.
      .flatMap {
        case None           => Future.successful(true)
        case Some(workItem) =>
          nrsService.submitNrs(workItem).map(_ => true)
      }
    result.as(ScheduledJob.Result.Completed)
  }

  override def name: String = "nrs-submission-scheduler"

  override val enabled: Boolean = configuration.get[Boolean]("featureFlags.nrsSubmissionEnabled")

  override def initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.nrsSubmissionJob.initialDelay")

  override def interval: FiniteDuration = configuration.get[FiniteDuration]("scheduler.nrsSubmissionJob.interval")

  override val numberOfInstances: Int =
    configuration.get[Int]("scheduler.nrsSubmissionJob.numberOfInstances")

  implicit val hc: HeaderCarrier = HeaderCarrier()

}
