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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models

import enumeratum.values.{StringEnum, StringEnumEntry, StringPlayJsonValueEnum}

import scala.collection.immutable

object MessageTypes {
  val IE815Message: String = "IE815"
  val IENewMessages: String = "IE_NEW_MESSAGES"
  val IEMessageReceipt: String = "IE_MESSAGE_RECEIPT"
}

//todo replace the aboce MessageType with the below enum
sealed abstract class MessageEumn (val value: String) extends StringEnumEntry

object MessageTypes1 extends StringEnum[MessageEumn] with StringPlayJsonValueEnum[MessageEumn] {
  val values: immutable.IndexedSeq[MessageEumn] = findValues

  case object IE815 extends MessageEumn("ie815")
  case object IE704 extends MessageEumn("ie704")
  case object IE801 extends MessageEumn("ie801")
  case object IE802 extends MessageEumn("ie802")
  case object IE_NEW_MESSAGES extends MessageEumn("ieNewMessage")
  case object IE_MESSAGE_RECEIPT extends MessageEumn("ieMessageReceipt")


  override def withValue(i: String): MessageEumn =
    super.withValue(i.toLowerCase)
}
