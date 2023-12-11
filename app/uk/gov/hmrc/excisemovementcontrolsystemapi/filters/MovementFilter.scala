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

import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import java.time.Instant

sealed trait Filter {
  def filter(movements: Seq[Movement]): Seq[Movement]
}

case class FilterErn(ern: Option[String] = None) extends Filter {
  def filter(movements: Seq[Movement]): Seq[Movement] = {
    ern.fold[Seq[Movement]](movements)(e =>
      movements.filter(o => o.consignorId.equals(e))
    )
  }

}
case class FilterLrn(lrn: Option[String] = None) extends Filter {
  def filter(movements: Seq[Movement]): Seq[Movement] = {
    lrn.fold[Seq[Movement]](movements)(l =>
      movements.filter(o => o.localReferenceNumber.equals(l))
    )
  }
}

case class FilterArc(arc: Option[String] = None) extends Filter {
  def filter(movements: Seq[Movement]): Seq[Movement] = {
    arc.fold[Seq[Movement]](movements)(a =>
      movements.filter(o => o.administrativeReferenceCode.contains(a))
    )
  }
}

case class FilterUpdatedSince(updatedSince: Option[String] = None) extends Filter {
  def filter(movements: Seq[Movement]): Seq[Movement] = {
    updatedSince.fold[Seq[Movement]](movements)(a =>
    movements.filter(o => o.lastUpdated.isAfter(Instant.parse(a))))
  }
}

class FilterNothing extends Filter {
  def filter(movements: Seq[Movement]): Seq[Movement] = {
    movements
  }
}

case class MovementFilter (private val filter: Seq[Filter]) {

  def filterMovement(movements: Seq[Movement]): Seq[Movement] = {
    filter.foldLeft[Seq[Movement]](movements) {
      (result, e) => e.filter(result)
    }
  }
}

object MovementFilter {

  def empty = MovementFilter(Seq.empty)

  def and(filter: Seq[(String, Option[String])]): MovementFilter = {

    MovementFilter(
      filter match {
        case Nil => Seq(new FilterNothing)
        case m@ Seq(_,_*) => m.map (o =>
          o._1 match {
            case "ern" => FilterErn(o._2)
            case "lrn" => FilterLrn(o._2)
            case "arc" => FilterArc(o._2)
            case "lastUpdated" => FilterUpdatedSince(o._2)
            case _ => new FilterNothing
          }
        )
        case _ => Seq(new FilterNothing)
    })
  }
}
