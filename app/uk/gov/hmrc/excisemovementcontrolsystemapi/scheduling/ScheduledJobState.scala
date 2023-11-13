package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

trait ScheduledJobState { e: ScheduledJob =>
  sealed trait RunningOfJobSuccessful

  case object RunningOfJobSuccessful extends RunningOfJobSuccessful

  case class RunningOfJobFailed(jobName: String, wrappedCause: Throwable) extends RuntimeException {

    def asResult: Result = {
      Result(
        s"""The execution of scheduled job $jobName failed with error '${wrappedCause.getMessage}'.
           |The next execution of the job will do retry."""
          .stripMargin
          .replace('\n', ' ')
      )
    }
  }
}
