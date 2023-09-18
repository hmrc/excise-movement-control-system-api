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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISRequest, EISResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MovementMessageConnector @Inject()(http: HttpClient, eisUtils: EisUtils) {

  def post(message: String, messageType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpResponse, EISResponse]] = {

    val eisRequest = EISRequest(eisUtils.generateCorrelationId, eisUtils.getCurrentDateTimeString, messageType, "APIP", "user1", message)
    val json: JsValue = Json.toJson(eisRequest)

    http.POST[JsValue, EISResponse]("http://localhost:9000/emcs-api-eis-stub/eis/receiver/v1/messages", json, getJsonHeaders(eisRequest)(hc))
      .map(
        response => Right(response)
      )
  }

  protected def getJsonHeaders(eisRequest: EISRequest)(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    Seq(HeaderNames.ACCEPT -> ContentTypes.JSON,
      HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
      "dateTime" -> eisRequest.createdDateTime,
      "x-correlation-id" -> eisRequest.emcsCorrelationId,
      "x-forwarded-host" -> "",
      "source" -> eisRequest.source)
  }

}
