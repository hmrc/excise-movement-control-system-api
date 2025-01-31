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
import play.api.libs.json.{JsArray, JsNull, Json}

import java.util.UUID

class MessageProcessingAuditInfoSpec extends PlaySpec {

  "MessageProcessingSuccessAuditInfo.writes" should {
    "serialize jobId as a null if None provided" in {
      val batchId                           = UUID.randomUUID().toString
      val messageProcessingSuccessAuditInfo =
        MessageProcessingSuccessAuditInfo("ern", 10, 10, Seq.empty, batchId, None)

      val expectedResult = Json.obj(
        "exciseRegistrationNumber" -> "ern",
        "messagesAvailable"        -> 10,
        "messagesInBatch"          -> 10,
        "messages"                 -> JsArray.empty,
        "processingStatus"         -> "Success",
        "batchId"                  -> batchId,
        "jobId"                    -> JsNull
      )

      val output = Json.toJson(messageProcessingSuccessAuditInfo)

      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull

    }
  }

  "MessageProcessingFailureAuditInfo.writes" should {
    "serialize jobId as a null if None provided" in {
      val batchId                           = UUID.randomUUID().toString
      val messageProcessingSuccessAuditInfo =
        MessageProcessingFailureAuditInfo("ern", "Reason", batchId, None)

      val expectedResult = Json.obj(
        "exciseRegistrationNumber" -> "ern",
        "processingStatus"         -> "Failure",
        "failureReason"            -> "Reason",
        "batchId"                  -> batchId,
        "jobId"                    -> JsNull
      )

      val output = Json.toJson(messageProcessingSuccessAuditInfo)

      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull

    }
  }
}
