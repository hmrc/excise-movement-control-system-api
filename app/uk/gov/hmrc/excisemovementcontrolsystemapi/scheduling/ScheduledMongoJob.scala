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
    }
  }

}

