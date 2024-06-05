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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

trait Auditing {

  val auditSource = "excise-movement-control-system-api"

  case class AuditDetail(messageCode: String, content: JsValue, failureOpt: Option[String]) {
    def toJsObj: JsObject = {

      val messageInfo = Json.obj(
        "messageCode" -> messageCode,
        "content"     -> content
      )

      val outcomeInfo = failureOpt match {
        case None         =>
          Json.obj("outcome" -> Json.obj("status" -> "SUCCESS"))
        case Some(reason) =>
          Json.obj("outcome" -> Json.obj("status" -> "FAILURE", "failureReason" -> reason))
      }

      messageInfo ++ outcomeInfo
    }
  }

  object AuditEventFactory {

    def createAuditEvent(input: IEMessage, failureOpt: Option[String])(implicit
      hc: HeaderCarrier
    ): ExtendedDataEvent = {
      val detail = AuditDetail(input.messageType, input.toJson, failureOpt)
      ExtendedDataEvent(
        auditSource = auditSource,
        auditType = input.auditType.name,
        tags = hc.toAuditTags(),
        detail = detail.toJsObj
      )
    }
  }

}
