package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec

class MessageServiceSpec extends PlaySpec with ScalaFutures {

  "updateMessages" when {
    "there is a movement but we have never retrieved anything" when {
      "we try to retrieve messages but there are none" should {
        "return Done" in {
          val service = new MessageService

          service.updateMessages("testErn").futureValue
          // look in db, find most recent submitted date
          // look in new collection, last retrieved date for this ern
          // there won't be one.
          // call the connector to get messages for this ern
          // record the lastRetrieved date
          // make sure call happens etc
        }
      }
    }
    "there is a new movement since we last retrieved messages" should {
      "go and get messages" in {
//        MessagesService.updateMessages()

      }
    }
  }

}
