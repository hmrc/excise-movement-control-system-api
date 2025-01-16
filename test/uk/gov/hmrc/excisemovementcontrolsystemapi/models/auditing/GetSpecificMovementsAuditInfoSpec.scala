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

import java.util.UUID

class GetSpecificMovementsAuditInfoSpec extends PlaySpec {

  "GetSpecificMovementsAuditInfo.writes" should {
    "serializes the authExciseNumber sequence into a comma-seperated string" in {
      val uuid             = UUID.randomUUID().toString
      val request          = GetSpecificMovementRequestAuditInfo(uuid)
      val userDetails      = UserDetails("gatewayId", "groupIdentifier")
      val authExciseNumber = NonEmptySeq("ern1", Seq("ern2", "ern3"))

      val testModel =
        GetSpecificMovementAuditInfo(
          request = request,
          userDetails = userDetails,
          authExciseNumber = authExciseNumber
        )

      val output = Json.toJson(testModel)

      (output \ "authExciseNumber").get mustBe JsString("ern1,ern2,ern3")

    }
  }
}
