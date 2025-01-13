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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import cats.data.{NonEmptyList, NonEmptySeq}
import play.api.libs.functional.syntax.toInvariantFunctorOps
import play.api.libs.json._

object CommonFormats extends CommonFormats

trait CommonFormats {

  implicit def nonEmptyListFormat[A: Format]: Format[NonEmptyList[A]] =
    Format
      .of[List[A]]
      .inmap(
        NonEmptyList.fromListUnsafe,
        _.toList
      )

  implicit def notEmptySeqFormat[A: Format]: Format[NonEmptySeq[A]] =
    Format
      .of[Seq[A]]
      .inmap(
        NonEmptySeq.fromSeqUnsafe,
        _.toSeq
      )

  val commaWriter: Writes[NonEmptySeq[String]] = (input: NonEmptySeq[String]) => Json.toJson(input.toSeq.mkString(","))
}
