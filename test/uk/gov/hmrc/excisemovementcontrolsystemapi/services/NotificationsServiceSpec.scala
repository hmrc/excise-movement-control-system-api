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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ClientBoxIdRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationsServiceSpec
    extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach {

  private val clientBoxIdRepository   = mock[ClientBoxIdRepository]
  private val boxIdRepository         = mock[BoxIdRepository]
  private val pushNotificationService = mock[PushNotificationService]
  private val dateTimeService         = mock[DateTimeService]

  private val notificationsService = new NotificationsService(
    clientBoxIdRepository,
    boxIdRepository,
    pushNotificationService)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      clientBoxIdRepository,
      boxIdRepository,
      pushNotificationService,
      dateTimeService,
    )
  }

  "subscribe ERNs for client id" when {
    "subscribes all erns to the client's box Id" when {
      "the client's boxId isn't in the cache" when {
        "gets the client's default box and adds to the cache" in {
          val clientId = "testClient"
          val boxId = "testBox"
          val consignor = "consignor"
          val consignee = "consignee"
          when(clientBoxIdRepository.getBoxId(any)).thenReturn(Future.successful(None))
          when(clientBoxIdRepository.save(any, any)).thenReturn(Future.successful(Done))
          when(pushNotificationService.getBoxId(any, any)(any)).thenReturn(Future.successful(Right(boxId)))
          when(boxIdRepository.save(any, any)).thenReturn(Future.successful(Done))

          notificationsService.subscribeErns(clientId, Seq(consignor, consignee))

          verify(clientBoxIdRepository).getBoxId(clientId)
          verify(pushNotificationService).getBoxId(eqTo(clientId), eqTo(None))(any)
          verify(clientBoxIdRepository.save(clientId, boxId))
          verify(boxIdRepository.save(consignor, boxId))
          verify(boxIdRepository.save(consignee, boxId))
        }
      }
    }
  }
}
