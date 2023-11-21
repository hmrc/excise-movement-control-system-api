/*
 * Copyright 2023 HM Revenue & Customs
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
