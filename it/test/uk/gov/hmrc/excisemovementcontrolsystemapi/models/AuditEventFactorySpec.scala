/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.{AuditEventFactory, Auditing}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

class AuditEventFactorySpec extends AnyFreeSpec with Matchers with Auditing with TestXml {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "IE704Message" - TestType(IE704TestMessageType, IE704Message.createFromXml(IE704))
  "IE801Message" - TestType(IE801TestMessageType, IE801Message.createFromXml(IE801))
  "IE802Message" - TestType(IE802TestMessageType, IE802Message.createFromXml(IE802))
  "IE803Message" - TestType(IE803TestMessageType, IE803Message.createFromXml(IE803))
  "IE807Message" - TestType(IE807TestMessageType, IE807Message.createFromXml(IE807))
  "IE810Message" - TestType(IE810TestMessageType, IE810Message.createFromXml(IE810))
  "IE813Message" - TestType(IE813TestMessageType, IE813Message.createFromXml(IE813))
  "IE815Message" - TestType(IE815TestMessageType, IE815Message.createFromXml(IE815))
  "IE818Message" - TestType(IE818TestMessageType, IE818Message.createFromXml(IE818))
  "IE819Message" - TestType(IE819TestMessageType, IE819Message.createFromXml(IE819))
  "IE829Message" - TestType(IE829TestMessageType, IE829Message.createFromXml(IE829))
  "IE837Message" - TestType(IE837TestMessageType, IE837Message.createFromXml(IE837WithConsignor))
  "IE839Message" - TestType(IE839TestMessageType, IE839Message.createFromXml(IE839))
  "IE840Message" - TestType(IE840TestMessageType, IE840Message.createFromXml(IE840))
  "IE871Message" - TestType(IE871TestMessageType, IE871Message.createFromXml(IE871WithConsignor))
  "IE881Message" - TestType(IE881TestMessageType, IE881Message.createFromXml(IE881))
  "IE905Message" - TestType(IE905TestMessageType, IE905Message.createFromXml(IE905))

  case class TestType(testObject: TestMessageType, message: IEMessage) {

    "successfully converted to success audit event" in {

      val result         = AuditEventFactory.createMessageAuditEvent(message, None)
      val expectedResult = ExtendedDataEvent(
        auditSource = "excise-movement-control-system-api",
        auditType = message.messageAuditType.name,
        detail = testObject.auditEvent
      )

      result.auditSource mustBe expectedResult.auditSource
      result.auditType mustBe expectedResult.auditType
      result.detail mustBe expectedResult.detail
    }

    "converted to failure audit event" in {
      val testMessage    = "Test Message"
      val result         = AuditEventFactory.createMessageAuditEvent(message, Some(testMessage))
      val expectedResult = ExtendedDataEvent(
        auditSource = "excise-movement-control-system-api",
        auditType = message.messageAuditType.name,
        detail = testObject.auditFailure(testMessage)
      )

      result.auditSource mustBe expectedResult.auditSource
      result.auditType mustBe expectedResult.auditType
      result.detail mustBe expectedResult.detail
    }
  }

  "createMessageSubmitted" - {
    "populates" in {}
    "populates administrativeReferenceCode as provided" in {} //Should cover both cases
    ""
    "populates movementId as provided" in {} //Should cover both cases

    "populates consigneeId as provided" in {}

    "submittedToCore true on success" in {}

    "submittedToCore false on success" in {}

  }
}
