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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.v2._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NrsMetadata

class NrsEventIdMapperV2 extends NrsEventIdMapper {
  def mapMessageToEventId(message: IEMessage): String =
    message match {
      case _: IE815MessageV2 => NrsMetadata.EmcsCreateMovementNotableEventId
      case _: IE810MessageV2 => NrsMetadata.EmccCancelMovement
      case _: IE813MessageV2 => NrsMetadata.EmcsChangeDestinationNotableEventId
      case _: IE818MessageV2 => NrsMetadata.EmcsReportOfReceiptNotableEvent
      case _: IE819MessageV2 => NrsMetadata.EmcsSubmitAlertOrRejectionNotableEventId
      case _: IE837MessageV2 => NrsMetadata.EmcsExplainADelayNotableEventId
      case _: IE871MessageV2 => NrsMetadata.EmcsExplainAShortageNotableEventId
      case _                 => throw new RuntimeException(s"[NrsEventClientMapper] - Unsupported message: ${message.messageType}")
    }

}
