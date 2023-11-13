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

import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE810Message, IE813Message, IE815Message, IE818Message, IE819Message, IE837Message, IE871Message, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NrsMetadata

class NrsEventIdMapper {
  def mapMessageToEventId(message: IEMessage): String = {
    message match {
      case _: IE815Message => NrsMetadata.EmcsCreateMovementNotableEventId
      case _: IE810Message => NrsMetadata.EmccCancelMovement
      case _: IE813Message => NrsMetadata.EmcsChangeDestinationNotableEventId
      case _: IE818Message => NrsMetadata.EmcsReportOfReceiptNotableEvent
      case _: IE819Message => NrsMetadata.EmcsSubmitAlertOrRejectionNotableEventId
      case _: IE837Message => NrsMetadata.EmcsExplainADelayNotableEventId
      case _: IE871Message => NrsMetadata.EmcsExplainAShortageNotableEventId
      case _ => throw new RuntimeException(s"[NrsEventClientMapper] - Unsupported message: ${message.messageType}")
    }
  }

}
