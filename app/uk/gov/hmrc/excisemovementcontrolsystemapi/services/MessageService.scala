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

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MessageService @Inject()(
  movementRepository: MovementRepository
)(implicit val executionContext: ExecutionContext) {

  private def getConsignorAndConsigneeFromArc(message: IEMessage): Future[Set[String]] = {
    message.administrativeReferenceCode match {
      case Some(a) =>  getMovementUsingArc(a).map(m => Set(Some(m.consignorId), m.consigneeId).flatten)
      case _ => throw new RuntimeException(s"[MessageService] - ${message.messageType} message must have an administrative reference code")
    }
  }

  private def getConsignorFromArc(message: IEMessage): Future[Set[String]] = {
    message.administrativeReferenceCode match {
      case Some(a) =>  getMovementUsingArc(a).map(m => Set(m.consignorId))
      case _ => throw new RuntimeException(s"[MessageService] - ${message.messageType} message must have an administrative reference code")
    }
  }

  private def getMovementUsingArc(arc: String): Future[Movement] = {
    movementRepository.getMovementByARC(arc).map {
      case Seq() => throw new RuntimeException(s"[MessageService] - Zero movements found for administrative reference code $arc")
      case head :: Nil => head
      case _ => throw new RuntimeException(s"[MessageService] - Multiple movements found for administrative reference code $arc")
    }
  }

  def getErns(ieMessage: IEMessage): Future[Set[String]] = {

    ieMessage match {
      case ie801: IE801Message => Future.successful(Set(ie801.consigneeId, ie801.consignorId).flatten)
      case ie810: IE810Message => getConsignorAndConsigneeFromArc(ie810)
      case ie813: IE813Message => getConsignorFromArc(ie813)
      case ie815: IE815Message => Future.successful(Set(ie815.consignorId))
      case ie818: IE818Message => Future.successful(Set(ie818.consigneeId).flatten)
      case ie819: IE819Message => Future.successful(Set(ie819.consigneeId).flatten)
      case ie837: IE837Message => Future.successful(Set(ie837.consignorId, ie837.consigneeId).flatten)
      case ie871: IE871Message => Future.successful(Set(ie871.consignorId).flatten)

      case _ => throw new RuntimeException(s"[MessageService] - Unsupported Message Type: ${ieMessage.messageType}")
    }
  }
}
