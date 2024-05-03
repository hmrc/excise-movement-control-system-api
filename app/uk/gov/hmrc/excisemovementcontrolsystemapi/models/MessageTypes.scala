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


sealed abstract class MessageTypes(val value: String) extends StringEnumEntry

object MessageTypes extends StringEnum[MessageTypes] with StringPlayJsonValueEnum[MessageTypes] {
  val values: immutable.IndexedSeq[MessageTypes] = findValues

  case object IE704 extends MessageTypes("IE704")
  case object IE801 extends MessageTypes("IE801")
  case object IE802 extends MessageTypes("IE802")
  case object IE803 extends MessageTypes("IE803")
  case object IE807 extends MessageTypes("IE807")
  case object IE810 extends MessageTypes("IE810")
  case object IE813 extends MessageTypes("IE813")
  case object IE815 extends MessageTypes("IE815")
  case object IE818 extends MessageTypes("IE818")
  case object IE819 extends MessageTypes("IE819")
  case object IE829 extends MessageTypes("IE829")
  case object IE837 extends MessageTypes("IE837")
  case object IE839 extends MessageTypes("IE839")
  case object IE840 extends MessageTypes("IE840")
  case object IE871 extends MessageTypes("IE871")
  case object IE881 extends MessageTypes("IE881")
  case object IE905 extends MessageTypes("IE905")
  case object IE_NEW_MESSAGES extends MessageTypes("IENewMessage")
  case object IE_MESSAGE_RECEIPT extends MessageTypes("IEMessageReceipt")
  case object IE_MOVEMENT_FOR_TRADER extends MessageTypes("IEMessageForTrader")
}

