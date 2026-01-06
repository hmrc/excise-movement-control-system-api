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

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE810MessageV1, IE813MessageV1, IE815MessageV1, IE818MessageV1, IE819MessageV1, IE837MessageV1, IE871MessageV1, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NrsMetadata

class NrsEventIdMapper {
  def mapMessageToEventId(message: IEMessage): String =
    message match {
      case _: IE815MessageV1 => NrsMetadata.EmcsCreateMovementNotableEventId
      case _: IE810MessageV1 => NrsMetadata.EmccCancelMovement
      case _: IE813MessageV1 => NrsMetadata.EmcsChangeDestinationNotableEventId
      case _: IE818MessageV1 => NrsMetadata.EmcsReportOfReceiptNotableEvent
      case _: IE819MessageV1 => NrsMetadata.EmcsSubmitAlertOrRejectionNotableEventId
      case _: IE837MessageV1 => NrsMetadata.EmcsExplainADelayNotableEventId
      case _: IE871MessageV1 => NrsMetadata.EmcsExplainAShortageNotableEventId
      case _                 => throw new RuntimeException(s"[NrsEventClientMapper] - Unsupported message: ${message.messageType}")
    }

}
