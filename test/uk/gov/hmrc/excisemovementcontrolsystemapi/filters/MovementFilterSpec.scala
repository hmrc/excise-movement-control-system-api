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

package uk.gov.hmrc.excisemovementcontrolsystemapi.filters

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import java.time.Instant
import java.time.format.DateTimeParseException

class MovementFilterSpec extends PlaySpec {

  val now = Instant.now()

  private val m1 = Movement("lrn3", "test1", Some("consigneeId"), Some("arc1"), now.plusSeconds(500))
  private val m2 = Movement("2", "test2", Some("consigneeId2"), Some("arc2"), now.plusSeconds(1000))
  private val m3 = Movement("5", "test2", Some("consigneeId2"), Some("arc3"), now.minusSeconds(1000))
  private val m4 = Movement("2", "test4", Some("consigneeId2"), Some("arc4"), now.minusSeconds(1000))
  private val m5 = Movement("lrn345", "test2abc", Some("consigneeId2"), Some("arc3fgn"), now.minusSeconds(1000))

  private val movements = Seq(m1, m2, m3, m4, m5)


  "filterMovement" should {
    "filter by LRN" in {
      val filter = MovementFilter.and(Seq("lrn" -> Some("lrn3")))

      filter.filterMovement(movements) mustBe Seq(m1)
    }

    "filter by ERN" in {
      val filter = MovementFilter.and(Seq("ern" -> Some("test1")))

      filter.filterMovement(movements) mustBe Seq(m1)
    }

    "filter by ARC" in {
      val filter = MovementFilter.and(Seq("arc" -> Some("arc1")))

      filter.filterMovement(movements) mustBe Seq(m1)
    }

    "filter by updatedSince" in {
      val filter = MovementFilter.and(Seq("updatedSince" -> Some(now.plusSeconds(700).toString())))

      filter.filterMovement(movements) mustBe Seq(m2)
    }

    "filter by updatedSince and include movements with a updatedSince time that equals the filter time" in {
      val filter = MovementFilter.and(Seq("updatedSince" -> Some(now.plusSeconds(500).toString())))

      filter.filterMovement(movements) mustBe Seq(m1, m2)
    }

    "filter by ERN, LRN, ARC and updatedSince" in {
      val filter = MovementFilter.and(Seq(
        "lrn" -> Some("2"),
        "ern" -> Some("test2"),
        "arc" -> Some("arc2"),
        "updatedSince" -> Some(now.toString())
      ))

      filter.filterMovement(movements) mustBe Seq(m2)
    }

    "filter by ERN and LRN" in {
      val filter = MovementFilter.and(Seq(
        "lrn" -> Some("2"),
        "ern" -> Some("test2")
      ))

      filter.filterMovement(movements) mustBe Seq(m2)
    }

    "not return any match for an LRN" in {

      val filter = MovementFilter.and(Seq(
        "ern" -> None,
        "lrn" -> Some("3"),
        "arc" -> Some("arc3")
      ))

      filter.filterMovement(movements) mustBe Seq.empty
    }

    "not return any match for an ARC" in {

      val filter = MovementFilter.and(Seq(
        "ern" -> None,
        "lrn" -> None,
        "arc" -> Some("3")
      ))

      filter.filterMovement(movements) mustBe Seq.empty
    }

    "not filter" when {
      "empty sequence" in {
        val filter = MovementFilter.and(Seq.empty)

        filter.filterMovement(movements) mustBe movements
      }
    }

    "there is no filter" in {
      val filter = MovementFilter.and(Seq("ern" -> None, "lrn" -> None, "arc" -> None, "updatedSince" -> None))

      filter.filterMovement(movements) mustBe movements
    }

    "filter is initialise as empty" in {
      val filter = MovementFilter.empty

      filter.filterMovement(movements) mustBe movements
    }

    "succeed when a valid date format is provided" in {
      val filter = MovementFilter.and(Seq("updatedSince" -> Some("2020-11-15T17:02:34.00Z")))

      filter.filterMovement(movements) mustBe Seq(m1, m2, m3, m4, m5)
    }

    "fail when an invalid date format is provided" in {
      intercept[DateTimeParseException] {
        val filter = MovementFilter.and(Seq("updatedSince" -> Some("invalidDate")))
        filter.filterMovement(movements)
      }.getMessage mustBe "Text 'invalidDate' could not be parsed at index 0"
    }

  }
}
