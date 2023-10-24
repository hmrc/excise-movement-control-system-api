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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util

import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.libs.json.{Json, Reads}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, ServiceUnavailable}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorMessage, EISResponse}
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

class EISHttpReader(
  correlationId: String,
  consignorId: String,
  createdDateTime: String
) extends HttpReads[Either[Result, EISResponse]] with Logging {

    override def read(method: String, url: String, response: HttpResponse): Either[Result, EISResponse] = {
        val result = extractIfSuccessful[EISResponse](response)
        result match {
          case Right(eisResponse) => Right(eisResponse)
          case Left(httpResponse: HttpResponse) => Left(handleErrorResponse(httpResponse))
        }
    }

  private def handleErrorResponse
  (
    response: HttpResponse
  ): Result = {

    logger.warn(EISErrorMessage(createdDateTime, consignorId, response.body, correlationId, MessageTypes.IE815.value))

    val messageAsJson = response.json
    response.status match {
      case BAD_REQUEST => BadRequest(messageAsJson)
      case NOT_FOUND => NotFound(messageAsJson)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(messageAsJson)
      case _ => InternalServerError(messageAsJson)
    }
  }

  def extractIfSuccessful[T](response: HttpResponse)(implicit reads: Reads[T], tt: TypeTag[T]): Either[HttpResponse, T] =
    if (is2xx(response.status)) Right(jsonAs[T](response.body))
    else Left(response)

  private def jsonAs[T](body: String)(implicit reads: Reads[T],  tt: TypeTag[T]): T = {
    Try(Json.parse(body).as[T]) match {
      case Success(obj) => obj
      case Failure(exception) => throw new RuntimeException(s"Response body could not be read as type ${typeOf[T]}", exception)
    }
  }
}

object EISHttpReader {
  def apply(correlationId: String, consignorId: String, createDateTime: String): EISHttpReader = {
    new EISHttpReader(
      correlationId: String,
      consignorId: String,
      createDateTime: String
    )
  }
}
