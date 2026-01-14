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

package uk.gov.hmrc.excisemovementcontrolsystemapi.factories

import generated.v1
import generated.v2
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._

import scala.xml.NodeSeq

trait IEMessageFactory {
  type MessageOption = Either[DataRecord[v1.MessagesOption], DataRecord[v2.MessagesOption]]

  def createIEMessage(message: MessageOption): IEMessage

  def createFromXml(messageType: String, xml: NodeSeq): IEMessage
}

class IEMessageFactoryException(message: String) extends RuntimeException(message)
