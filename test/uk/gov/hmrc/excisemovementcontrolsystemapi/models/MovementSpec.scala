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
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MovementSpec extends AnyFreeSpec with Matchers {

  private val oldFormat: OFormat[Movement] = Json.format[Movement]
  private val timestamp                    = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val movement                     =
    Movement(UUID.randomUUID().toString, Some("boxId"), "123", "345", Some("789"), None, timestamp, Seq.empty)

  "must read/write a Movement" in {
    val result = Json.toJson(movement).as[Movement]
    result mustEqual movement
  }

  "must read a movement from the old json representation" in {
    val result = Json.toJson(movement)(oldFormat).as[Movement]
    result mustEqual movement
  }
}
