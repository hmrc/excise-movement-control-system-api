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

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.GetMovementResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

trait MovementTestUtils {

  def createMovementResponse(
                              ern: String,
                              lrn: String,
                              arc: String,
                              consigneeId: Some[String]
                            ): GetMovementResponse = {
    GetMovementResponse(
      ern,
      lrn,
      consigneeId,
      Some(arc),
      "Accepted"
    )
  }

  def createMovementResponseFromMovement(
                             movement: Movement
                            ): GetMovementResponse = {
    GetMovementResponse(
      movement.consignorId,
      movement.localReferenceNumber,
      movement.consigneeId,
      movement.administrativeReferenceCode,
      "Accepted"
    )
  }

}
