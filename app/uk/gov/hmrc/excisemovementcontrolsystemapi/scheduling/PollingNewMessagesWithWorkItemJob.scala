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
import cats.syntax.all._
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.NotificationResponse.{NotInUseNotificationResponse, SuccessPushNotificationResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling.PollingNewMessagesWithWorkItemJob._
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.DurationConverters.ScalaDurationOps
import scala.util.control.NonFatal

class PollingNewMessagesWithWorkItemJob @Inject()
(
  mongoLockRepository: MongoLockRepository,
  newMessageService: GetNewMessageService,
  workItemService: WorkItemService,
  movementService: MovementService,
  messageParser: NewMessageParserService,
  notificationService: PushNotificationService,
  auditService: AuditService,
  appConfig: AppConfig,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) extends ScheduledMongoJob
  with Logging {

  override val enabled: Boolean = true

  override def name: String = "polling-new-messages"

  override def intervalBetweenJobRunning: FiniteDuration = appConfig.interval

  override def initialDelay: FiniteDuration = appConfig.initialDelay

  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy override val lockKeeper: LockService = LockService(mongoLockRepository, lockId = "PollingNewMessageWithWorkItem", ttl = 1.hour)

  override def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful] = {
    val now = dateTimeService.timestamp()
    process(now.minus(appConfig.failureRetryAfter.toJava), now, appConfig.maxFailureRetryAttempts)
  }

  private def process(failedBefore: Instant, availableBefore: Instant, maximumRetries: Int): Future[RunningOfJobSuccessful] = {
    workItemService.pullOutstanding(failedBefore, availableBefore)
      .flatMap {
        case None => Future.successful(RunningOfJobSuccessful)
        case Some(wi) =>

          val ern = wi.item.exciseNumber

          getNewMessages(ern).flatMap {

            case MessagesOutstanding =>
              logger.info(s"[PollingNewMessageWithWorkItemJob] - Work item for ERN $ern has further outstanding messages and has been added back to the queue")
              workItemService.markAs(wi.id, ProcessingStatus.ToDo, Some(wi.availableAt))

            case Processed =>
              logger.info(s"[PollingNewMessageWithWorkItemJob] - Work item for ERN $ern processed and rescheduled")
              workItemService.rescheduleWorkItem(wi)

            case PollingFailed if wi.failureCount < maximumRetries =>
              logger.error(s"[PollingNewMessageWithWorkItemJob] - Work item for ERN $ern has failed")
              workItemService.markAs(wi.id, ProcessingStatus.Failed)

            case _ =>
              logger.error(s"[PollingNewMessageWithWorkItemJob] - Work item for ERN $ern has" +
                s" failed $maximumRetries times and has been moved to the slower polling interval")
              workItemService.rescheduleWorkItemForceSlow(wi)

          }
            .flatMap(_ => process(failedBefore, availableBefore, maximumRetries))
      }
      .recoverWith {
        case NonFatal(e) =>
          logger.error("[PollingNewMessageWithWorkItemJob] - Failed to collect and process the movement", e)
          Future.failed(RunningOfJobFailed(name, e))
      }
  }

  private def getNewMessages(exciseNumber: String): Future[NewMessageResult] = {
    newMessageService.getNewMessagesAndAcknowledge(exciseNumber)
      .flatMap {
        case Some((consumptionResponse, messageCount)) if messageCount > 10 =>
          processMessages(exciseNumber, consumptionResponse).map(_ => MessagesOutstanding)

        case Some((consumptionResponse, _)) =>
          processMessages(exciseNumber, consumptionResponse).map(_ => Processed)

        case _ =>
          logger.error(s"[PollingNewMessageWithWorkItemJob] - Could not get messages for ern: $exciseNumber. Will retry later")
          Future.successful(PollingFailed)
      }
      .recover {
        case NonFatal(e) =>
          logger.error(s"[PollingNewMessageWithWorkItemJob] - Could not get messages for ern: $exciseNumber with message: ${e.getMessage}. Will retry later", e)
          PollingFailed
      }
  }

  private def processMessages(
    exciseNumber: String,
    consumptionResponse: EISConsumptionResponse
  ): Future[Boolean] = {

    messageParser.extractMessages(consumptionResponse.message)
      .foldLeft(successful(true)) { case (acc, x) =>
        acc.flatMap {
          _ => {
            for {
              saveResult <- saveToDbAndSendNotification(x, exciseNumber)
              _ <- auditService.auditMessage(x).value
            } yield saveResult

          }
        }
      }
  }

  private def saveToDbAndSendNotification(
    message: IEMessage,
    exciseNumber: String
  )(implicit ec: ExecutionContext): Future[Boolean] = {

    movementService.updateMovement(message, exciseNumber).map {
      movements =>

        movements.map(m =>
          if (appConfig.featureFlagPPN) {
            notificationService.sendNotification(exciseNumber, m, message.messageIdentifier)
          } else {
            Future.successful(NotInUseNotificationResponse())
          }
        )
          .sequence
          .map { o =>
            o.forall {
              case _: SuccessPushNotificationResponse => true
              case _: NotInUseNotificationResponse => true
              case _ => false
            }
          }
    }.flatten
  }
}

object PollingNewMessagesWithWorkItemJob {

  private sealed trait NewMessageResult

  private case object Processed extends NewMessageResult

  private case object MessagesOutstanding extends NewMessageResult

  private case object PollingFailed extends NewMessageResult
}