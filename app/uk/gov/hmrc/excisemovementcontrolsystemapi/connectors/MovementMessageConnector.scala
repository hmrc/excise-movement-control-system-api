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

import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.libs.json.{Json, Reads}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util.CustomHttpReader
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EisUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.Header.EmcsSource
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISRequest, EISResponse, Header}
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

class MovementMessageConnector @Inject()
(
  httpClient: HttpClient,
  eisUtils: EisUtils,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) extends Logging {


  def post(message: String, messageType: String)(implicit hc: HeaderCarrier): Future[Either[Result, EISResponse]] = {

    implicit val customResponseReads: HttpReads[HttpResponse] = CustomHttpReader

    //todo: add metrics
    //todo: add retry
    //todo: message need to be encode Base64
    val correlationId = eisUtils.generateCorrelationId
    val createdDateTime = eisUtils.getCurrentDateTimeString
    val eisRequest = EISRequest(correlationId, createdDateTime, messageType, EmcsSource, "user1", message)

    httpClient.POST[EISRequest, HttpResponse](
      appConfig.emcsReceiverMessageUrl,
      eisRequest,
      Header.build(correlationId, createdDateTime)
    ).map(
      response => {
        val result = extractIfSuccessful[EISResponse](response)
        result match {
          case Right(eisResponse) => Right(eisResponse)
          case Left(httpResponse: HttpResponse) => Left(handleErrorResponse(httpResponse.status, httpResponse.body, correlationId))
        }
      }
    ).recover {
      case ex: Throwable =>
        logger.error(s"EIS error with message: ${ex.getMessage} and correlationId: $correlationId", ex)
        Left(InternalServerError(ex.getMessage))
    }
  }

  private def jsonAs[T](body: String)(implicit reads: Reads[T],  tt: TypeTag[T]): T = {
    Try(Json.parse(body).as[T]) match {
      case Success(obj) => obj
      case Failure(exception) => throw new RuntimeException(s"Response body could not be read as type ${typeOf[T]}", exception)
    }
  }

  private def handleErrorResponse(
    status: Int,
    message: String,
    correlationId: String
  ): Result = {

    logger.error(s"EIS error with message: $message, status: $status and correlationId: $correlationId")
    status match {
      case BAD_REQUEST => BadRequest(message)
      case NOT_FOUND => NotFound(message)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(message)
      case _ => InternalServerError(message)
    }
  }

  protected def extractIfSuccessful[T](response: HttpResponse)(implicit reads: Reads[T], tt: TypeTag[T]): Either[HttpResponse, T] =
    if (is2xx(response.status)) Right(jsonAs[T](response.body))
    else Left(response)
}
