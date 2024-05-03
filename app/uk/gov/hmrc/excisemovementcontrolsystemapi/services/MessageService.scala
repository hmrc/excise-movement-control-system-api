/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import cats.syntax.all._
import org.apache.pekko.Done
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE704Message, IE801Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MovementCreator {
  def create(
    boxId: Option[String],
    localReferenceNumber: String,
    consignorId: String,
    consigneeId: Option[String],
    administrativeReferenceCode: Option[String] = None,
    lastUpdated: Instant = Instant.now,
    messages: Seq[Message] = Seq.empty
  ): Movement
}

@Singleton
class MessageService @Inject
(
  movementRepository: MovementRepository,
  ernRetrievalRepository: ErnRetrievalRepository,
  messageConnector: MessageConnector,
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils,
  movementCreator: MovementCreator = Movement.apply
)(implicit executionContext: ExecutionContext) {

  def updateMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] = {
    for {
      _ <- ernRetrievalRepository.getLastRetrieved(ern)
      messages <- messageConnector.getNewMessages(ern)
      _ <- updateMovements(ern, messages)
      _ <- ernRetrievalRepository.save(ern)
    } yield Done
  }

  private def updateMovements(ern: String, messages: Seq[IEMessage]): Future[Done] = {

    if (messages.nonEmpty) {
      movementRepository.getAllBy(ern).map { movements =>

        messages.foldLeft(Seq.empty[Movement]) { (updatedMovements, message) =>

          val movement = findByArc(updatedMovements, message) orElse
            findByLrn(updatedMovements, message) orElse
            findByArc(movements, message) orElse
            findByLrn(movements, message)

          (
            movement.map { movement =>
              movement.copy(messages = movement.messages :+ convertMessage(message))
            }.getOrElse {
              message match {
                case ie704: IE704Message => createMovementFromIE704(ern, ie704)
                case ie801: IE801Message => createMovementFromIE801(ern, ie801)
                case _ => ???
              }
            } +: updatedMovements
            ).distinctBy(_._id)
        }.traverse(movement => movementRepository.save(movement))
      }.as(Done)
    } else {
      Future.successful(Done)
    }
  }

//
//    if (messages.nonEmpty) {
//      movementRepository.getAllBy(ern).map { movements =>
//        messages.groupBy { message =>
//          lazy val arcMovement = findByArc(movements, message)
//          lazy val lrnMovement = findByLrn(movements, message)
//          arcMovement orElse lrnMovement
//        }.toSeq.traverse { case (maybeMovement, messages) =>
//          ifThereIsAMovement(maybeMovement).getOrElse {
////            ifThereIsNoMovement(messages)
//                      ???
//          }
//        }
//      }
//    }.as(Done) else {
//      Future.successful(Done)
//    }


//    def ifThereIsAMovement(maybeMovement: Option[Movement]) = {
//      maybeMovement.map { m =>
//        val updatedMovement = m.copy(messages = m.messages ++ messages.map(convertMessage))
//        movementRepository.updateMovement(updatedMovement)
//      }
//    }

//    def ifThereIsNoMovement(messages: Seq[IEMessage]) = {
//      // here group by arc or lrn here again
//      //      messages.groupBy(_.administrativeReferenceCode) ??? how? arc is a Seq[Option]
//
//      messages.traverse {
//        case ie704: IE704Message => createMovementFromIE704(ern, ie704)
//        case ie801: IE801Message => createMovementFromIE801(ern, ie801)
//      }
//    }
//  }

//  def groupMessagesByArc(messages: Seq[IEMessage]): Map[String, Seq[IEMessage]] = {
//
//    // arc is a seq opt because some message types can have multiple arcs
//    // this is because they send some messages by batch- same ERN but different arcs
//
//    // Seq[IEMessage] => Seq[IEMessage that has only 1 Option of arc in not a seq]
//
//    val thing2 = messages.groupBy(_.administrativeReferenceCode.flatten.head)
//
//    // oh you just do this it's simple..
//
//    println(thing2)
//    //
////    HashMap(
////      List(23XI00000000000000013) -> List(Message type: IE801, message identifier: GB00001, LRN: Some(lrnie8158976912), ARC: List(Some(23XI00000000000000013))),
////    List(23XI00000000000000012) -> List(Message type: IE801, message identifier: GB00001, LRN: Some(lrnie8158976912), ARC: List(Some(23XI00000000000000012)), Message type: IE802, message identifier: GB0002, ARC: List(Some(23XI00000000000000012))))
////
//    thing2
//  }

  private def createMovementFromIE704(consignor: String, message: IE704Message): Movement = {
    movementCreator.create(
      None,
      message.localReferenceNumber.get,
      consignor,
      None,
      administrativeReferenceCode = message.administrativeReferenceCode.head,
      messages = Seq(convertMessage(message))
    )
  }

  private def createMovementFromIE801(consignor: String, message: IE801Message): Movement = {
    movementCreator.create(
      None,
      message.localReferenceNumber.get,
      consignor,
      message.consigneeId,
      administrativeReferenceCode = message.administrativeReferenceCode.head,
      messages = Seq(convertMessage(message))
    )
  }

  private def findByArc(movements: Seq[Movement], message: IEMessage): Option[Movement] = {
    movements.find(movement =>
      movement.administrativeReferenceCode.exists(arc =>
        message.administrativeReferenceCode.flatten.contains(arc)
      )
    )
  }

  private def findByLrn(movements: Seq[Movement], message: IEMessage): Option[Movement] = {
    movements.find(movement => message.lrnEquals(movement.localReferenceNumber))
  }

  private def convertMessage(input: IEMessage): Message = {
    Message(
      encodedMessage = emcsUtils.encode(input.toXml.toString),
      messageType = input.messageType,
      messageId = input.messageIdentifier,
      createdOn = dateTimeService.timestamp()
    )
  }
}
