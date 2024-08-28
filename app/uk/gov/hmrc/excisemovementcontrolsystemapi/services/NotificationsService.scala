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

import cats.data.EitherT
import cats.implicits.{toFunctorOps, toTraverseOps}
import org.apache.pekko.Done
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService.NoBoxIdError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationsService @Inject() (
  boxIdRepository: BoxIdRepository,
  pushNotificationService: PushNotificationService,
  movementRepository: MovementRepository
)(implicit ec: ExecutionContext) {

  def subscribeErns(clientId: String, erns: Seq[String])(implicit hc: HeaderCarrier): Future[Done] =
    for {
      boxId <- getClientBoxId(clientId)
      _     <- saveBoxIdForErns(boxId, erns)
    } yield Done

  def unsubscribeErns(clientId: String, erns: Seq[String])(implicit hc: HeaderCarrier): Future[Done] =
    for {
      boxId <- getClientBoxId(clientId)
      _     <- removeBoxIdForErns(boxId, erns)
    } yield Done

  private def getClientBoxId(clientId: String)(implicit hc: HeaderCarrier): Future[String] =
    EitherT(pushNotificationService.getBoxId(clientId)).toOption.getOrElseF {
      Future.failed(NoBoxIdError(clientId))
    }

  private def saveBoxIdForErns(boxId: String, erns: Seq[String]): Future[Done] =
    erns
      .traverse { ern =>
        for {
          _ <- boxIdRepository.save(ern, boxId)
          _ <- movementRepository.addBoxIdToMessages(ern, boxId)
        } yield Done
      }
      .as(Done)

  private def removeBoxIdForErns(boxId: String, erns: Seq[String]): Future[Done] =
    erns
      .traverse { ern =>
        for {
          _ <- boxIdRepository.delete(ern, boxId)
          _ <- movementRepository.removeBoxIdFromMessages(ern, boxId)
        } yield Done
      }
      .as(Done)
}

object NotificationsService {

  final case class NoBoxIdError(clientId: String) extends Throwable {
    override def getMessage: String = s"No box id found in PPNS for $clientId"
  }
}
