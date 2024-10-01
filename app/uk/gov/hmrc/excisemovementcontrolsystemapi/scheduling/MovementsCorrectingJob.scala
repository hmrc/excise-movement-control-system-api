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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ProblemMovementsWorkItemRepo
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

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

  lazy val maxRetries: Int = configuration.get[Int]("scheduler.movementsCorrectingJob.maxRetries")

  override def execute(implicit ec: ExecutionContext): Future[ScheduledJob.Result] = {
    val now = dateTimeService.timestamp()

    val result: Future[Boolean] = problemMovementsWorkItemRepo
      .pullOutstanding(failedBefore = now.minus(1, ChronoUnit.DAYS), now)
      .flatMap {
        case None     => Future.successful(true)
        case Some(wi) =>
          messageService
            .archiveAndFixProblemMovement(wi.item.movementId)
            .flatMap { _ =>
              problemMovementsWorkItemRepo.complete(wi.id, ProcessingStatus.Succeeded)
            }
            .recoverWith {
              case e if wi.failureCount < maxRetries =>
                logger.warn(s"movements correcting job item ${wi.id} failed - ${e.getMessage}")
                problemMovementsWorkItemRepo.markAs(wi.id, ProcessingStatus.Failed)
              case e                                 =>
                logger.warn(s"movements correcting job item ${wi.id} failed (marking permanently) - ${e.getMessage}")
                problemMovementsWorkItemRepo.markAs(wi.id, ProcessingStatus.PermanentlyFailed)
            }
      }
    result.as(ScheduledJob.Result.Completed)
  }

  override val enabled: Boolean = configuration.get[Boolean]("featureFlags.movementsCorrectingEnabled")

  override val numberOfInstances: Int =
    configuration.get[Int]("scheduler.movementsCorrectingJob.numberOfInstances")

  override def initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.movementsCorrectingJob.initialDelay")

  override def interval: FiniteDuration = configuration.get[FiniteDuration]("scheduler.movementsCorrectingJob.interval")

  implicit val hc: HeaderCarrier = HeaderCarrier()

}
