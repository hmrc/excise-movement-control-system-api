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
import play.api.libs.json.{JsNull, JsString, Json}

import java.util.UUID

class GetSpecificMessageAuditInfoSpec extends PlaySpec {
  "GetSpecificMessageAuditInfo.writes" should {
    "serialize the authExciseNumber seqeuence into a comma-seperated string" in {
      val movementId = UUID.randomUUID().toString
      val messageId  = UUID.randomUUID().toString

      val request          = GetSpecificMessageRequestAuditInfo(movementId, messageId)
      val response         = GetSpecificMessageResponseAuditInfo(
        Some("testCorrelationId"),
        "IE801",
        "MovementGenerated",
        "lrn",
        Some("arc"),
        "consignorId",
        Some("consigneeId")
      )
      val userDetails      = UserDetails("gatewayId", "groupIdentifier")
      val authExciseNumber = NonEmptySeq("ern1", Seq("ern2", "ern3"))

      val testModel =
        GetSpecificMessageAuditInfo(
          request = request,
          response = response,
          userDetails = userDetails,
          authExciseNumber = authExciseNumber
        )

      val output = Json.toJson(testModel)

      (output \ "authExciseNumber").get mustBe JsString("ern1,ern2,ern3")

    }

  }

  "GetSpecificMessageResponseAuditInfo.writes" should {
    "serialize administrativeReferenceCode as a null if None provided" in {
      val getSpecificMessageResponseAuditInfo =
        GetSpecificMessageResponseAuditInfo(
          Some("correlationId"),
          "IE801",
          "MovementGenerated",
          "lrn",
          None,
          "consignorId",
          Some("consigneeId")
        )
      val expectedResult                      = Json.obj(
        "correlationId"               -> "correlationId",
        "messageTypeCode"             -> "IE801",
        "messageType"                 -> "MovementGenerated",
        "localReferenceNumber"        -> "lrn",
        "administrativeReferenceCode" -> JsNull,
        "consignorId"                 -> "consignorId",
        "consigneeId"                 -> "consigneeId"
      )

      val output = Json.toJson(getSpecificMessageResponseAuditInfo)

      output mustBe expectedResult
      (output \ "administrativeReferenceCode").get mustBe JsNull

    }
  }

}
