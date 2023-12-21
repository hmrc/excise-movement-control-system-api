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


sealed abstract class MessageEnum(val value: String) extends StringEnumEntry

/*
todo: we do need to account for all message here.
 Just thinking for the end of october we will not deploy all
 message, so we may getnew one that are not in this enum
*/

object MessageTypes extends StringEnum[MessageEnum] with StringPlayJsonValueEnum[MessageEnum] {
  val values: immutable.IndexedSeq[MessageEnum] = findValues

  case object IE704 extends MessageEnum("IE704")
  case object IE801 extends MessageEnum("IE801")
  case object IE802 extends MessageEnum("IE802")
  case object IE803 extends MessageEnum("IE803")
  case object IE807 extends MessageEnum("IE807")
  case object IE810 extends MessageEnum("IE810")
  case object IE813 extends MessageEnum("IE813")
  case object IE815 extends MessageEnum("IE815")
  case object IE818 extends MessageEnum("IE818")
  case object IE819 extends MessageEnum("IE819")
  case object IE829 extends MessageEnum("IE829")
  case object IE837 extends MessageEnum("IE837")
  case object IE840 extends MessageEnum("IE840")
  case object IE871 extends MessageEnum("IE871")
  case object IE_NEW_MESSAGES extends MessageEnum("IENewMessage")
  case object IE_MESSAGE_RECEIPT extends MessageEnum("IEMessageReceipt")
  case object IE_MOVEMENT_FOR_TRADER extends MessageEnum("IEMessageForTrader")
}

