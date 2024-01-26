package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.google.inject.ImplementedBy
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PushNotificationConnector

import javax.inject.Inject
import scala.concurrent.Future

class PushNotificationServiceImpl @Inject()
(
  notificationConnector: PushNotificationConnector,
  movementService: MovementService
) {

  def sendNotification: Future[Boolean] = ???
}

@ImplementedBy(classOf[PushNotificationServiceImpl])
trait PushNotificationService {
  def sendNotification: Future[Boolean]
}
