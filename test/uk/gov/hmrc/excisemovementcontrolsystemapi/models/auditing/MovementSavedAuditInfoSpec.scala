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

class MovementSavedAuditInfoSpec extends PlaySpec {

  "MovementSavedSuccessAuditInfo.writes" should {
    "serialise jobId as null when None is provided" in {

      val batchId                       = UUID.randomUUID().toString
      val movementSavedSuccessAuditInfo =
        MovementSavedSuccessAuditInfo(
          1,
          1,
          "movementId",
          Some("lrn"),
          Some("arc"),
          "consignorId",
          Some("consigneeId"),
          batchId,
          None,
          Seq.empty,
          Seq.empty
        )

      val expectedResult = Json.obj(
        "saveStatus"                  -> "Success",
        "messagesAdded"               -> 1,
        "totalMessages"               -> 1,
        "movementId"                  -> "movementId",
        "localReferenceNumber"        -> "lrn",
        "administrativeReferenceCode" -> "arc",
        "consignorId"                 -> "consignorId",
        "consigneeId"                 -> "consigneeId",
        "batchId"                     -> batchId,
        "jobId"                       -> JsNull,
        "keyMessageDetails"           -> JsArray.empty,
        "fullMessageDetails"          -> JsArray.empty
      )

      val output = Json.toJson(movementSavedSuccessAuditInfo)

      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull
    }
  }

  "MovementSavedFailureAuditInfo.writes" should {
    "serialise jobId as null when None is provided" in {

      val batchId = UUID.randomUUID().toString

      val movementSavedFailureAuditInfo = MovementSavedFailureAuditInfo(
        "Failure reason",
        1,
        1,
        "movementId",
        Some("lrn"),
        Some("arc"),
        "consignorId",
        Some("consigneeId"),
        batchId,
        None,
        Seq.empty,
        Seq.empty
      )

      val expectedResult = Json.obj(
        "saveStatus"                  -> "Failure",
        "failureReason"               -> "Failure reason",
        "messagesToBeAdded"           -> 1,
        "totalMessages"               -> 1,
        "movementId"                  -> "movementId",
        "localReferenceNumber"        -> "lrn",
        "administrativeReferenceCode" -> "arc",
        "consignorId"                 -> "consignorId",
        "consigneeId"                 -> "consigneeId",
        "batchId"                     -> batchId,
        "jobId"                       -> JsNull,
        "keyMessageDetails"           -> JsArray.empty,
        "fullMessageDetails"          -> JsArray.empty
      )

      val output = Json.toJson(movementSavedFailureAuditInfo)
      output mustBe expectedResult
      (output \ "jobId").get mustBe JsNull
    }
  }
}
