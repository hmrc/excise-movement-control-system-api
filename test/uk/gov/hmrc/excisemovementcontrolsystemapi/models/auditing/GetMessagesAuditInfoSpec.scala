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

import cats.data.NonEmptySeq
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsArray, JsNull, JsString, Json}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Message

import java.time.Instant
import java.util.UUID

class GetMessagesAuditInfoSpec extends PlaySpec {

  "GetMessagesRequestAuditInfo.writes" should {
    "write json when updatedSince and traderType are present" in {
      val getMessagesRequestAuditInfo = GetMessagesRequestAuditInfo(
        "movementId",
        Some("timestamp"),
        Some("consignor")
      )

      val json = Json.toJson(getMessagesRequestAuditInfo)

      val expectedJson = Json.obj(
        "movementId"   -> "movementId",
        "updatedSince" -> "timestamp",
        "traderType"   -> "consignor"
      )

      json mustBe expectedJson
    }

    "write json when updatedSince and traderType are not present" in {
      val getMessagesRequestAuditInfo = GetMessagesRequestAuditInfo(
        "movementId",
        None,
        None
      )

      val json = Json.toJson(getMessagesRequestAuditInfo)

      val expectedJson = Json.obj(
        "movementId" -> "movementId"
      )

      json mustBe expectedJson
    }
  }

  "GetMessagesResponseAuditInfo.writes" should {
    "serialize administrativeReferenceCode as a null if None provided" in {
      val getMessagesResponseAuditInfo =
        GetMessagesResponseAuditInfo(1, Seq.empty[MessageAuditInfo], "lrn", None, "consignorId", Some("consigneeId"))
      val expectedResult               = Json.obj(
        "numberOfMessages"            -> 1,
        "message"                     -> JsArray.empty,
        "localReferenceNumber"        -> "lrn",
        "administrativeReferenceCode" -> JsNull,
        "consignorId"                 -> "consignorId",
        "consigneeId"                 -> "consigneeId"
      )

      val output = Json.toJson(getMessagesResponseAuditInfo)

      output mustBe expectedResult
      (output \ "administrativeReferenceCode").get mustBe JsNull

    }
  }

  "GetMessagesAuditInfo.writes" should {
    "serializes the authExciseNumber sequence into a comma-seperated string" in {
      val request          = GetMessagesRequestAuditInfo(
        "movementId",
        Some("timestamp"),
        Some("consignor")
      )
      val messages         = MessageAuditInfo(
        UUID.randomUUID().toString,
        Some("correlationId"),
        "IE815",
        "DraftMovement",
        "recipient",
        Instant.now()
      )
      val response         = GetMessagesResponseAuditInfo(
        1,
        Seq(messages),
        "lrn",
        Some("arc"),
        "consignorId",
        Some("consigneeId")
      )
      val userDetails      = UserDetails("gatewayId", "groupIdentifier")
      val authExciseNumber = NonEmptySeq("ern1", Seq("ern2", "ern3"))

      val testModel =
        GetMessagesAuditInfo(
          request = request,
          response = response,
          userDetails = userDetails,
          authExciseNumber = authExciseNumber
        )

      val output = Json.toJson(testModel)

      (output \ "authExciseNumber").get mustBe JsString("ern1,ern2,ern3")

    }
  }
}
