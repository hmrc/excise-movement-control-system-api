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

import scala.xml.NodeSeq

trait IEMessage {
  // def not val as will be evaluated straight away
//  def localReferenceNumber: Option[String]
//  def consignorId: Option[String]
  def consigneeId: Option[String]
  def administrativeReferenceCode: Option[String]
  def messageType: String
  def toXml: NodeSeq
  def getErns: Set[String] //= Set(consignorId, consigneeId).flatten

  def lrnEquals(lrn: String): Boolean

}