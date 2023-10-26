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


sealed abstract class MessageEumn (val value: String) extends StringEnumEntry

/*
todo: we do need to account for all message here.
 Just thinking for the end of october we will not deploy all
 message, so we may getnew one that are not in this enum
*/

object MessageTypes extends StringEnum[MessageEumn] with StringPlayJsonValueEnum[MessageEumn] {
  val values: immutable.IndexedSeq[MessageEumn] = findValues

  case object IE815 extends MessageEumn("IE815")
  case object IE704 extends MessageEumn("IE704")
  case object IE801 extends MessageEumn("IE801")
  case object IE802 extends MessageEumn("IE802")
  case object IE810 extends MessageEumn("IE810")
  case object IE813 extends MessageEumn("IE813")
  case object IE818 extends MessageEumn("IE818")
  case object IE819 extends MessageEumn("IE819")
  case object IE837 extends MessageEumn("IE837")
  case object IE871 extends MessageEumn("IE871")
  case object IE_NEW_MESSAGES extends MessageEumn("IENewMessage")
  case object IE_MESSAGE_RECEIPT extends MessageEumn("IEMessageReceipt")
}

