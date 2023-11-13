package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import java.util.concurrent.Semaphore
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait ExclusiveScheduledJob extends ScheduledJob {
  def executeInMutex(implicit ec: ExecutionContext): Future[this.Result]

  final def execute(implicit ec: ExecutionContext): Future[Result] =
    if (mutex.tryAcquire()) {
      Try(executeInMutex) match {
        case Success(f) => f andThen { case _ => mutex.release() }
        case Failure(e) => Future.successful(mutex.release()).flatMap(_ => Future.failed(e))
      }
    } else Future.successful(Result("Skipping execution: job running"))

  def isRunning: Future[Boolean] = Future.successful(mutex.availablePermits() == 0)

  final private val mutex = new Semaphore(1)
}
