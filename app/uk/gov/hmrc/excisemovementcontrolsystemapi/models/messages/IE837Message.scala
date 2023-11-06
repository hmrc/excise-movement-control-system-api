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

import generated.{IE837Type, MessagesOption, Number1Value31, Number2Value30}
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes

import scala.xml.NodeSeq


case class IE837Message
(
  private val obj: IE837Type,
  private val key: Option[String],
  private val namespace: Option[String]
) extends IEMessage {
  def consignorId: Option[String] = {
    if (obj.Body.ExplanationOnDelayForDelivery.AttributesValue.SubmitterType == Number1Value31) { //Generated class for SubmitterType 1
      Some(obj.Body.ExplanationOnDelayForDelivery.AttributesValue.SubmitterIdentification)
    } else None
  }

  override def consigneeId: Option[String] =
    if (obj.Body.ExplanationOnDelayForDelivery.AttributesValue.SubmitterType == Number2Value30) { //Generated class for SubmitterType 2
      Some(obj.Body.ExplanationOnDelayForDelivery.AttributesValue.SubmitterIdentification)
    } else None

  override def getErns: Set[String] = Set(consignorId, consigneeId).flatten

  override def administrativeReferenceCode: Option[String] =
    Some(obj.Body.ExplanationOnDelayForDelivery.ExciseMovement.AdministrativeReferenceCode)

  override def messageType: String = MessageTypes.IE837.value

  override def toXml: NodeSeq = {
    scalaxb.toXML[IE837Type](obj, namespace, key, generated.defaultScope)
  }

  override def lrnEquals(lrn: String): Boolean = false
}

object IE837Message {
  def apply(message: DataRecord[MessagesOption]): IE837Message = {
    IE837Message(message.as[IE837Type], message.key, message.namespace)
  }

  def createFromXml(xml: NodeSeq): IE837Message = {
    val ie837: IE837Type = scalaxb.fromXML[IE837Type](xml)
    IE837Message(ie837, Some(ie837.productPrefix), None)
  }
}