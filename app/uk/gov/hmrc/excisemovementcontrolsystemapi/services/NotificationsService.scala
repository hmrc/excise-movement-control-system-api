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

import cats.implicits.toTraverseOps
import org.apache.pekko.Done
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ClientBoxIdRepository}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationsService @Inject() (
  clientBoxIdRepository: ClientBoxIdRepository,
  boxIdRepository: BoxIdRepository,
  pushNotificationService: PushNotificationService
                                     )(implicit hc: HeaderCarrier, ec: ExecutionContext) {
  def subscribeErns(clientId: String, erns: Seq[String]): Future[Done] = {
    clientBoxIdRepository.getBoxId(clientId).flatMap { maybeBoxId =>
      val boxId = maybeBoxId.getOrElse {
        val box = pushNotificationService.getBoxId(clientId)
          box.map(x => x.map(y => {
          clientBoxIdRepository.save(clientId, y)
          y
        }))
        box
      }
      erns.traverse { ern =>
        boxIdRepository.save(ern, boxId)
      }.as(Done)
    }
  }

}
