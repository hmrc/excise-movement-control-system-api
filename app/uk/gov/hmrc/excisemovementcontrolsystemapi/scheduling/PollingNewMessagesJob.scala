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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageTypes, ShowNewMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementMessageService, ShowNewMessageParser}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import javax.inject.Inject
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PollingNewMessagesJob @Inject()(
  mongoLockRepository: MongoLockRepository,
  newMessageService: GetNewMessageService,
  movementService: MovementMessageService,
  messageParser: ShowNewMessageParser,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) extends ScheduledMongoJob
    with Logging {

  override def name: String = "polling-new-message"
  override def interval: FiniteDuration = appConfig.interval
  override def initialDelay: FiniteDuration = appConfig.initialDelay
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy override val lockKeeper: LockService = LockService(mongoLockRepository, lockId = "PollingNewMessagesJob", ttl = 1.hour)

  override def runJob(implicit ec: ExecutionContext): Future[RunningOfJobSuccessful] = {
    //todo: we may want to get the consigneeId too here
    movementService
      .getUniqueConsignorId
      .flatMap {(movement: Seq[Movement]) => Future.sequence(movement.map(m => getNewMessages(m.consignorId))) }
      .map(_ => RunningOfJobSuccessful)
      .recoverWith {
        case NonFatal(e) =>
          logger.error("Failed to collect and process ann the movement", e)
          Future.failed(RunningOfJobFailed(name, e))
      }
  }

  private def getNewMessages(consignorId: String): Future[Unit] = {

    newMessageService.getNewMessagesAndAcknowledge(consignorId)
      .flatMap(message => message.fold[Future[Unit]](successful(()))(m => saveToDB(consignorId, m)))
      .recover {
        case NonFatal(e) =>
          logger.warn(s"Could not get messages for ern: ${consignorId} with message: ${e.getMessage}. Will retry later", e)
          successful(())
    }
  }

  private def saveToDB
  (
    consignorId: String,
    newMessageResponse: ShowNewMessageResponse
  )(implicit ec: ExecutionContext): Future[Unit] = {

    println(s"XXX=> ERN: $consignorId")
    val messages = messageParser.extractMessages(newMessageResponse.message)

    //! process IE818 or IE704 first if any.We are Processing message in sequence (not in parallel)
    // If we want to process them in parallel we need to put a lock on mongo when reading
    // and writing. As we could read just before  or while writing and the next time we write we
    // may do not write the up to date info. At the moment we have a mongo lovk on the Job.
    messages.filter(isAcceptedOrRefusalMessage(_))
      .foldLeft(successful(())) { case (acc, x) =>
        acc.flatMap {
          _ => save(x, consignorId).map { (x: Unit) => successful(x) }
        }
      }

//    Future.sequence(processFirst
//      .map(o => save(o, consignorId)))
//      .map(_ => successful(()))

//    Future.sequence(
//      messages
//        .filterNot(isAcceptedOrRefusalMessage(_))
//        .map(o => save(o, consignorId)))
//      .map(_ => successful(()))

//          println(s"MESSAGES": ${messages.})
    messages
      .filterNot(isAcceptedOrRefusalMessage(_))
      .foldLeft(successful(())) { case (acc, x) =>
        acc.flatMap {
          _ => save(x, consignorId).map { (x: Unit) => successful(x) }
        }
      }
  }

  private def save(message: IEMessage, consignorId: String)(implicit ec: ExecutionContext): Future[Unit] = {
    println(s"**=> ern: ${consignorId}, message: ${message.getType}")
    movementService.updateMovement( message, consignorId)
      .flatMap {
        case true => successful(())
        case _ =>
          logger.warn(s"Could not update movement for ern: $consignorId")
          successful(())
      }
  }

  private def isAcceptedOrRefusalMessage(message: IEMessage): Boolean =
    message.getType.equals(MessageTypes.IE801.value) ||
      message.getType.equals(MessageTypes.IE704.value)

}