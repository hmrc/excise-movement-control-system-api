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

import generated.IE815Type
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes

import scala.xml.NodeSeq

case class IE815Message (private val obj: IE815Type) extends IEMessage {
  def localReferenceNumber: String =
    obj.Body.SubmittedDraftOfEADESAD.EadEsadDraft.LocalReferenceNumber

  def consignorId: String =
    obj.Body.SubmittedDraftOfEADESAD.ConsignorTrader.TraderExciseNumber

  override def consigneeId: Option[String] =
    obj.Body.SubmittedDraftOfEADESAD.ConsigneeTrader.flatMap(_.Traderid)

  override def getErns: Set[String] = Set(consignorId)
  override def administrativeReferenceCode: Option[String] = None

  override def messageType: String = MessageTypes.IE815.value

  override def toXml: NodeSeq =
    scalaxb.toXML[IE815Type](obj, MessageTypes.IE815.value, generated.defaultScope)

  override def lrnEquals(lrn: String): Boolean = localReferenceNumber.equals(lrn)

}

object IE815Message {
  def createFromXml(xml: NodeSeq): IE815Message = {
    IE815Message(scalaxb.fromXML[IE815Type](xml))
  }
}