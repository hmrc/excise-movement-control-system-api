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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE801Message, IE810Message, IE813Message, IE815Message, IE818Message, IE819Message, IE837Message, IE871Message, IEMessage}

class ErnsMapper {
  /*
   The illegal state exception for IE818 message should never happen here,
   because these should have been caught previously during the validation.

   We are trying to get the ERN to use in the logs here, so want the one that is both in the auth and the message
 */
  def getSingleErnFromMessage(message: IEMessage, validErns: Set[String]) = {
    message match {
      case x: IE801Message => matchErn(x.consignorId, x.consigneeId, validErns, x.messageType)
      //For 810 & 813 we have no ERN in message so just use auth
      case _: IE810Message => validErns.head
      case _: IE813Message => validErns.head
      case x: IE815Message => x.consignorId
      case x: IE818Message => x.consigneeId.getOrElse(throw new IllegalStateException(s"[ErnMapper] - ern not supplied for message: ${x.messageType}"))
      case x: IE819Message => x.consigneeId.getOrElse(throw new IllegalStateException(s"[ErnMapper] - ern not supplied for message: ${x.messageType}"))
      case x: IE837Message => matchErn(x.consignorId, x.consigneeId, validErns, x.messageType)
      case x: IE871Message => x.consignorId.getOrElse(throw new IllegalStateException(s"[ErnMapper] - ern not supplied for message: ${x.messageType}"))
      case _ => throw new RuntimeException(s"[ErnMapper] - Unsupported Message Type: ${message.messageType}")
    }
  }

  private def matchErn(
                        consignorId: Option[String],
                        consigneeId: Option[String],
                        erns: Set[String],
                        messageType: String
                      ): String = {
    val messageErn: Set[String] = Set(consignorId, consigneeId).flatten
    val availableErn = erns.intersect(messageErn)

    if (availableErn.nonEmpty) availableErn.head
    else throw new IllegalStateException(s"[ErnMapper] - ern not supplied for $messageType message")
  }

}
