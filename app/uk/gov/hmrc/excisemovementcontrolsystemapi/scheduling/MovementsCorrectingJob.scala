package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import cats.implicits.toFunctorOps
import org.apache.pekko.Done
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ProblemMovementsWorkItemRepo
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DAYS, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MovementsCorrectingJob @Inject() (
  configuration: Configuration,
  messageService: MessageService,
  problemMovementsWorkItemRepo: ProblemMovementsWorkItemRepo
)(implicit hc: HeaderCarrier)
    extends ScheduledJob
    with Logging {
  override def name: String = "movements-correcting-job"

  override def execute(implicit ec: ExecutionContext): Future[ScheduledJob.Result] = {
    val now = Instant.now()

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

  override def initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("scheduler.movementsCorrectingJob.initialDelay")

  override def interval: FiniteDuration = configuration.get[FiniteDuration]("scheduler.movementsCorrectingJob.interval")
}
