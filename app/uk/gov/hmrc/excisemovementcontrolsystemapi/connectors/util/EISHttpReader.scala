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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorMessage, EISSubmissionResponse}
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

class EISHttpReader(
                     val correlationId: String,
                     val ern: String,
                     val createdDateTime: String
                   ) extends HttpReads[Either[Result, EISSubmissionResponse]] with Logging {

  override def read(method: String, url: String, response: HttpResponse): Either[Result, EISSubmissionResponse] = {
    val result = extractIfSuccessful[EISSubmissionResponse](response)
    result match {
      case Right(eisResponse) => Right(eisResponse)
      case Left(httpResponse: HttpResponse) => Left(handleErrorResponse(httpResponse))
    }
  }

  private def handleErrorResponse
  (
    response: HttpResponse
  ): Result = {

    logger.warn(EISErrorMessage(createdDateTime, ern, response.body, correlationId, MessageTypes.IE815.value))

    val messageAsJson = response.json
    response.status match {
      case BAD_REQUEST =>
        BadRequest(Json.toJson(removeControlDocumentReferences(messageAsJson.toString)))
      case NOT_FOUND => NotFound(messageAsJson)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(messageAsJson)
      case _ => InternalServerError(messageAsJson)
    }
  }

  def extractIfSuccessful[T](response: HttpResponse)(implicit reads: Reads[T], tt: TypeTag[T]): Either[HttpResponse, T] =
    if (is2xx(response.status)) Right(jsonAs[T](response.body))
    else Left(response)

  private def jsonAs[T](body: String)(implicit reads: Reads[T], tt: TypeTag[T]): T = {
    Try(Json.parse(body).as[T]) match {
      case Success(obj) => obj
      case Failure(exception) => throw new RuntimeException(s"Response body could not be read as type ${typeOf[T]}", exception)
    }
  }

  def removeControlDocumentReferences(errorMsg: String): String = {
    val result = errorMsg.replaceAll("/con:[^/]*(?=/)", "")
    result
  }
}

object EISHttpReader {
  def apply(correlationId: String, ern: String, createDateTime: String): EISHttpReader = {
    new EISHttpReader(
      correlationId: String,
      ern: String,
      createDateTime: String
    )
  }
}
