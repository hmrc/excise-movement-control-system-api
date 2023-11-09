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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MessageService @Inject()(movementRepository: MovementRepository, implicit val executionContext: ExecutionContext) {

  private def getErnsUsingArc(arc: String): Future[Set[String]] = {

    movementRepository.getMovementByARC(arc).map {
      case Seq() => throw new RuntimeException(s"[MessageService] - Zero movements found for administrative reference code $arc")
      case head :: Nil => Set(Some(head.consignorId), head.consigneeId).flatten
      case _ => throw new RuntimeException(s"[MessageService] - Multiple movements found for administrative reference code $arc")
    }

  }

  def getErns(ieMessage: IEMessage): Future[Set[String]] = {

    ieMessage match {
      case ie801Message: IE801Message =>
        Future.successful(Set(ie801Message.consigneeId, ie801Message.consignorId).flatten)

      case _: IE810Message =>
        val arc = ieMessage.administrativeReferenceCode.getOrElse(throw new RuntimeException("IE810 message must have an administrative reference code"))
        getErnsUsingArc(arc)

      case ie815Message: IE815Message =>
        Future.successful(Set(ie815Message.consignorId))

      case ie818Message: IE818Message =>
        Future.successful(Set(ie818Message.consigneeId).flatten)

      case ie837Message: IE837Message =>
        Future.successful(Set(ie837Message.consignorId, ie837Message.consigneeId).flatten)

      case ie871Message: IE871Message =>
        Future.successful(Set(ie871Message.consignorId).flatten)

      case _ => throw new RuntimeException(s"[MessageService] - Unsupported Message Type: ${ieMessage.messageType}")
    }

  }
}
