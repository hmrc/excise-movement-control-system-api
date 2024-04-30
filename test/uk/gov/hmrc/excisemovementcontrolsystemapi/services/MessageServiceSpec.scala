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
