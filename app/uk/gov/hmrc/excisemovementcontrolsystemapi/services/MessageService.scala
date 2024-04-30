package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done

import scala.concurrent.Future

class MessageService {

  def updateMessages(ern: String): Future[Done] = {


    ???
  }

}


// note to self, never return future[unit]