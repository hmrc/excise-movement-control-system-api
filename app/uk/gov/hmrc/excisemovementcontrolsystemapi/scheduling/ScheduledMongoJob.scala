package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import play.api.Logging
import uk.gov.hmrc.mongo.lock.LockService

import scala.concurrent.{ExecutionContext, Future}

trait ScheduledMongoJob extends ExclusiveScheduledJob with ScheduledJobState with Logging {

  val lockKeeper: LockService

  def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful]

  override def executeInMutex(implicit ec: ExecutionContext): Future[Result] = {
    lockKeeper withLock {
      runJob
    } map {
      case Some(_) => Result(s"$name Job ran successfully.")
      case _       => Result(s"$name did not run because repository was locked by another instance of the scheduler.")
    } recover {
      case failure: RunningOfJobFailed =>
        logger.error("The execution of the job failed.", failure.wrappedCause)
        failure.asResult
    }
  }

}

