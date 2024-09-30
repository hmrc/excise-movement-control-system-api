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
import org.apache.pekko.Done
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ProblemMovementsWorkItemRepo
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementsCorrectingJob @Inject() (
  configuration: Configuration,
  messageService: MessageService,
  problemMovementsWorkItemRepo: ProblemMovementsWorkItemRepo,
  dateTimeService: DateTimeService
) extends ScheduledJob
    with Logging {
  override def name: String = "movements-correcting-job"

  override def execute(implicit ec: ExecutionContext): Future[ScheduledJob.Result] = {
    val now = dateTimeService.timestamp()

    problemMovementsWorkItemRepo
      .pullOutstanding(failedBefore = now.minus(1, ChronoUnit.DAYS), now)
      .map {
        case Some(value) =>
          messageService.archiveAndFixProblemMovement(value.item.movementId)
        case None        => Done

      }
      .as(ScheduledJob.Result.Completed)
  }

  override val enabled: Boolean = configuration.get[Boolean]("featureFlags.movementsCorrectingEnabled")

  override val numberOfInstances: Int =
    configuration.get[Int]("scheduler.movementsCorrectingJob.numberOfInstances")

  override def initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.movementsCorrectingJob.initialDelay")

  override def interval: FiniteDuration = configuration.get[FiniteDuration]("scheduler.movementsCorrectingJob.interval")
  implicit val hc: HeaderCarrier        = HeaderCarrier()

}
