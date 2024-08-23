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
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Results.Ok
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NotificationsService.NoBoxIdError
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationsServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with IntegrationPatience with BeforeAndAfterEach {

  private val movementRepository   = mock[MovementRepository]
  private val boxIdRepository         = mock[BoxIdRepository]
  private val pushNotificationService = mock[PushNotificationService]
  private val dateTimeService         = mock[DateTimeService]

  private val notificationsService =
    new NotificationsService(boxIdRepository, pushNotificationService, movementRepository)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      movementRepository,
      boxIdRepository,
      pushNotificationService,
      dateTimeService
    )
  }

  "subscribeErns" - {

    "must add a relationship between the boxId for the given clientId and the given ERNs and update movements to add notifications for the relevant box id" in {

      val clientId = "clientId"
      val boxId = "testBox"
      val ern1 = "ern1"
      val ern2 = "ern2"

      when(pushNotificationService.getBoxId(any, any)(any)).thenReturn(Future.successful(Right(boxId)))
      when(boxIdRepository.save(any, any)).thenReturn(Future.successful(Done))
      when(movementRepository.addBoxIdToMessages(any, any)).thenReturn(Future.successful(Done))

      notificationsService.subscribeErns(clientId, Seq(ern1, ern2)).futureValue

      verify(pushNotificationService).getBoxId(eqTo(clientId), eqTo(None))(any)
      verify(boxIdRepository).save(ern1, boxId)
      verify(boxIdRepository).save(ern2, boxId)
      verify(movementRepository).addBoxIdToMessages(ern1, boxId)
      verify(movementRepository).addBoxIdToMessages(ern2, boxId)
    }

    "must fail with a NoBoxIdError when there is no box id for the client" in {

      val clientId = "clientId"
      val ern1 = "ern1"

      when(pushNotificationService.getBoxId(any, any)(any)).thenReturn(Future.successful(Left(Ok)))

      val result = notificationsService.subscribeErns(clientId, Seq(ern1)).failed.futureValue

      result mustBe NoBoxIdError(clientId)

      verify(pushNotificationService).getBoxId(eqTo(clientId), eqTo(None))(any)
      verify(boxIdRepository, times(0)).save(any, any)
      verify(movementRepository, times(0)).addBoxIdToMessages(any, any)
    }
  }
}
