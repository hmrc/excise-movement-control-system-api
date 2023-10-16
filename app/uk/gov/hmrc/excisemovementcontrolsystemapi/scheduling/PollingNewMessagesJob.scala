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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementMessageService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PollingNewMessagesJob @Inject()(
  newMessageService: GetNewMessageService,
  movementService: MovementMessageService,
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
    movementService
      .getUniqueConsignorId.flatMap(
      stream =>
        stream.runWith(Sink.foreachAsync[Movement](appConfig.parallelism)(getNewMessages(_)))
          .map(_ => RunningOfJobSuccessful)
    )
      .recoverWith {
        case NonFatal(e) =>
          logger.error("Failed to get all new messages", e)
          Future.failed(RunningOfJobFailed(name, e))
      }
  }

  private def getNewMessages(movement: Movement)(implicit ec: ExecutionContext): Future[Unit] = {

    newMessageService.getNewMessagesAndAcknowledge(movement.consignorId)
      .flatMap(message => message.fold[Future[Unit]](successful(()))(m => saveToDB(movement, m)))
      .recover {
        case NonFatal(e) =>
          logger.warn(s"Could not get messages for ern: ${movement.consignorId} with message: ${e.getMessage}. Will retry later", e)
          successful(())
    }
  }

  private def saveToDB(
    movement: Movement,
    message: ShowNewMessageResponse
  )(implicit ec: ExecutionContext): Future[Unit] = {

    /*
    todo
    1. extract messages
    2. get LRN, ARC for that message that mach consignorId
    3. store the message into mongo according LRN/ARC anc consignorID
     */
    movementService.updateMovement(movement.localReferenceNumber, movement.consignorId, message.message)
      .flatMap {
      case true => successful(())
      case _ =>
          logger.warn("Could not came new message to cache")
          successful(())
    }
  }
}