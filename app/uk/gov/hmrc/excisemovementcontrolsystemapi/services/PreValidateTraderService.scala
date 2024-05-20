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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.google.inject.Singleton
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PreValidateTraderConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.ParsedPreValidateTraderRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{PreValidateTraderEISResponse, PreValidateTraderMessageResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreValidateTraderService @Inject()(
  connector: PreValidateTraderConnector,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext) extends Logging {

  def submitMessage[A](request: ParsedPreValidateTraderRequest[A])(implicit hc: HeaderCarrier): Future[Either[Result, PreValidateTraderMessageResponse]] = {
    connector.submitMessage(request.json, request.request.erns.head)
      .map {
        case Right(x) => Right(convertEISToResponseFormat(x)).flatten
        case Left(error) => Left(error)
      }
  }

  private def convertEISToResponseFormat(eisResponse: PreValidateTraderEISResponse): Either[Result, PreValidateTraderMessageResponse] = {
    val exciseTraderValidationResponse = eisResponse.exciseTraderValidationResponse
    // EIS returns an array of size one all the time so if that's not the case we need to log an error
    if (exciseTraderValidationResponse.exciseTraderResponse.length == 1) {
      val exciseTraderResponse = exciseTraderValidationResponse.exciseTraderResponse.head
      Right(
        PreValidateTraderMessageResponse(
          exciseTraderValidationResponse.validationTimestamp,
          exciseTraderResponse.exciseRegistrationNumber,
          exciseTraderResponse.entityGroup,
          exciseTraderResponse.validTrader,
          exciseTraderResponse.errorCode,
          exciseTraderResponse.errorText,
          exciseTraderResponse.traderType,
          exciseTraderResponse.validateProductAuthorisationResponse
        )
      )
    } else {
      logger.warn(s"[PreValidateTraderService] - PreValidateTrader response from EIS did not match expected format." +
        s" Response: $eisResponse")

      Left(InternalServerError(Json.toJson(ErrorResponse(
        dateTimeService.timestamp(),
        "PreValidateTrader Error",
        "Failed to parse preValidateTrader response"
      ))))
    }
  }
}
