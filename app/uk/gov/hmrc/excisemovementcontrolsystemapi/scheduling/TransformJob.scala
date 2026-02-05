/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits.toTraverseOps
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MovementRepository, TransformLogRepository, TransformationRepository}
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.{ActorAttributes, Materializer, Supervision, SystemMaterializer}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.Done
import org.mongodb.scala._
import play.api.Play.materializer
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Movement, TransformLog}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{EnhancedTransformationError, TransformService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.lock.{LockRepository, LockService}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoUtils.DuplicateKey

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import javax.inject.{Inject, Singleton}
import scala.util.control.NonFatal

@Singleton
class TransformJob @Inject()(
                              configuration: Configuration,
                              val movementRepository: MovementRepository,
                              transformationRepository: TransformationRepository,
                              transformLogRepository: TransformLogRepository,
                              transformService: TransformService,
                              timeService: DateTimeService,
                              val lockRepository: LockRepository
                            )(implicit val mat: Materializer) extends LockService with Logging {

  def name: String = "movement-transformation-job"

  implicit val ec: ExecutionContext = mat.executionContext
  override val lockId: String = name
  override val ttl: Duration = Duration.create(15, MINUTES)

  def execute(): Future[ScheduledJob.Result] = {

    val done = Source.fromPublisher(movementRepository.collection.find().batchSize(10))
      .mapAsync(1) {
        movement =>
          transformLogRepository.findLog(movement).map {
            case true => None
            case false => Some(movement)
          }
      }
      .mapAsync(1) {
        mov =>
          mov.map {
            movement =>
              val transformResult =
                Future.sequence(movement.messages.map {
                  message =>
                    transformService.transform(message.messageType, message.encodedMessage, message.messageId).map {
                      case Right(e) => Right(message.copy(encodedMessage = e))
                      case Left(err) =>
                        logger.warn(s"$name Error while transforming movement id: ${movement._id} message type: ${message.messageType} & message Id: ${message.messageId}")
                        Left(EnhancedTransformationError(err, messageType = message.messageType, messageID = message.messageId))
                    }
                })

              val updatedMovement = transformResult.map { a => a.collect { case Right(e) => e } }.map {
                messages => movement.copy(messages = messages)
              }

              val errorList = transformResult.map(a => a.collect { case Left(e) => e })

              for {
                updatedMov <- updatedMovement
                errList <- errorList
                mov <- transformAndLog(movement, updatedMov, errList)
              } yield {
                mov
              }
          }.getOrElse(Future.successful(None))
      }.mapAsync(1) {
        movement =>
          movement.map {
            m =>
              transformationRepository.saveMovements(Seq(m)).flatMap {
                _ => transformLogRepository.saveLog(TransformLog(m._id, isTransformSuccess = true, errors = Nil, lastUpdated = timeService.timestamp()))
              }.recover {
                case DuplicateKey(e) => logger.warn(s"Illegal State Duplicate key $e")
              }
          }.traverse(identity)

      }.withAttributes(ActorAttributes.withSupervisionStrategy { err =>
        logger.error(
          s"$name :  ${err.getClass.getName}"
        )
        Supervision.resume
      }).run().map {
        _ => ScheduledJob.Result.Completed
      }
    done
  }

  withLock(execute())

  //atomic adder
  def transformAndLog(movement: Movement, updatedMovement: Movement, errList: Seq[EnhancedTransformationError]) = {
    if (updatedMovement.messages.size != movement.messages.size) {
      val log = TransformLog(updatedMovement._id, isTransformSuccess = false, errList, timeService.timestamp())
      transformLogRepository.saveLog(log).map {
        _ => None
      }

    } else {
      Future.successful(Some(updatedMovement))
    }
  }
}

