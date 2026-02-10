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

import cats.implicits.toTraverseOps
import cats.syntax.parallel._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MovementRepository, TransformLogRepository, TransformationRepository}
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.{ActorAttributes, Materializer, Supervision}
import org.mongodb.scala.model.Filters.equal
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Movement, TransformLog}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{EnhancedTransformationError, TransformService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.lock.{LockRepository, TimePeriodLockService}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoUtils.DuplicateKey

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import javax.inject.{Inject, Singleton}

@Singleton
class TransformJob @Inject() (
  val movementRepository: MovementRepository,
  transformationRepository: TransformationRepository,
  transformLogRepository: TransformLogRepository,
  transformService: TransformService,
  timeService: DateTimeService,
  val lockRepository: LockRepository
)(implicit val mat: Materializer)
    extends TimePeriodLockService
    with Logging {

  def name: String = "movement-transformation-job"

  implicit val ec: ExecutionContext = mat.executionContext
  override val lockId: String       = name
  override val ttl: Duration        = Duration.create(15, MINUTES)

  def execute(): Future[ScheduledJob.Result] = {
    logger.warn(s"$name started")
    val movementsCount   = movementRepository.collection.countDocuments().toFuture()
    val transformedCount = new AtomicInteger(0)
    val processorCount   = Runtime.getRuntime.availableProcessors()
    val done             = Source
      .fromPublisher(movementRepository.collection.find().batchSize(100).limit(25000))
      .zipWithIndex
      .mapAsync(1) { case (movement, index) =>
        // Renew the lock every 5000 movements
        if (index % 5000 == 0)
          withRenewedLock(Future.successful(movement))
        else
          Future.successful(Some(movement))
      }
      .collect { case Some(movement) => movement }
      .grouped(100)
      .mapAsyncUnordered(processorCount) { movements =>
        movements.flatTraverse[Future, Movement] { movement =>
          transformLogRepository.findLog(movement).map {
            case true  => Nil
            case false => List(movement)
          }
        }

      }
      .mapAsyncUnordered(processorCount) { mov =>
        mov
          .flatTraverse[Future, Movement] { movement =>
            val transformResult =
              Future.sequence(movement.messages.map { message =>
                transformService.transform(message.messageType, message.encodedMessage).map {
                  case Right(e)  => Right(message.copy(encodedMessage = e))
                  case Left(err) =>
                    logger.warn(
                      s"$name Error while transforming movement id: ${movement._id} message type: ${message.messageType} & message Id: ${message.messageId}"
                    )
                    Left(
                      EnhancedTransformationError(err, messageType = message.messageType, messageID = message.messageId)
                    )
                }
              })

            val updatedMovement = transformResult.map(a => a.collect { case Right(e) => e }).map { messages =>
              movement.copy(messages = messages)
            }

            val errorList = transformResult.map(a => a.collect { case Left(e) => e })

            for {
              updatedMov <- updatedMovement
              errList    <- errorList
              mov        <- logTransformation(movement, updatedMov, errList)
            } yield mov
          }

      }
      .mapAsyncUnordered(processorCount) { movements =>
        transformationRepository
          .updateMovements(movements)
          .flatMap { _ =>
            transformLogRepository
              .saveLogs(
                movements.map { m =>
                  TransformLog(
                    m._id,
                    isTransformSuccess = true,
                    errors = Nil,
                    lastUpdatedMovement = m.lastUpdated,
                    lastUpdatedLog = timeService.timestamp(),
                    m.messages.size
                  )
                }
              )
          }
          .recover { case DuplicateKey(e) =>
            logger.warn(s"Illegal State Duplicate key $e")
            false
          }
          .map { _ =>
            movements
          }
      }
      .withAttributes(ActorAttributes.withSupervisionStrategy { err =>
        logger.error(
          s"$name :  ${err.getClass.getName}  "
        )
        Supervision.resume
      })
      .map { movements =>
        movementsCount.foreach { count =>
          if (transformedCount.addAndGet(movements.length) % 1000 == 0)
            logger.warn(
              s"Progress: ${transformedCount.get()}/$count : ${((transformedCount.get().toDouble / count) * 100).round}%"
            )
        }
      }
      .run()
      .map { _ =>
        logger.warn(s"$name completed")
        ScheduledJob.Result.Completed
      }

    done
  }

  shouldRun().map {
    case true  => withRenewedLock(execute())
    case false => logger.warn("All movements are already transformed")
  }

  //atomic adder
  private def logTransformation(
    movement: Movement,
    updatedMovement: Movement,
    errList: Seq[EnhancedTransformationError]
  ) =
    if (updatedMovement.messages.size != movement.messages.size) {
      val log = TransformLog(
        updatedMovement._id,
        isTransformSuccess = false,
        errList,
        movement.lastUpdated,
        timeService.timestamp(),
        updatedMovement.messages.size
      )
      transformLogRepository.saveLog(log).map { _ =>
        Nil
      }

    } else {
      Future.successful(List(updatedMovement))
    }

  private def shouldRun(): Future[Boolean] = {
    val filter            = equal("isTransformSuccess", true)
    val movementsCount    = movementRepository.collection.countDocuments().toFuture()
    val transformLogCount = transformLogRepository.collection.countDocuments(filter).toFuture()

    for {
      movementCount <- movementsCount
      logCount      <- transformLogCount

    } yield movementCount > logCount
  }

}
