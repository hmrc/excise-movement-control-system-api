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

import play.api.libs.json.{JsObject, JsValue}
import generated.v1
import generated.v2
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.MessageAuditType

import scala.xml.NodeSeq

trait IEMessage {
  def messageIdentifier: String

  def consigneeId: Option[String]
  def consignorId: Option[String]

  def administrativeReferenceCode: Seq[Option[String]]

  def messageType: String

  def toXml: NodeSeq

  def toJson: JsValue
  def toJsObject: JsObject

  def lrnEquals(lrn: String): Boolean

  def messageAuditType: MessageAuditType

  def correlationId: Option[String]

  def optionalLocalReferenceNumber: Option[String]
}

trait SubmitterTypeConverter {
  def convertSubmitterTypeV1(submitterType: v1.SubmitterType): ExciseTraderType =
    submitterType match {
      case v1.Number1Value31 => Consignor
      case v1.Number2Value30 => Consignee
    }

  def convertSubmitterTypeV2(submitterType: v2.SubmitterType): ExciseTraderType =
    submitterType match {
      case v2.Number1Value30 => Consignor
      case v2.Number2Value29 => Consignee
    }
}
