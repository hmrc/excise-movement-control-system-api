package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import akka.actor.{Cancellable, Scheduler}
import dispatch.Future
import org.apache.commons.lang3.time.StopWatch
import play.api.inject.ApplicationLifecycle
import play.api.{Application, Logging}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

trait RunningOfScheduledJobs extends Logging {

  implicit val ec: ExecutionContext

  val application: Application

  lazy val scheduler: Scheduler = application.actorSystem.scheduler

  val scheduledJobs: Seq[ScheduledJob]

  val applicationLifecycle: ApplicationLifecycle

  private[scheduling] var cancellables: Seq[Cancellable] = Seq.empty

  cancellables = scheduledJobs.map { job =>
    scheduler.scheduleWithFixedDelay(job.initialDelay, job.interval)(new Runnable() {
      def run(): Unit = {
        val stopWatch = new StopWatch
        stopWatch.start()
        logger.info(s"Executing job ${job.name}")

        job.execute.onComplete {
          case Success(job.Result(message)) =>
            stopWatch.stop()
            logger.info(s"Completed job ${job.name} in $stopWatch: $message")
          case Failure(throwable)           =>
            stopWatch.stop()
            logger.error(s"Exception running job ${job.name} after $stopWatch", throwable)
        }
      }
    })
  }

  applicationLifecycle.addStopHook(() => {
    logger.info(s"Cancelling all scheduled jobs.")
    cancellables.foreach(_.cancel())
    scheduledJobs.foreach { job =>
      logger.info(s"Checking if job ${job.configKey} is running")
      while (Await.result(job.isRunning, 5.seconds)) {
        logger.warn(s"Waiting for job ${job.configKey} to finish")
        Thread.sleep(1000)
      }
      logger.warn(s"Job ${job.configKey} is finished")
    }

    Future.successful(())
  })
}

