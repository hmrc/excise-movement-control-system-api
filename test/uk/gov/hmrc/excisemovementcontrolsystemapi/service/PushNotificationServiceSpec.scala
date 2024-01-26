package uk.gov.hmrc.excisemovementcontrolsystemapi.service

import org.mockito.MockitoSugar.verify
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MovementService, PushNotificationServiceImpl}

class PushNotificationServiceSpec extends PlaySpec {

  private val notificationConnector = mock[PushNotificationConnector]
  private val movementService = mock[MovementService]

  "sendNotification" should {
    "get a boxId" in {

      val sut = new PushNotificationServiceImpl(notificationConnector, movementService)

      await(sut.sendNotification)

//      verify(movementService).getBoxId
      fail()
    }

  }
}
