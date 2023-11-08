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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.{IE801, IE810, IE815, IE818, IE837}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MessageService @Inject()(movementRepository: MovementRepository, implicit val executionContext: ExecutionContext) {

  private def getErnsUsingArc(arc: String): Future[Set[String]] = {

    val movementFuture = movementRepository.getMovementByARC(arc)

    val singleMovement = movementFuture.map {
      case Seq() => throw new RuntimeException(s"[MessageService] - Zero movements found for administrative reference code $arc")
      case head :: Nil => head
      case _ => throw new RuntimeException(s"[MessageService] - Multiple movements found for administrative reference code $arc")
    }

    singleMovement.map(movement => Set(Some(movement.consignorId), movement.consigneeId).flatten)

  }

  def getErns(ieMessage: IEMessage): Future[Set[String]] = {

    ieMessage.messageType match {
      case IE801.value =>
        val iE801Message = ieMessage.asInstanceOf[IE801Message]
        Future.successful(Set(iE801Message.consigneeId, iE801Message.consignorId).flatten)

      case IE810.value =>
        val arc = ieMessage.administrativeReferenceCode.getOrElse(throw new RuntimeException("put sensible message here"))
        getErnsUsingArc(arc)

      case IE815.value =>
        val ie815Message = ieMessage.asInstanceOf[IE815Message]
        Future.successful(Set(ie815Message.consignorId))

      case IE818.value =>
        val ie818Message = ieMessage.asInstanceOf[IE818Message]
        Future.successful(Set(ie818Message.consigneeId).flatten)

      case IE837.value =>
        val ie837Message = ieMessage.asInstanceOf[IE837Message]
        Future.successful(Set(ie837Message.consignorId, ie837Message.consigneeId).flatten)

      case _ => throw new RuntimeException(s"[MessageService] - Unsupported Message Type: ${ieMessage.messageType}")
    }

  }
}
