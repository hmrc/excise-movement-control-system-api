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

import akka.http.scaladsl.util.FastFuture.successful
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ShowNewMessageResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumber
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.GetNewMessageService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PollingNewMessagesJob @Inject()(
  newMessageService: GetNewMessageService,
  exciseNumberRepository: ExciseNumberRepository,
  appConfig: AppConfig
) (implicit mat: Materializer)
  extends ExclusiveScheduledJob
    with ScheduledJobState
    with Logging {

  override def name: String = "polling-new-message"
  override def interval: FiniteDuration = appConfig.interval
  override def initialDelay: FiniteDuration = appConfig.initialDelay
  implicit val hc: HeaderCarrier = HeaderCarrier()

  //todo: do we may need to lock mongo db until we have finished the task?
  //  lazy override val lockKeeper: LockService = LockService(mongoLockRepository, lockId = "RetryPushNotificationsJob", ttl = 1.hour)


  override def executeInMutex(implicit ec: ExecutionContext): Future[Result] = {
      runJob.map {_ =>  Result(s"$name Job ran successfully.")
    } recover {
      case failure: RunningOfJobFailed =>
        logger.error("The execution of the job failed.", failure.wrappedCause)
        failure.asResult
    }
  }

   def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful] = {
     exciseNumberRepository
       .getAll
       .runWith(Sink.foreachAsync[ExciseNumber](appConfig.parallelism)(getNewMessages(_)))
       .map(_ => RunningOfJobSuccessful)
       .recoverWith {
         case NonFatal(e) =>
           logger.error("Failed to get all new messages", e)
           Future.failed(RunningOfJobFailed(name, e))
       }
   }

  private def getNewMessages(exciseNumber: ExciseNumber)(implicit ec: ExecutionContext): Future[Unit] = {

    newMessageService.getNewMessagesAndAcknowledge(exciseNumber.exciseNumber)
      .flatMap(message => message.fold[Future[Unit]](successful(()))(m => saveToDB(m)))
      .recover {
        case NonFatal(e) =>
          logger.warn(s"Could not get messages for ern: ${exciseNumber.exciseNumber} with message: ${e.getMessage}. Will retry later", e)
          successful(())
    }
  }

  private def saveToDB(message: ShowNewMessageResponse): Future[Unit] = {


    logger.warn("test")
    successful(())
  }
}