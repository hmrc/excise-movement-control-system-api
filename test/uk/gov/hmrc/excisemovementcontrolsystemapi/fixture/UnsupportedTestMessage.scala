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

package uk.gov.hmrc.excisemovementcontrolsystemapi.fixture

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage

import scala.xml.NodeSeq

object UnsupportedTestMessage extends IEMessage {
  override def consigneeId: Option[String] = None

  override def administrativeReferenceCode: Seq[Option[String]] = Seq.empty

  override def messageType: String = "any-type"

  override def toXml: NodeSeq = NodeSeq.Empty

  override def lrnEquals(lrn: String): Boolean = false

  override def messageIdentifier: String = "message-id"
}
