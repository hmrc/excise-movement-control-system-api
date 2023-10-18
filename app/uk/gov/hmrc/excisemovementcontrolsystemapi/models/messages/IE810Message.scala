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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages

import generated.{IE810Type, MessagesOption}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes

import scala.xml.NodeSeq

case class IE810Message
(
  private val obj: IE810Type,
  private val key: Option[String],
  private val namespace: Option[String]
) extends IEMessage {
  override def localReferenceNumber: Option[String] = None

  override def administrativeRefCode: Option[String] =
    Some(obj.Body.CancellationOfEAD.ExciseMovementEad.AdministrativeReferenceCode)

  override def toXml: NodeSeq = {
    val ns: String = namespace.fold(generated.defaultScope.uri)(o => o)
    scalaxb.toXML[IE810Type](obj, None, key, scalaxb.toScope(key -> ns))
  }

  override def getType: String = MessageTypes.IE810.value
}

object IE810Message {
  def apply(message: DataRecord[MessagesOption]): IE810Message = {
    IE810Message(message.as[IE810Type], message.key, message.namespace)
  }
}