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

package uk.gov.hmrc.excisemovementcontrolsystemapi.fixture

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ExciseMovementResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat

import java.time.Instant

trait MovementTestUtils {

  def createMovementResponse(
    consignorId: String,
    lrn: String,
    arc: String,
    consigneeId: Option[String],
    lastUpdated: Option[Instant]
  ): ExciseMovementResponse =
    ExciseMovementResponse(
      "cfdb20c7-d0b0-4b8b-a071-737d68dede5e",
      None,
      lrn,
      consignorId,
      consigneeId,
      Some(arc),
      lastUpdated.map(_.asStringInMilliseconds)
    )

  def createMovementResponseFromMovement(
    movement: Movement
  ): ExciseMovementResponse =
    ExciseMovementResponse(
      movement._id,
      None,
      movement.localReferenceNumber,
      movement.consignorId,
      movement.consigneeId,
      movement.administrativeReferenceCode,
      Some(movement.lastUpdated.asStringInMilliseconds)
    )

}
