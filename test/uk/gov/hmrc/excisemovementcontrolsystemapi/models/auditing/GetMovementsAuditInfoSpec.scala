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
import play.api.libs.json.{JsString, Json}

class GetMovementsAuditInfoSpec extends PlaySpec {

  "GetMovementsParametersAuditInfo.writes" should {
    "create an json when only ERN is populated" in {
      val request = GetMovementsParametersAuditInfo(Some("ern"), None, None, None, None)

      val json = Json.toJson(request)

      val expectedJson = Json.obj("exciseRegistrationNumber" -> "ern")

      json mustBe expectedJson
    }

    "create an json when only ERN and LRN are populated" in {
      val request = GetMovementsParametersAuditInfo(Some("ern"), None, Some("lrn"), None, None)

      val json = Json.toJson(request)

      val expectedJson = Json.obj("exciseRegistrationNumber" -> "ern", "localReferenceNumber" -> "lrn")

      json mustBe expectedJson
    }

    "create an json when all fields are populated" in {
      val request = GetMovementsParametersAuditInfo(Some("ern"), Some("arc"), Some("lrn"), Some("us"), Some("trader"))

      val json = Json.toJson(request)

      val expectedJson = Json.obj(
        "exciseRegistrationNumber"    -> "ern",
        "administrativeReferenceCode" -> "arc",
        "localReferenceNumber"        -> "lrn",
        "updatedSince"                -> "us",
        "traderType"                  -> "trader"
      )

      json mustBe expectedJson
    }
  }

  "GetMovementsAuditInfo.writes" should {
    "serializes the authExciseNumber sequence into a comma-seperated string" in {
      val request          = GetMovementsParametersAuditInfo(Some("ern"), None, None, None, None)
      val response         = GetMovementsResponseAuditInfo(5)
      val userDetails      = UserDetails("gatewayId", "groupIdentifier")
      val authExciseNumber = NonEmptySeq("ern1", Seq("ern2", "ern3"))

      val testModel =
        GetMovementsAuditInfo(
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
