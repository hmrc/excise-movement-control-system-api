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

class MovementFilterSpec extends PlaySpec {

  private val now = Instant.now()

  private val m1 = Movement("lrn3", "test1", Some("consigneeId"), Some("arc1"), now.plusSeconds(500))
  private val m2 = Movement("2", "test2", Some("consigneeId2"), Some("arc2"), now.plusSeconds(1000))
  private val m3 = Movement("5", "test2", Some("consigneeId2"), Some("arc3"), now.minusSeconds(1000))
  private val m4 = Movement("2", "test4", Some("consigneeId2"), Some("arc4"), now.minusSeconds(1000))
  private val m5 = Movement("lrn345", "test2abc", Some("consigneeId2"), Some("arc3fgn"), now.minusSeconds(1000))

  private val movements = Seq(m1, m2, m3, m4, m5)


  "filterMovement" should {
    "filter by LRN" in {
      val filter = MovementFilterBuilder().withLrn(Some("lrn3")).build()

      filter.filterMovement(movements) mustBe Seq(m1)
    }

    "filter by ERN" in {
      val filter = MovementFilterBuilder().withErn(Some("test1")).build()

      filter.filterMovement(movements) mustBe Seq(m1)
    }

    "filter by ERN should find consignee" in {
      val filter = MovementFilterBuilder().withErn(Some("consigneeId2")).build()

      filter.filterMovement(movements) mustBe Seq(m2, m3, m4, m5)
    }

    "filter by ARC" in {
      val filter = MovementFilterBuilder().withArc(Some("arc1")).build()

      filter.filterMovement(movements) mustBe Seq(m1)
    }

    "filter by updatedSince" in {
      val filter = MovementFilterBuilder().withUpdatedSince(Some(now.plusSeconds(700))).build()

      filter.filterMovement(movements) mustBe Seq(m2)
    }

    "filter by updatedSince and include movements with a updatedSince time that equals the filter time" in {
      val filter = MovementFilterBuilder().withUpdatedSince(Some(now.plusSeconds(500))).build()

      filter.filterMovement(movements) mustBe Seq(m1, m2)
    }

    "filter by ERN, LRN, ARC and updatedSince" in {
      val filter = MovementFilterBuilder()
        .withErn(Some("test2"))
        .withLrn(Some("2"))
        .withArc(Some("arc2"))
        .withUpdatedSince(Some(now)).build()

      filter.filterMovement(movements) mustBe Seq(m2)
    }

    "filter by ERN and LRN" in {
      val filter = MovementFilterBuilder().withErn(Some("test2")).withLrn(Some("2")).build()

      filter.filterMovement(movements) mustBe Seq(m2)
    }

    "not return any match for an LRN" in {
      val filter = MovementFilterBuilder().withLrn(Some("3")).withArc(Some("arc3")).build()

      filter.filterMovement(movements) mustBe Seq.empty
    }

    "not return any match for an ARC" in {
      val filter = MovementFilterBuilder().withArc(Some("3")).build()

      filter.filterMovement(movements) mustBe Seq.empty
    }

    "not filter" when {
      "empty builder" in {
        val filter = MovementFilterBuilder().build()

        filter.filterMovement(movements) mustBe movements
      }
    }

    "there is no filter" in {
      val filter = MovementFilterBuilder().withErn(None).withLrn(None).withArc(None).withUpdatedSince(None).build()

      filter.filterMovement(movements) mustBe movements
    }

    "filter is initialise as empty" in {
      val filter = MovementFilter.empty

      filter.filterMovement(movements) mustBe movements
    }
  }
}
