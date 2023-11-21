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
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberQueueWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling.PollingNewMessageWithWorkItemJob.{MessageError, MessageReceived, NewMessageResult, NoMessageFound}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementService, NewMessageParserService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PollingNewMessageWithWorkItemJob @Inject()
(
  mongoLockRepository: MongoLockRepository,
  newMessageService: GetNewMessageService,
  workItemRepository: ExciseNumberQueueWorkItemRepository,
  movementService: MovementService,
  messageParser: NewMessageParserService,
  appConfig: AppConfig,
  dateTimeService: TimestampSupport
)(implicit ec: ExecutionContext) extends ScheduledMongoJob
  with Logging {

  override val enabled: Boolean = true
  override def name: String = "polling-new-messages"
  override def interval: FiniteDuration = appConfig.interval
  override def initialDelay: FiniteDuration = appConfig.initialDelay
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy override val lockKeeper: LockService = LockService(mongoLockRepository, lockId = "PollingNewMessageWithWorkItem", ttl = 1.hour)

  override def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful] = {
    val now = dateTimeService.timestamp
    process(now.minus(1, ChronoUnit.DAYS), now, appConfig.retryAttempts)
  }

  private def process(failedBefore: Instant, availableBefore: Instant, retryAttempts: Int): Future[RunningOfJobSuccessful] = {
    workItemRepository.pullOutstanding(failedBefore, availableBefore)
      .flatMap {
        case None => Future.successful(RunningOfJobSuccessful)
        case Some(wi) => getNewMessages(wi.item.exciseNumber).flatMap { success => // call your function to process a WorkItem
          success match {
            case (true, NoMessageFound) => workItemRepository.complete(wi.id, ProcessingStatus.Succeeded)
            case (true, MessageReceived) => workItemRepository.markAs(wi.id, ProcessingStatus.ToDo)
            case (false, _) if wi.failureCount < retryAttempts => workItemRepository.markAs(wi.id, ProcessingStatus.Failed)
            case _ => workItemRepository.markAs(wi.id, ProcessingStatus.PermanentlyFailed)
          }
        }.flatMap(_ => process(failedBefore, availableBefore, retryAttempts))
      }
      .recoverWith {
        case NonFatal(e) =>
          logger.error("[PollingNewMessageWithWorkItemJob] - Failed to collect and process ann the movement", e)
          Future.failed(RunningOfJobFailed(name, e))
      }
  }


  private def getNewMessages(exciseNumber: String): Future[(Boolean, NewMessageResult)] = {
    newMessageService.getNewMessagesAndAcknowledge(exciseNumber)
      .flatMap(message =>
        message match {
          case Some(response) if response.message.nonEmpty =>
            saveToDB(exciseNumber, response).map(result => (result, MessageReceived))
          case _ => Future.successful((true, NoMessageFound))
        })
      .recover {
        case NonFatal(e) =>
          logger.error(s"[PollingNewMessageWithWorkItemJob] - Could not get messages for ern: ${exciseNumber} with message: ${e.getMessage}. Will retry later", e)
          (false, MessageError)
      }
  }

  private def saveToDB(
    exciseNumber: String,
    newMessageResponse: EISConsumptionResponse
  ): Future[Boolean] = {

    messageParser.extractMessages(newMessageResponse.message)
      .foldLeft(successful(true)) { case (acc, x) =>
        acc.flatMap {
          _ => save(x, exciseNumber)
        }
      }
  }

  private def save(message: IEMessage, exciseNumber: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    movementService.updateMovement( message, exciseNumber)
      .flatMap {
        case true =>
          successful(true)
        case _ =>
          logger.warn(s"[PollingNewMessageWithWorkItemJob] - Could not update movement for ern: $exciseNumber")
          successful(false)
      }
  }
}

object PollingNewMessageWithWorkItemJob {

  sealed trait NewMessageResult
  case object MessageReceived extends NewMessageResult
  case object NoMessageFound extends NewMessageResult
  case object MessageError extends NewMessageResult
}