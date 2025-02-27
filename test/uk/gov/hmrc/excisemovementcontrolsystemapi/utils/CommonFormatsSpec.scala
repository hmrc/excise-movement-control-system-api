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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import cats.data.{NonEmptyList, NonEmptySeq}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

import scala.util.Try

class CommonFormatsSpec extends PlaySpec with CommonFormats {
  "nonEmptyListFormat" should {
    "read a list with at least one value" in {
      val input  = Json.toJson(List(1, 2, 3))
      val result = nonEmptyListFormat[Int].reads(input)

      result.get mustBe NonEmptyList[Int](1, List(2, 3))
    }

    "fail when trying to parse an empty list" in {
      val input  = Json.toJson(List[Int]())
      val result = Try(nonEmptyListFormat[Int].reads(input))

      result.isFailure mustBe true
    }

    "serialize into a list" in {
      val input          = NonEmptyList(1, List(2, 3))
      val expectedResult = Json.toJson(List(1, 2, 3))
      val result         = Json.toJson(input)(nonEmptyListFormat[Int].writes(_))
      result mustBe expectedResult
    }
  }

  "nonEmptySeqFormat" should {
    "read a list with at least one value" in {
      val input  = Json.toJson(Seq(1, 2, 3))
      val result = notEmptySeqFormat[Int].reads(input)

      result.get mustBe NonEmptySeq[Int](1, List(2, 3))
    }

    "fail when trying to parse an empty list" in {
      val input  = Json.toJson(Seq[Int]())
      val result = Try(notEmptySeqFormat[Int].reads(input))

      result.isFailure mustBe true
    }

    "serialize into a list" in {
      val input          = NonEmptySeq(1, Seq(2, 3))
      val expectedResult = Json.toJson(Seq(1, 2, 3))
      val result         = Json.toJson(input)(notEmptySeqFormat[Int].writes(_))
      result mustBe expectedResult
    }
  }
}
