/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, Json}

import java.util.UUID

class MessageAcknowledgedAuditInfoSpec extends PlaySpec {

  "MessageAcknowledgedFailureAuditInfo.writes" should {
    "serialize jobId as a null if None provided" in {
      val batchId                             = UUID.randomUUID().toString
      val messageAcknowledgedFailureAuditInfo =
        MessageAcknowledgedFailureAuditInfo("test message", batchId, None, "ern")

      val expectedResult = Json.obj(
        "acknowledgementStatus"    -> "Failure",
        "failureReason"            -> "test message",
        "batchId"                  -> batchId,
        "jobId"                    -> JsNull,
        "exciseRegistrationNumber" -> "ern"
      )

      val output = Json.toJson(messageAcknowledgedFailureAuditInfo)

      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull

    }
  }

  "MessageAcknowledgedSuccessAuditInfo.writes" should {
    "serialize jobId as a null if None provided" in {
      val batchId                             = UUID.randomUUID().toString
      val messageAcknowledgedSuccessAuditInfo =
        MessageAcknowledgedSuccessAuditInfo(batchId, None, "ern", 1)

      val expectedResult = Json.obj(
        "acknowledgementStatus"    -> "Success",
        "batchId"                  -> batchId,
        "jobId"                    -> JsNull,
        "exciseRegistrationNumber" -> "ern",
        "recordsAffected"          -> 1
      )

      val output = Json.toJson(messageAcknowledgedSuccessAuditInfo)

      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull

    }
  }
}
