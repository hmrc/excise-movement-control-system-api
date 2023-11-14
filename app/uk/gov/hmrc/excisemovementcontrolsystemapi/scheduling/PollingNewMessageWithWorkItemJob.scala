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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.ExciseNumberQueueWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementService, NewMessageParserService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, Succeeded, ToDo}

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
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) extends ScheduledMongoJob
  with Logging {

  override val enabled: Boolean = true
  override def name: String = "polling-new-message-wi"
  override def interval: FiniteDuration = appConfig.interval
  override def initialDelay: FiniteDuration = appConfig.initialDelay
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy override val lockKeeper: LockService = LockService(mongoLockRepository, lockId = "PollingNewMessageWithWorkItem", ttl = 1.hour)

  override def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful] = {
    //todo: we may want to get the consigneeId too here
    process
  }

  private def process: Future[RunningOfJobSuccessful] = {
    val availableBefore = dateTimeService.instant
    workItemRepository.pullOutstanding(failedBefore = dateTimeService.instant.minus(1, ChronoUnit.DAYS), availableBefore = availableBefore) // grab the next WorkItem
      .flatMap {
        case None => Future.successful(RunningOfJobSuccessful) // there is no more - we've finished
        case Some(wi) => getNewMessages(wi.item.exciseNumber).flatMap { success => // call your function to process a WorkItem
          success match {
            case (true, Succeeded) =>
              workItemRepository.complete(wi.id, ProcessingStatus.Succeeded)
            case (true, ToDo) =>
              workItemRepository.markAs(wi.id, ProcessingStatus.ToDo)
            case (false, _ )

              if wi.failureCount < 3 => workItemRepository.markAs(wi.id, ProcessingStatus.Failed)
            case _ => workItemRepository.markAs(wi.id, ProcessingStatus.PermanentlyFailed)


            //              if(success._1) {
            //                workItemRepository.complete(wi.id, ProcessingStatus.Succeeded)
            //                // mark as completed
            //                //workItemRepository.completeAndDelete(wi.id) // alternatively, remove from mongo
            //              } else if(!success && wi.failureCount < 3) {
            //                workItemRepository.markAs(wi.id, ProcessingStatus.Failed)
            //              } // mark as failed - it will be reprocessed after a duration specified by `inProgressRetryAfterProperty`
            //              else workItemRepository.markAs(wi.id, ProcessingStatus.PermanentlyFailed)
          } // you can also mark as any other status defined by `ProcessingStatus`
        }.flatMap(_ => process) // and repeat
      }
  }


  private def getNewMessages(consignorId: String): Future[(Boolean, ProcessingStatus)] = {
    newMessageService.getNewMessagesAndAcknowledge(consignorId)
      .flatMap(message =>
        message match {
          case Some(response) if response.message.nonEmpty =>
            saveToDB(consignorId, response).map(result => (result, ToDo))
          case _ => Future.successful((true, Succeeded))
        })
      .recover {
        case NonFatal(e) =>
          logger.error(s"Could not get messages for ern: ${consignorId} with message: ${e.getMessage}. Will retry later", e)
          (false, Failed)
      }
  }

  private def saveToDB
  (
    consignorId: String,
    newMessageResponse: EISConsumptionResponse
  )(implicit ec: ExecutionContext): Future[Boolean] = {

    val messages = messageParser.extractMessages(newMessageResponse.message)

    //! process IE801 or IE704 first if any.We are Processing message in sequence (not in parallel)
    // If we want to process them in parallel we need to put a lock on mongo when reading
    // and writing. As we could read just before  or while writing and the next time we write we
    // may do not write the up to date info. At the moment we have a mongo lock on the Job.
    messages.filter(isAcceptedOrRefusalMessage(_))
      .foldLeft(successful(true)) { case (acc, x) =>
        acc.flatMap {
          _ => save(x, consignorId)
        }
      }

    messages
      .filterNot(isAcceptedOrRefusalMessage(_))
      .foldLeft(successful(true)) { case (acc, x) =>
        acc.flatMap {
          _ => save(x, consignorId)
        }
      }
  }

  private def save(message: IEMessage, consignorId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    movementService.updateMovement( message, consignorId)
      .flatMap {
        case true => successful(true)
        case _ =>
          logger.warn(s"Could not update movement for ern: $consignorId")
          successful(false)
      }
  }

  private def isAcceptedOrRefusalMessage(message: IEMessage): Boolean =
    message.messageType.equals(MessageTypes.IE801.value) ||
      message.messageType.equals(MessageTypes.IE704.value)

}
