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

import play.api.libs.json.JsValue
import generated.{Number1Value31, Number2Value30, SubmitterType}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.AuditType

import scala.xml.NodeSeq

trait IEMessage  {
  def messageIdentifier: String

  def consigneeId: Option[String]

  //todo:
  // here it feel wrong returning a Seq[Option[String]]
  // for every message when only the IE829 has a list of ARC while all the other have definitely
  // one. We may should not derived the administrativeReferenceCode from the interface and
  // use a matching/visitor pattern to retrieve this for each single message.
  def administrativeReferenceCode: Seq[Option[String]]

  def messageType: String

  def toXml: NodeSeq

  def toJson: JsValue

  def lrnEquals(lrn: String): Boolean

  def auditType: AuditType
}

trait SubmitterTypeConverter {
  def convertSubmitterType(submitterType: SubmitterType): ExciseTraderType = {
    submitterType match {
      case Number1Value31 => Consignor
      case Number2Value30 => Consignee
    }
  }
}